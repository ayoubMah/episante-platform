from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.python import PythonOperator, ShortCircuitOperator
from airflow.utils.dates import days_ago
from datetime import datetime, timedelta
from pathlib import Path

# ── Config ────────────────────────────────────────────────────────────────────
SPARK_JOBS_DIR = "/home/spark/spark_jobs"
PYTHON_BIN     = "/home/spark/spark_jobs/envDev/bin/python3"
URLS_FILE      = f"{SPARK_JOBS_DIR}/config/urls_to_process.txt"

default_args = {
    "owner": "sirius",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
    "email_on_failure": False,
    "email_on_retry": False,
}

# ── Fonction : vérifier s'il y a des URLs pending ─────────────────────────────
def check_pending_urls():
    """
    Retourne True s'il y a des URLs pending, False sinon.
    ShortCircuitOperator arrête le DAG proprement si False.
    """
    filepath = Path(URLS_FILE)
    if not filepath.exists():
        print(" Fichier URLs introuvable")
        return False

    with filepath.open("r") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("|")
            if len(parts) == 3 and parts[2].strip().lower() == "pending":
                print(f" URL pending trouvée : {parts[1].strip()}")
                return True

    print("ℹ Aucune URL pending — pipeline ignorée")
    return False


with DAG(
    dag_id="sirius_pipeline",
    description="Pipeline SPARCS : Download → Bronze → Silver → Gold",
    default_args=default_args,
    start_date=days_ago(1),
    schedule_interval="0 2 * * *",  # Tous les jours à 2h du matin
    catchup=False,
    tags=["sirius", "sparcs", "pipeline"],
) as dag:

    # ── Task 0 
    t0_check = ShortCircuitOperator(
        task_id="check_pending_urls",
        python_callable=check_pending_urls,
    )

    # ── Task 1
    t1_download = BashOperator(
        task_id="job1_download",
        bash_command=f"{PYTHON_BIN} {SPARK_JOBS_DIR}/job11.py",
        execution_timeout=timedelta(hours=2),
    )

    # ── Task 2 
    t2_bronze = BashOperator(
        task_id="job2_bronze",
        bash_command=f"{PYTHON_BIN} {SPARK_JOBS_DIR}/job22.py",
        execution_timeout=timedelta(hours=2),
    )

    # ── Task 3
    t3_silver = BashOperator(
        task_id="job3_silver",
        bash_command=f"{PYTHON_BIN} {SPARK_JOBS_DIR}/job33.py",
        execution_timeout=timedelta(hours=2),
    )

    # ── Task 4 
    t4_gold = BashOperator(
        task_id="job4_gold",
        bash_command=f"{PYTHON_BIN} {SPARK_JOBS_DIR}/job44.py",
        execution_timeout=timedelta(hours=1),
    )

    # ── Dépendances 
    t0_check >> t1_download >> t2_bronze >> t3_silver >> t4_gold