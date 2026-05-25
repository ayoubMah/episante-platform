"""
Airflow DAG — EpiSante Big Data Pipeline.

Orchestrates the 4-stage ETL:
  Download CSV → Bronze (HDFS) → Silver (MongoDB) → Gold (PostgreSQL)
"""

from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.python import ShortCircuitOperator
from airflow.utils.dates import days_ago
from datetime import timedelta
from pathlib import Path

SPARK_JOBS_DIR = "/opt/spark-jobs"
URLS_FILE      = f"{SPARK_JOBS_DIR}/config/urls_to_process.txt"

default_args = {
    "owner": "episante",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
    "email_on_failure": False,
}


def check_pending_urls():
    filepath = Path(URLS_FILE)
    if not filepath.exists():
        print("URL file not found")
        return False

    with filepath.open("r") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("|")
            if len(parts) == 3 and parts[2].strip().lower() == "pending":
                return True

    print("No pending URLs — skipping pipeline")
    return False


with DAG(
    dag_id="episante_bigdata_pipeline",
    description="ETL: Download → Bronze (HDFS) → Silver (MongoDB) → Gold (PostgreSQL)",
    default_args=default_args,
    start_date=days_ago(1),
    schedule_interval="0 2 * * *",
    catchup=False,
    tags=["episante", "etl", "bigdata"],
) as dag:

    t0_check = ShortCircuitOperator(
        task_id="check_pending_urls",
        python_callable=check_pending_urls,
    )

    t1_download = BashOperator(
        task_id="job1_download",
        bash_command=f"python3 {SPARK_JOBS_DIR}/job1.py",
        execution_timeout=timedelta(hours=2),
    )

    t2_bronze = BashOperator(
        task_id="job2_bronze_hdfs",
        bash_command=f"spark-submit {SPARK_JOBS_DIR}/job2.py",
        execution_timeout=timedelta(hours=2),
    )

    t3_silver = BashOperator(
        task_id="job3_silver_mongodb",
        bash_command=f"spark-submit --packages org.mongodb.spark:mongo-spark-connector_2.12:10.2.1 {SPARK_JOBS_DIR}/job3.py",
        execution_timeout=timedelta(hours=2),
    )

    t4_gold = BashOperator(
        task_id="job4_gold_postgres",
        bash_command=f"spark-submit --packages org.mongodb.spark:mongo-spark-connector_2.12:10.2.1,org.postgresql:postgresql:42.7.1 {SPARK_JOBS_DIR}/job4.py",
        execution_timeout=timedelta(hours=1),
    )

    t0_check >> t1_download >> t2_bronze >> t3_silver >> t4_gold
