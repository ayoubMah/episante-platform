"""
Job 4 — Gold Layer: MongoDB Silver → PostgreSQL Data Warehouse.

Reads Silver documents from MongoDB, aggregates KPIs, and writes
to the Gold PostgreSQL warehouse. Supports incremental processing
via timestamp-based delta detection.
"""

import json
import os
import sys
from datetime import datetime, timezone

from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    col, sum as _sum, avg, max as _max,
    lit, concat, countDistinct, to_date
)

# ── Config (env-configurable) ────────────────────────────────────────────────
MONGO_HOST       = os.environ.get("MONGO_HOST", "mongodb")
MONGO_URI        = os.environ.get("MONGO_URI", f"mongodb://{MONGO_HOST}:27017")
MONGO_DB         = os.environ.get("MONGO_DB", "episante")
MONGO_COLLECTION = os.environ.get("MONGO_COLLECTION", "sparcs_silver")

PG_HOST     = os.environ.get("DW_HOST", "postgres-dw")
PG_PORT     = os.environ.get("DW_PORT", "5432")
PG_DB       = os.environ.get("DW_DB", "episante_gold")
PG_TABLE    = os.environ.get("DW_TABLE", "kpi_appointments")
PG_USER     = os.environ.get("DW_USER", "spark")
PG_PASSWORD = os.environ.get("DW_PASSWORD", "spark_pass")
PG_URL      = f"jdbc:postgresql://{PG_HOST}:{PG_PORT}/{PG_DB}"

JDBC_OPTS = {
    "url": PG_URL,
    "dbtable": PG_TABLE,
    "user": PG_USER,
    "password": PG_PASSWORD,
    "driver": "org.postgresql.Driver",
}

STATE_DIR      = os.environ.get("STATE_DIR", "/opt/spark-jobs/state")
STATE_FILE_4   = f"{STATE_DIR}/job4_state.json"
STATE_FILE_3   = f"{STATE_DIR}/job3_state.json"


def load_state(path: str) -> dict:
    if os.path.exists(path):
        with open(path, "r") as f:
            return json.load(f)
    return {"processed_years": [], "status": "done", "last_processed": None, "last_timestamp": None}


def save_state(processed_years: list, status: str, last_year: int | None = None, last_ts: str | None = None):
    os.makedirs(os.path.dirname(STATE_FILE_4), exist_ok=True)
    state = {
        "processed_years": sorted(processed_years),
        "status": status,
        "last_year": last_year,
        "last_timestamp": last_ts,
        "last_processed": datetime.now(timezone.utc).isoformat(),
    }
    with open(STATE_FILE_4, "w") as f:
        json.dump(state, f, indent=2)
    print(f"  State saved: {state}")


def get_year_from_job3() -> int | None:
    state3 = load_state(STATE_FILE_3)
    if state3.get("status") != "done":
        print("  Job 3 not done yet — waiting")
        return None
    years = state3.get("processed_years", [])
    return min(years) if years else None


def main():
    print("=" * 50)
    print("  Job 4 — Gold (MongoDB Silver → PostgreSQL DW)")
    print("=" * 50)

    spark = SparkSession.builder \
        .appName("SPARCS_Gold_PostgreSQL") \
        .config("spark.driver.memory", "4g") \
        .config("spark.sql.shuffle.partitions", "4") \
        .config("spark.sql.adaptive.enabled", "true") \
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
        .getOrCreate()

    processed_years = set()
    new_last_ts = None

    try:
        state4 = load_state(STATE_FILE_4)
        processed_years = set(state4.get("processed_years", []))
        last_timestamp = state4.get("last_timestamp")

        if state4.get("status") == "running":
            print("  Previous job interrupted — full reprocess")
            processed_years = set()
            last_timestamp = None

        year = get_year_from_job3()
        if year is None:
            print("  Nothing to process.")
            spark.stop()
            sys.exit(0)

        if year in processed_years and last_timestamp is None:
            print(f"  Year {year} already in Gold — nothing to do.")
            spark.stop()
            sys.exit(0)

        print(f"  Processing year: {year}")
        save_state(list(processed_years), status="running", last_year=year, last_ts=last_timestamp)

        # Read Silver
        if last_timestamp:
            pipeline = (
                f'[{{"$match": {{'
                f'"calendar_year": {int(year)}, '
                f'"metadata_ingestion_timestamp": {{"$gt": {{"$date": "{last_timestamp}"}}}}'
                f'}}}}]'
            )
        else:
            pipeline = f'[{{"$match": {{"calendar_year": {int(year)}}}}}]'

        print(f"  Reading MongoDB Silver (year={year})...")
        df_silver = spark.read \
            .format("mongodb") \
            .option("connection.uri", MONGO_URI) \
            .option("database", MONGO_DB) \
            .option("collection", MONGO_COLLECTION) \
            .option("spark.mongodb.read.aggregation.pipeline", pipeline) \
            .load().coalesce(4).cache()

        n_docs = df_silver.count()
        print(f"  {n_docs:,} Silver documents loaded")

        if n_docs == 0:
            processed_years.add(year)
            save_state(sorted(processed_years), status="done", last_year=year, last_ts=last_timestamp)
            df_silver.unpersist()
            spark.stop()
            sys.exit(0)

        max_ts_row = df_silver.agg(_max("metadata_ingestion_timestamp").alias("max_ts")).collect()[0]
        new_last_ts = max_ts_row["max_ts"].isoformat() if max_ts_row["max_ts"] else last_timestamp

        # Transform
        df_gold = df_silver.withColumn(
            "month_start_date",
            to_date(concat(col("calendar_year_month"), lit("-01")), "yyyy-MM-dd")
        )

        # Aggregate KPIs
        df_agg = df_gold.groupBy(
            "calendar_year", "calendar_month", "calendar_year_month", "month_start_date",
            "patient_age_group", "patient_gender", "complexity_level", "severity_label",
            "reason_label", "appointment_region",
        ).agg(
            countDistinct("appointment_id").alias("total_appointments"),
            _sum("appointment_duration_minutes").alias("total_duration_minutes"),
            avg("appointment_duration_minutes").alias("avg_duration_minutes"),
            _max("metadata_batch_id").alias("last_batch_id"),
            _max("metadata_ingestion_timestamp").alias("load_timestamp"),
        ).cache()

        df_silver.unpersist()
        total_rows = df_agg.count()
        print(f"  {total_rows:,} Gold KPI rows aggregated")

        # Write PostgreSQL
        print(f"  Writing PostgreSQL: {PG_HOST}/{PG_DB}.{PG_TABLE}")
        df_agg.write \
            .format("jdbc") \
            .options(**JDBC_OPTS) \
            .mode("append") \
            .save()
        print("  PostgreSQL write complete")
        df_agg.unpersist()

        processed_years.add(year)
        save_state(sorted(processed_years), status="done", last_year=year, last_ts=new_last_ts)

        print(f"  Job 4 done for year {year}")

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
