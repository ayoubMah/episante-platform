"""
Job 3 — Silver Layer: HDFS Bronze → MongoDB.

Reads Parquet from HDFS, applies cleaning and transformations,
writes structured documents to MongoDB. Supports incremental
processing via state file with timestamp-based delta detection.
"""

import json
import os
import sys
import uuid
from datetime import datetime, timezone
from pathlib import Path

from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    col, when, lit, concat, monotonically_increasing_id,
    current_timestamp, trim, upper, to_timestamp,
    max as _max, coalesce
)

# ── Config (env-configurable) ────────────────────────────────────────────────
HDFS_NAMENODE    = os.environ.get("HDFS_NAMENODE", "namenode")
HDFS_BRONZE_PATH = os.environ.get("HDFS_BRONZE_PATH", f"hdfs://{HDFS_NAMENODE}:9000/bronze/sparcs_parquet")
MONGO_HOST       = os.environ.get("MONGO_HOST", "mongodb")
MONGO_URI        = os.environ.get("MONGO_URI", f"mongodb://{MONGO_HOST}:27017")
MONGO_DB         = os.environ.get("MONGO_DB", "episante")
MONGO_COLLECTION = os.environ.get("MONGO_COLLECTION", "sparcs_silver")
STATE_DIR        = os.environ.get("STATE_DIR", "/opt/spark-jobs/state")
STATE_FILE       = f"{STATE_DIR}/job3_state.json"
BATCH_ID         = str(uuid.uuid4())[:8]


def load_state() -> dict:
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, "r") as f:
            return json.load(f)
    return {"processed_years": [], "last_processed": None, "status": "done", "last_timestamp": None}


def save_state(processed_years: list, status: str = "done", last_ts: str | None = None):
    os.makedirs(os.path.dirname(STATE_FILE), exist_ok=True)
    state = {
        "processed_years": sorted(processed_years),
        "last_processed": datetime.now(timezone.utc).isoformat(),
        "status": status,
        "last_timestamp": last_ts,
    }
    with open(STATE_FILE, "w") as f:
        json.dump(state, f, indent=2)
    print(f"  State saved: {state}")


def main():
    print("=" * 50)
    print("  Job 3 — Silver (HDFS Bronze → MongoDB)")
    print("=" * 50)

    spark = SparkSession.builder \
        .appName("SPARCS_Silver_Incremental") \
        .config("spark.driver.memory", "6g") \
        .config("spark.sql.shuffle.partitions", "4") \
        .config("spark.sql.adaptive.enabled", "true") \
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
        .getOrCreate()

    processed_years = set()
    new_last_ts = None

    try:
        state = load_state()
        processed_years = set(state["processed_years"])
        last_timestamp = state.get("last_timestamp")

        if state["status"] == "running":
            print("  Previous job interrupted — full reprocess")
            processed_years = set()
            last_timestamp = None

        # List HDFS partition years
        fs = spark._jvm.org.apache.hadoop.fs.FileSystem.get(spark._jsc.hadoopConfiguration())
        hdfs_path = spark._jvm.org.apache.hadoop.fs.Path(HDFS_BRONZE_PATH)
        hdfs_years = set()
        try:
            for status_obj in fs.listStatus(hdfs_path):
                name = status_obj.getPath().getName()
                if name.startswith("Discharge_Year="):
                    try:
                        hdfs_years.add(int(name.split("=")[1]))
                    except ValueError:
                        pass
        except Exception:
            pass

        remaining = sorted(hdfs_years - processed_years)
        print(f"  HDFS years: {sorted(hdfs_years)}")
        print(f"  Already processed: {sorted(processed_years)}")
        print(f"  Remaining: {remaining}")

        if not remaining:
            print("  Nothing to process.")
            spark.stop()
            sys.exit(0)

        year = remaining[0]
        print(f"  Processing year: {year}")
        save_state(list(processed_years), status="running", last_ts=last_timestamp)

        # Read Bronze
        path = f"{HDFS_BRONZE_PATH}/Discharge_Year={year}"
        df_bronze = spark.read.parquet(path).coalesce(4)
        df_bronze = df_bronze.withColumn("Discharge_Year", lit(year).cast("int"))

        if "bronze_ingestion_ts" not in df_bronze.columns:
            df_bronze = df_bronze.withColumn("bronze_ingestion_ts", current_timestamp())
        else:
            df_bronze = df_bronze.withColumn("bronze_ingestion_ts",
                coalesce(to_timestamp(col("bronze_ingestion_ts")), current_timestamp()))

        if last_timestamp:
            df_bronze = df_bronze.filter(col("bronze_ingestion_ts") > to_timestamp(lit(last_timestamp)))

        df_bronze.cache()
        total = df_bronze.count()
        print(f"  {total:,} delta rows loaded")

        if total == 0:
            all_done = sorted(processed_years | {year})
            save_state(all_done, status="done", last_ts=last_timestamp)
            df_bronze.unpersist()
            spark.stop()
            sys.exit(0)

        max_ts_row = df_bronze.agg(_max("bronze_ingestion_ts").alias("max_ts")).collect()[0]
        new_last_ts = max_ts_row["max_ts"].isoformat() if max_ts_row["max_ts"] else last_timestamp

        # Clean
        COLS_REQUIRED = ["Health_Service_Area", "Hospital_County", "Age_Group", "Gender",
                         "APR_Severity_of_Illness_Description", "CCS_Diagnosis_Description"]
        df_clean = df_bronze.dropDuplicates().dropna(subset=COLS_REQUIRED).cache()
        print(f"  {df_clean.count():,} rows after cleaning")
        df_bronze.unpersist()

        # Transform
        df_silver = df_clean \
            .withColumn("appointment_region", trim(col("Health_Service_Area"))) \
            .withColumn("appointment_county", trim(col("Hospital_County"))) \
            .withColumn("patient_age_group", trim(col("Age_Group"))) \
            .withColumn("patient_gender", trim(upper(col("Gender")))) \
            .withColumn("calendar_year", col("Discharge_Year").cast("int")) \
            .withColumn("calendar_month", lit(6).cast("int")) \
            .withColumn("calendar_year_month", concat(col("Discharge_Year").cast("string"), lit("-06"))) \
            .withColumn("severity_label", trim(col("APR_Severity_of_Illness_Description"))) \
            .withColumn("complexity_level",
                when(col("APR_Severity_of_Illness_Description") == "Minor", lit(1))
                .when(col("APR_Severity_of_Illness_Description") == "Moderate", lit(2))
                .when(col("APR_Severity_of_Illness_Description") == "Major", lit(3))
                .when(col("APR_Severity_of_Illness_Description") == "Extreme", lit(4))
                .otherwise(lit(1))) \
            .withColumn("appointment_duration_minutes",
                when(col("complexity_level") == 1, lit(15))
                .when(col("complexity_level") == 2, lit(30))
                .when(col("complexity_level") == 3, lit(45))
                .when(col("complexity_level") == 4, lit(60))
                .otherwise(lit(15))) \
            .withColumn("reason_label", trim(col("CCS_Diagnosis_Description"))) \
            .withColumn("appointment_id", monotonically_increasing_id()) \
            .withColumn("metadata_ingestion_timestamp", current_timestamp()) \
            .withColumn("metadata_batch_id", lit(BATCH_ID))

        SILVER_COLS = [
            "appointment_id", "appointment_region", "appointment_county",
            "patient_age_group", "patient_gender", "calendar_year", "calendar_month",
            "calendar_year_month", "severity_label", "complexity_level",
            "appointment_duration_minutes", "reason_label",
            "metadata_ingestion_timestamp", "metadata_batch_id",
        ]

        # Write MongoDB
        print(f"  Writing MongoDB: {MONGO_DB}.{MONGO_COLLECTION}")
        df_silver.select(SILVER_COLS).write \
            .format("mongodb") \
            .mode("append") \
            .option("connection.uri", MONGO_URI) \
            .option("database", MONGO_DB) \
            .option("collection", MONGO_COLLECTION) \
            .save()
        print("  MongoDB write complete")

        all_done = sorted(processed_years | {year})
        save_state(all_done, status="done", last_ts=new_last_ts)
        df_clean.unpersist()
        print(f"  Job 3 done for year {year}")

    except Exception as e:
        save_state(list(processed_years), status="error", last_ts=new_last_ts)
        print(f"  Error: {e}")
        import traceback
        traceback.print_exc()
        spark.stop()
        sys.exit(1)

    spark.stop()


if __name__ == "__main__":
    main()
