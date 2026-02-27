import sys
import os
from pathlib import Path
from pyspark.sql import SparkSession

# ── Config ────────────────────────────────────────────────────────────────────

HDFS_BRONZE_PATH = "hdfs://192.168.1.30:9000/bronze/sparcs_parquet"
CSV_BASE_PATH    = "/Data_Sirius/raw_csv"
URLS_FILE        = Path("/home/spark/spark_jobs/config/urls_to_process.txt")

# ── Utils : lecture fichier URLs ──────────────────────────────────────────────

def get_smallest_pending_year(filepath: Path) -> str | None:
    """Retourne la plus petite année avec status 'pending'."""
    if not filepath.exists():
        raise FileNotFoundError(f"Fichier URLs introuvable : {filepath}")

    pending_years = []
    with filepath.open("r") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("|")
            if len(parts) != 3:
                continue
            _, year_str, status = parts
            if status.strip().lower() == "pending":
                try:
                    pending_years.append(int(year_str.strip()))
                except ValueError:
                    continue

    if not pending_years:
        return None

    return str(min(pending_years))

# ── Utils : mise à jour status ────────────────────────────────────────────────

def mark_year_as_done(filepath: Path, year: str, status: str = "done"):
    """Met à jour le status d'une année dans le fichier URLs."""
    lines = filepath.read_text().splitlines(keepends=True)
    with filepath.open("w") as f:
        for line in lines:
            parts = line.strip().split("|")
            if len(parts) == 3 and parts[1].strip() == year:
                parts[2] = status + "\n"
                f.write("|".join(parts))
            else:
                f.write(line)
            print(f"   → urls_to_process.txt mis à jour : {year} → {status}")

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    print("===== Job 2 - Bronze HDFS =====")

    # ── Récupérer l'année à traiter
    try:
        year = get_smallest_pending_year(URLS_FILE)
    except Exception as e:
        print(f"Erreur lecture fichier URLs : {e}")
        sys.exit(1)

    if not year:
        print("Aucune année 'pending' à traiter. Rien à faire.")
        sys.exit(0)

    print(f"Année à traiter : {year}")

    csv_path = f"file://{CSV_BASE_PATH}/SPARCS_{year}.csv"
    local_csv = Path(f"{CSV_BASE_PATH}/SPARCS_{year}.csv")

    if not local_csv.exists():
        print(f"CSV introuvable : {local_csv}")
        print("   → Lance job1 d'abord.")
        sys.exit(1)

    # ── Spark Session
    spark = SparkSession.builder \
        .appName(f"SPARCS_Bronze_{year}") \
        .config("spark.sql.adaptive.enabled", "true") \
        .config("spark.sql.adaptive.coalescePartitions.enabled", "true") \
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
        .getOrCreate()

    try:
        # ── Lecture CSV
        print(f"\n Lecture CSV : {csv_path}")
        df = spark.read \
            .option("header", "true") \
            .option("inferSchema", "true") \
            .option("mode", "PERMISSIVE") \
            .csv(csv_path)

        count = df.count()
        print(f"   → {count:,} lignes chargées")
        df.groupBy("Discharge Year").count().orderBy("Discharge Year").show()

        # ── Renommage colonnes (espaces / tirets → underscores)
        df_clean = df
        for old_col in df.columns:
            new_col = old_col.replace(" ", "_").replace("-", "_")
            if old_col != new_col:
                df_clean = df_clean.withColumnRenamed(old_col, new_col)

        # ── Écriture HDFS Bronze Parquet (append pour accumuler les années)
        print(f"\n ÉCRITURE HDFS : {HDFS_BRONZE_PATH}")
        df_clean.write \
            .mode("append") \
            .partitionBy("Discharge_Year") \
            .parquet(HDFS_BRONZE_PATH)
        print(" Écriture Parquet terminée !")

        # ── Vérification relecture HDFS
        print("\n Vérification relecture HDFS...")
        df_check = spark.read.parquet(HDFS_BRONZE_PATH)
        total = df_check.count()
        print(f"   → {total:,} lignes totales dans Bronze HDFS")
        df_check.groupBy("Discharge_Year").count().orderBy("Discharge_Year").show()

        # ── Suppression CSV local après confirmation
        print(f"\n Suppression CSV local : {local_csv}")
        os.remove(local_csv)
        print(f"   → {local_csv} supprimé ")

        # ── Mise à jour status dans urls_to_process.txt
        mark_year_as_done(URLS_FILE, year, "done")
        print(f"\n Job 2 terminé avec succès pour l'année {year}.")

    except Exception as e:
        print(f"\n Erreur Job 2 : {e}")
        mark_year_as_done(URLS_FILE, year, "error")
        spark.stop()
        sys.exit(1)

    spark.stop()

if __name__ == "__main__":
    main()