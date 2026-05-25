"""
Job 2 — Bronze Layer: CSV → HDFS Parquet.

Reads raw CSV files, cleans column names, writes partitioned Parquet
to HDFS Bronze layer. Marks processed years in the URL config file.
"""

import os
import sys
from pathlib import Path
from pyspark.sql import SparkSession

# ── Config (env-configurable) ────────────────────────────────────────────────
HDFS_NAMENODE    = os.environ.get("HDFS_NAMENODE", "namenode")
HDFS_BRONZE_PATH = os.environ.get("HDFS_BRONZE_PATH", f"hdfs://{HDFS_NAMENODE}:9000/bronze/sparcs_parquet")
CSV_BASE_PATH    = os.environ.get("RAW_CSV_DIR", "/opt/spark-jobs/data/raw_csv")
URLS_FILE        = Path(os.environ.get("URLS_FILE", "/opt/spark-jobs/config/urls_to_process.txt"))


def get_smallest_pending_year(filepath: Path) -> str | None:
    if not filepath.exists():
        raise FileNotFoundError(f"URL file not found: {filepath}")

    pending_years = []
    with filepath.open("r") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("|")
            if len(parts) != 3:
                continue
            _, year_str, status = [p.strip() for p in parts]
            if status.lower() == "pending":
                try:
                    pending_years.append(int(year_str))
                except ValueError:
                    continue
    return str(min(pending_years)) if pending_years else None


def mark_year_as_done(filepath: Path, year: str, status: str = "done"):
    lines = filepath.read_text().splitlines(keepends=True)
    with filepath.open("w") as f:
        for line in lines:
            parts = line.strip().split("|")
            if len(parts) == 3 and parts[1].strip() == year:
                parts[2] = f" {status}\n"
                f.write("|".join(parts))
            else:
                f.write(line)
    print(f"  Marked year {year} as {status}")


def main():
    print("=" * 50)
    print("  Job 2 — Bronze (CSV → HDFS Parquet)")
    print("=" * 50)

    try:
        year = get_smallest_pending_year(URLS_FILE)
    except Exception as e:
        print(f"  Error: {e}")
        sys.exit(1)

    if not year:
        print("  No pending years.")
        sys.exit(0)

    print(f"  Year: {year}")
    csv_path = f"file://{CSV_BASE_PATH}/SPARCS_{year}.csv"
    local_csv = Path(f"{CSV_BASE_PATH}/SPARCS_{year}.csv")

    if not local_csv.exists():
        print(f"  CSV not found: {local_csv} — run Job 1 first.")
        sys.exit(1)

    spark = SparkSession.builder \
        .appName(f"SPARCS_Bronze_{year}") \
        .config("spark.sql.adaptive.enabled", "true") \
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
        .getOrCreate()

    try:
        print(f"  Reading CSV: {csv_path}")
        df = spark.read \
            .option("header", "true") \
            .option("inferSchema", "true") \
            .option("mode", "PERMISSIVE") \
            .csv(csv_path)

        count = df.count()
        print(f"  {count:,} rows loaded")

        df_clean = df
        for old_col in df.columns:
            new_col = old_col.replace(" ", "_").replace("-", "_")
            if old_col != new_col:
                df_clean = df_clean.withColumnRenamed(old_col, new_col)

        print(f"  Writing HDFS: {HDFS_BRONZE_PATH}")
        df_clean.write \
            .mode("append") \
            .partitionBy("Discharge_Year") \
            .parquet(HDFS_BRONZE_PATH)
        print("  Parquet write complete")

        print("  Verifying HDFS...")
        df_check = spark.read.parquet(HDFS_BRONZE_PATH)
        total = df_check.count()
        print(f"  {total:,} total rows in Bronze")

        os.remove(local_csv)
        print(f"  Deleted local CSV: {local_csv}")

        mark_year_as_done(URLS_FILE, year, "done")
        print(f"  Job 2 done for year {year}")

    except Exception as e:
        print(f"  Error: {e}")
        mark_year_as_done(URLS_FILE, year, "error")
        spark.stop()
        sys.exit(1)

    spark.stop()


if __name__ == "__main__":
    main()
