#!/usr/bin/env python3
from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    col, sum as _sum, avg, max as _max,
    lit, concat, countDistinct, to_date, to_timestamp
)
import json
import os
import sys
from datetime import datetime, timezone


# ── State files ───────────────────────────────────────────────────────────────
STATE_FILE_44 = "/home/spark/spark_jobs/state/job44_state.json"
STATE_FILE_33 = "/home/spark/spark_jobs/state/job33_state.json"


def load_state(path: str) -> dict:
    """ Charge l'état du job depuis le fichier JSON."""
    if os.path.exists(path):
        with open(path, "r") as f:
            return json.load(f)
    return {
        "processed_years": [],
        "status": "done",
        "last_processed": None,
        "last_timestamp": None
    }


def save_state(processed_years: list, status: str, last_year: int | None = None, last_ts: str | None = None):
    """ Sauvegarde l'état avec timestamp max du run."""
    os.makedirs(os.path.dirname(STATE_FILE_44), exist_ok=True)
    state = {
        "processed_years": sorted(processed_years),
        "status": status,
        "last_year": last_year,
        "last_timestamp": last_ts,
        "last_processed": datetime.now(timezone.utc).isoformat()
    }
    with open(STATE_FILE_44, "w") as f:
        json.dump(state, f, indent=2)
    print(f"   → State saved : {state}")


def get_year_from_job33() -> int | None:
    """ Récupère la plus petite année traitée par job33 (status=done)."""
    state33 = load_state(STATE_FILE_33)
    status33 = state33.get("status", "done")

    if status33 != "done":
        print(f"    job33 status = '{status33}' → pas encore terminé, on attend")
        return None

    years = state33.get("processed_years", [])
    if not years:
        print("    job33_state.json vide → rien à traiter")
        return None

    return min(years)


# ── Config Spark ──────────────────────────────────────────────────────────────
spark = SparkSession.builder \
    .master("local[*]") \
    .appName("SPARCS_Gold_MySQL") \
    .config("spark.driver.memory", "4g") \
    .config("spark.executor.memory", "4g") \
    .config("spark.sql.shuffle.partitions", "4") \
    .config("spark.default.parallelism", "4") \
    .config("spark.sql.adaptive.enabled", "true") \
    .config("spark.sql.adaptive.coalescePartitions.enabled", "true") \
    .config("spark.sql.adaptive.advisoryPartitionSizeInBytes", "64MB") \
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
    .getOrCreate()

print("Spark master =", spark.sparkContext.master)

# ── Connexions ────────────────────────────────────────────────────────────────
MONGO_URI        = "mongodb://192.168.1.40:27017"
MONGO_DB         = "sirius"
MONGO_COLLECTION = "sparcs_silver"

MYSQL_HOST     = "192.168.1.50"
MYSQL_PORT     = "3306"
MYSQL_DB       = "sirius_gold"
MYSQL_TABLE    = "kpi_appointments"
MYSQL_USER     = "spark"
MYSQL_PASSWORD = "spark"
MYSQL_URL      = (
    f"jdbc:mysql://{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DB}"
    f"?useSSL=false&allowPublicKeyRetrieval=true"
)
JDBC_OPTS = {
    "url":      MYSQL_URL,
    "dbtable":  MYSQL_TABLE,
    "user":     MYSQL_USER,
    "password": MYSQL_PASSWORD,
    "driver":   "com.mysql.cj.jdbc.Driver"
}


def read_silver_for_year(year: int, last_ts: str | None):
    """
     Lit MongoDB pour l'année demandée.
     DELTA : Si last_ts défini, filtre uniquement les docs plus récents.
    """
    if last_ts:
        # Filtre combiné : année + timestamp > dernier run
        pipeline = (
            f'[{{"$match": {{'
            f'"calendar_year": {int(year)}, '
            f'"metadata_ingestion_timestamp": {{"$gt": {{"$date": "{last_ts}"}}}}'
            f'}}}}]'
        )
        print(f"   → Pipeline delta : calendar_year={year} + ts > {last_ts}")
    else:
        # Premier run : toute l'année sans filtre timestamp
        pipeline = f'[{{"$match": {{"calendar_year": {int(year)}}}}}]'
        print(f"   → Pipeline complet : calendar_year={year} (premier run)")

    return spark.read \
        .format("mongodb") \
        .option("connection.uri", MONGO_URI) \
        .option("database", MONGO_DB) \
        .option("collection", MONGO_COLLECTION) \
        .option("spark.mongodb.read.aggregation.pipeline", pipeline) \
        .load()


def main():
    print("\n===== Job 4 INCRÉMENTAL - Silver MongoDB → Gold MySQL (delta timestamp) =====")

    processed_years = set()
    new_last_ts     = None

    try:
        # ── 1. DÉTECTION INCRÉMENTALE via STATE ───────────────────────────────
        print("\n Détection incrémentale via job33_state.json...")

        state44         = load_state(STATE_FILE_44)
        processed_years = set(state44.get("processed_years", []))
        status44        = state44.get("status", "done")
        last_timestamp  = state44.get("last_timestamp")

        print(f"   → Status job44          : {status44}")
        print(f"   → Gold déjà chargé      : {sorted(processed_years)}")
        print(f"   → Dernier timestamp     : {last_timestamp}")

        # Reprise si job précédent interrompu
        if status44 == "running":
            print("   → Job précédent interrompu → reprise forcée")
            processed_years = set()
            last_timestamp  = None

        # Récupérer l'année cible depuis job33
        year_to_process = get_year_from_job33()

        if year_to_process is None:
            print(" Rien à traiter (job33 pas prêt).")
            spark.stop()
            sys.exit(0)

        if year_to_process in processed_years and last_timestamp is None:
            print(f" Année {year_to_process} déjà dans Gold et pas de nouveau timestamp → rien à faire.")
            spark.stop()
            sys.exit(0)

        print(f"   → Année à traiter       : {year_to_process}")

        save_state(list(processed_years), status="running",
                   last_year=year_to_process, last_ts=last_timestamp)

        # ── 2. LECTURE SILVER — 1 année + DELTA TIMESTAMP ─────────────────────
        print(f"\n Lecture Silver MongoDB (calendar_year={year_to_process})...")
        df_silver = read_silver_for_year(year_to_process, last_timestamp).coalesce(4).cache()
        n_docs = df_silver.count()
        print(f"   → {n_docs:,} documents Silver delta chargés ✅")

        if n_docs == 0:
            print(" 0 document delta pour cette année → rien à écrire.")
            processed_years.add(year_to_process)
            save_state(sorted(processed_years), status="done",
                       last_year=year_to_process, last_ts=last_timestamp)
            df_silver.unpersist()
            spark.stop()
            sys.exit(0)

        #  DELTA 
        max_ts_row  = df_silver.agg(_max("metadata_ingestion_timestamp").alias("max_ts")).collect()[0]
        new_last_ts = max_ts_row["max_ts"].isoformat() if max_ts_row["max_ts"] else last_timestamp
        print(f"   → Nouveau last_timestamp : {new_last_ts}")

        # ── 3. TRANSFORMATION : calendar_year_month → DATE pour Grafana ───────
        print("\n Transformations Gold...")
        df_gold = df_silver.withColumn(
            "month_start_date",
            to_date(concat(col("calendar_year_month"), lit("-01")), "yyyy-MM-dd")
        )

        # ── 4. AGRÉGATION KPI ─────────────────────────────────────────────────
        df_agg = df_gold.groupBy(
            "calendar_year",
            "calendar_month",
            "calendar_year_month",
            "month_start_date",
            "patient_age_group",
            "patient_gender",
            "complexity_level",
            "severity_label",
            "reason_label",
            "appointment_region"
        ).agg(
            countDistinct("appointment_id").alias("total_appointments"),
            _sum("appointment_duration_minutes").alias("total_duration_minutes"),
            avg("appointment_duration_minutes").alias("avg_duration_minutes"),
            _max("metadata_batch_id").alias("last_batch_id"),
            _max("metadata_ingestion_timestamp").alias("load_timestamp")
        ).cache()

        df_silver.unpersist()

        total_rows = df_agg.count()
        print(f"\n GOLD DELTA : {total_rows:,} lignes agrégées (year={year_to_process})")

        df_agg.groupBy("calendar_year") \
            .agg(_sum("total_appointments").alias("total_appointments")) \
            .orderBy("calendar_year").show()

        # ── 5. ÉCRITURE MYSQL (append delta) ──────────────────────────────────
        print(f"\n ÉCRITURE MySQL (append delta) → {MYSQL_HOST}/{MYSQL_DB}.{MYSQL_TABLE}")
        df_agg.write \
            .format("jdbc") \
            .options(**JDBC_OPTS) \
            .mode("append") \
            .save()
        print(" Écriture MySQL delta terminée !")
        df_agg.unpersist()

        # ── 6. SAUVEGARDER L'ÉTAT FINAL ───────────────────────────────────────
        processed_years.add(year_to_process)
        save_state(sorted(processed_years), status="done",
                   last_year=year_to_process, last_ts=new_last_ts)

        # ── 7. VÉRIFICATION LÉGÈRE MYSQL ──────────────────────────────────────
        print("\n Vérification MySQL...")
        df_check = spark.read.format("jdbc").options(**JDBC_OPTS).load()
        df_check.groupBy("calendar_year") \
            .agg(_sum("total_appointments").alias("total_appointments")) \
            .orderBy("calendar_year").show()

        print(f"\n Job 4 INCRÉMENTAL terminé !")
        print(f"   Année traitée        : {year_to_process}")
        print(f"   Gold total chargé    : {sorted(processed_years)}")
        print(f"   Nouveau last_ts      : {new_last_ts}")

    except Exception as e:
        try:
            save_state(list(processed_years), status="error",
                       last_ts=new_last_ts)
        except Exception:
            pass
        print(f"\n Erreur Job 4 : {e}")
        import traceback
        traceback.print_exc()
        spark.stop()
        sys.exit(1)

    spark.stop()


if __name__ == "__main__":
    main()
