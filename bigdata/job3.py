from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    col, when, lit, concat, monotonically_increasing_id,
    current_timestamp, trim, upper, to_date
)
import uuid
import sys
import json
import os
from datetime import datetime, timezone

# ── State Management ───────────────────────────────────────────────────────────
STATE_FILE = "/home/spark/spark_jobs/state/job33_state.json"

def load_state() -> dict:
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, "r") as f:
            return json.load(f)
    return {
        "processed_years": [],
        "last_processed": None,
        "status": "done"
    }

def save_state(processed_years: list, status: str = "done"):
    os.makedirs(os.path.dirname(STATE_FILE), exist_ok=True)
    state = {
        "processed_years": sorted(processed_years),
        "last_processed": datetime.now(timezone.utc).isoformat(),
        "status": status
    }
    with open(STATE_FILE, "w") as f:
        json.dump(state, f, indent=2)
    print(f"   → État sauvegardé : {state}")

# ── Config Spark OPTIMISÉE ─────────────────────────────────────────────────────
spark = SparkSession.builder \
    .appName("SPARCS_Silver_Incremental") \
    .config("spark.driver.memory", "6g") \
    .config("spark.executor.memory", "4g") \
    .config("spark.sql.shuffle.partitions", "4") \
    .config("spark.default.parallelism", "4") \
    .config("spark.sql.adaptive.enabled", "true") \
    .config("spark.sql.adaptive.coalescePartitions.enabled", "true") \
    .config("spark.sql.adaptive.advisoryPartitionSizeInBytes", "64MB") \
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
    .config("spark.executor.heartbeatInterval", "60s") \
    .config("spark.network.timeout", "300s") \
    .getOrCreate()

HDFS_BRONZE_PATH = "hdfs://192.168.1.30:9000/bronze/sparcs_parquet"
MONGO_URI        = "mongodb://192.168.1.40:27017"
MONGO_DB         = "sirius"
MONGO_COLLECTION = "sparcs_silver"
BATCH_ID         = str(uuid.uuid4())[:8]

def list_hdfs_years() -> set:
    """Liste les années HDFS via metadata uniquement (sans lire les données)."""
    fs = spark._jvm.org.apache.hadoop.fs.FileSystem.get(
        spark._jsc.hadoopConfiguration()
    )
    hdfs_path = spark._jvm.org.apache.hadoop.fs.Path(HDFS_BRONZE_PATH)
    years = set()
    try:
        for status in fs.listStatus(hdfs_path):
            name = status.getPath().getName()
            if name.startswith("Discharge_Year="):
                try:
                    years.add(int(name.split("=")[1]))
                except ValueError:
                    pass
    except Exception as e:
        print(f"    Erreur lecture HDFS metadata : {e}")
    return years
def main():
    print("===== Job 3 INCRÉMENTAL - Bronze → Silver =====")

    processed_years = set()
    try:
        # ── 1. DÉTECTION INCRÉMENTALE via STATE ───────────────────────────────
        print("\n Détection incrémentale...")

        state = load_state()
        processed_years = set(state["processed_years"])
        last_status = state["status"]

        print(f"   → Statut précédent       : {last_status}")
        print(f"   → Années déjà traitées   : {sorted(processed_years)}")

        if last_status == "running":
            print("   Job précédent interrompu → reprise forcée")
            processed_years = set()

        hdfs_years = list_hdfs_years()
        print(f"   → HDFS Bronze disponible : {sorted(hdfs_years)}")

        remaining_years = sorted(hdfs_years - processed_years)
        print(f"   → Années restantes       : {remaining_years}")

        if not remaining_years:
            print(" Aucune nouvelle donnée à traiter.")
            spark.stop()
            sys.exit(0)
            # Traiter uniquement l'année la plus ancienne non traitée
        year_to_process = remaining_years[0]
        print(f"   →  Traitement de l'année : {year_to_process}")

        # Marquer "running" avant de commencer
        save_state(list(processed_years), status="running")

        # ── 2. LECTURE HDFS — une seule année ─────────────────────────────────
        print(f"\n Lecture HDFS → Discharge_Year={year_to_process}...")
        path = f"{HDFS_BRONZE_PATH}/Discharge_Year={year_to_process}"
        df_bronze = spark.read.parquet(path).coalesce(4)

        # Recréer Discharge_Year (absent quand on lit un seul dossier partitionné)
        df_bronze = df_bronze.withColumn("Discharge_Year", lit(year_to_process).cast("int"))

        df_bronze.cache()
        total_bronze = df_bronze.count()
        print(f"   → {total_bronze:,} lignes chargées")

        # ── 3. NETTOYAGE ──────────────────────────────────────────────────────
        print("\n Nettoyage")

        # Discharge_Year retiré de COLS_REQUIRED (recréée via lit())
        COLS_REQUIRED = [
            "Health_Service_Area",
            "Hospital_County",
            "Age_Group",
            "Gender",
            "APR_Severity_of_Illness_Description",
            "CCS_Diagnosis_Description"
        ]

        df_clean = df_bronze \
            .dropDuplicates() \
            .dropna(subset=COLS_REQUIRED) \
            .cache()
        clean_count = df_clean.count()

        df_bronze.unpersist()
        print(f"   → {clean_count:,} lignes après nettoyage")

        # ── 4. TRANSFORMATIONS SILVER (selon PDF) ─────────────────────────────
        print("\n Transformations Silver...")

        df_silver = df_clean \
            \
            .withColumn("appointment_region",  trim(col("Health_Service_Area"))) \
            .withColumn("appointment_county",  trim(col("Hospital_County"))) \
            \
            .withColumn("patient_age_group",   trim(col("Age_Group"))) \
            .withColumn("patient_gender",      trim(upper(col("Gender")))) \
            \
            .withColumn("calendar_year",       col("Discharge_Year").cast("int")) \
            .withColumn("calendar_month",      lit(6).cast("int")) \
            .withColumn("calendar_year_month", concat(
                col("Discharge_Year").cast("string"), lit("-06")
            )) \
            \
            .withColumn("severity_label",      trim(col("APR_Severity_of_Illness_Description"))) \
            .withColumn("complexity_level",
                when(col("APR_Severity_of_Illness_Description") == "Minor",    lit(1))
                .when(col("APR_Severity_of_Illness_Description") == "Moderate", lit(2))
                .when(col("APR_Severity_of_Illness_Description") == "Major",    lit(3))
                .when(col("APR_Severity_of_Illness_Description") == "Extreme",  lit(4))
                .otherwise(lit(1))) \
            .withColumn("appointment_duration_minutes",
                when(col("complexity_level") == 1, lit(15))
                .when(col("complexity_level") == 2, lit(30))
                .when(col("complexity_level") == 3, lit(45))
                .when(col("complexity_level") == 4, lit(60))
                .otherwise(lit(15))) \
            \
            .withColumn("reason_label",                 trim(col("CCS_Diagnosis_Description"))) \
            \
            .withColumn("appointment_id",               monotonically_increasing_id()) \
            .withColumn("metadata_ingestion_timestamp", current_timestamp()) \
            .withColumn("metadata_batch_id",            lit(BATCH_ID))
        # Colonnes Silver finales — Drop tout le reste (Race, Ethnicity, Zip, admin...)
        SILVER_COLS = [
            "appointment_id",                   # appointment.id
            "appointment_region",               # appointment.region
            "appointment_county",               # appointment.county
            "patient_age_group",                # patient.age_group
            "patient_gender",                   # patient.gender
            "calendar_year",                    # calendar.year
            "calendar_month",                   # calendar.month
            "calendar_year_month",              # calendar.year_month (YYYY-MM)
            "severity_label",                   # severity.label
            "complexity_level",                 # complexity.level (1..4)
            "appointment_duration_minutes",     # appointment.duration_minutes
            "reason_label",                     # reason.label
            "metadata_ingestion_timestamp",     # metadata.ingestion_timestamp
            "metadata_batch_id"                 # metadata.batch_id
        ]
        df_silver_final = df_silver.select(SILVER_COLS)

        # ── 5. ÉCRITURE MONGODB ────────────────────────────────────────────────
        print(f"\n ÉCRITURE MongoDB → {MONGO_DB}.{MONGO_COLLECTION}")
        df_silver_final.write \
            .format("mongodb") \
            .mode("append") \
            .option("connection.uri", MONGO_URI) \
            .option("database", MONGO_DB) \
            .option("collection", MONGO_COLLECTION) \
            .save()
        print(" Écriture terminée !")

        # ── 6. VÉRIFICATION FINALE ─────────────────────────────────────────────
        print("\n🔍 Vérification MongoDB...")
        df_check = spark.read \
            .format("mongodb") \
            .option("connection.uri", MONGO_URI) \
            .option("database", MONGO_DB) \
            .option("collection", MONGO_COLLECTION) \
            .load()
        df_check.groupBy("calendar_year").count().orderBy("calendar_year").show()

        # ── 7. SAUVEGARDER L'ÉTAT FINAL ───────────────────────────────────────
        all_done = sorted(processed_years | {year_to_process})
        save_state(all_done, status="done")

        df_clean.unpersist()

        print(f"\n Job 3 INCRÉMENTAL terminé !")
        print(f"   Année traitée          : {year_to_process}")
        print(f"   Total années traitées  : {all_done}")
        print(f"   Années restantes       : {sorted(hdfs_years - set(all_done))}")

    except Exception as e:
        save_state(list(processed_years), status="error")
        print(f"\n Erreur Job 3 : {e}")
        import traceback
        traceback.print_exc()
        spark.stop()
        sys.exit(1)

    spark.stop()

if __name__ == "__main__":
    main()