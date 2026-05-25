# Big Data Analytics Pipeline

4-stage ETL pipeline using Apache Spark, HDFS, MongoDB, and PostgreSQL.

## Architecture

```
External CSV    →    HDFS (Bronze)    →    MongoDB (Silver)    →    PostgreSQL (Gold)
  job1.py              job2.py               job3.py                 job4.py
  Download             Raw Parquet           Cleaned docs            KPI aggregates
```

Orchestrated by an Airflow DAG (`dag.py`) that runs daily at 02:00.

## Pipeline Stages

### Job 1 — Download (External → Local CSV)
- Reads `config/urls_to_process.txt` for pending data source URLs
- Downloads the smallest pending year
- Stores raw CSV in local filesystem

### Job 2 — Bronze (CSV → HDFS Parquet)
- Reads raw CSV with PySpark
- Cleans column names (spaces/hyphens → underscores)
- Writes partitioned Parquet to HDFS (`/bronze/sparcs_parquet/Discharge_Year=YYYY`)
- Deletes local CSV after successful write

### Job 3 — Silver (HDFS → MongoDB)
- Reads Bronze Parquet from HDFS (incremental via state file)
- Drops duplicates, nulls on required columns
- Transforms: region, county, age group, gender, severity, complexity
- Generates appointment duration estimates from complexity
- Writes structured documents to MongoDB `episante.sparcs_silver`

### Job 4 — Gold (MongoDB → PostgreSQL)
- Reads Silver from MongoDB (incremental delta)
- Aggregates KPIs: appointment counts, duration stats, by region/severity/age/gender
- Writes to PostgreSQL `episante_gold.kpi_appointments`

## Infrastructure

Start the big data stack:

```bash
cd infra/compose
docker-compose -f docker-compose.bigdata.yml up -d
```

| Service | Port | Purpose |
|---------|------|---------|
| HDFS NameNode | 9870 (UI), 9000 | Distributed storage (Bronze) |
| HDFS DataNode | — | Data storage |
| MongoDB | 27017 | Document store (Silver) |
| PostgreSQL DW | 5433 | Data warehouse (Gold) |
| Spark Master | 8088 (UI), 7077 | Compute engine |
| Spark Worker | — | Compute node |

## Configuration

### URL Config (`config/urls_to_process.txt`)

```
# Format: URL | YEAR | STATUS
https://example.com/data-2020.csv | 2020 | pending
https://example.com/data-2021.csv | 2021 | pending
```

Copy from `config/urls_to_process.txt.example` and add your data source URLs.

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `HDFS_NAMENODE` | `namenode` | HDFS NameNode hostname |
| `MONGO_HOST` | `mongodb` | MongoDB hostname |
| `DW_HOST` | `postgres-dw` | PostgreSQL DW hostname |
| `DW_PASSWORD` | `spark_pass` | PostgreSQL DW password |
| `RAW_CSV_DIR` | `/opt/spark-jobs/data/raw_csv` | Local CSV storage |
| `URLS_FILE` | `/opt/spark-jobs/config/urls_to_process.txt` | URL config path |
| `STATE_DIR` | `/opt/spark-jobs/state` | State files directory |

## Running Jobs Manually

```bash
# Submit via Spark
docker exec episante-spark-master spark-submit /opt/spark-jobs/job1.py
docker exec episante-spark-master spark-submit /opt/spark-jobs/job2.py
docker exec episante-spark-master spark-submit --packages org.mongodb.spark:mongo-spark-connector_2.12:10.2.1 /opt/spark-jobs/job3.py
docker exec episante-spark-master spark-submit --packages org.mongodb.spark:mongo-spark-connector_2.12:10.2.1,org.postgresql:postgresql:42.7.1 /opt/spark-jobs/job4.py
```

## State Management

Jobs 3 and 4 use JSON state files for incremental processing:
- `state/job3_state.json` — tracks processed years and last timestamp
- `state/job4_state.json` — tracks Gold-loaded years

If a job is interrupted (`status: "running"`), the next run performs a full reprocess.
