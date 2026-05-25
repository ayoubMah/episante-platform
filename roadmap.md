# EpiSante — Project Roadmap

## Phase 0 — Architecture & Foundations

- [x] Microservices list & responsibilities
- [x] VM topology
- [x] Network diagram (gateway, backend, data, Kafka, big data)
- [x] Chosen technologies documented:
  - [x] Spring Boot, Java 21
  - [x] PostgreSQL
  - [x] React + Vite + Tailwind
  - [x] Kafka, Spark Streaming
  - [x] Hadoop, MongoDB, Postgres DW
  - [x] HashiCorp Vault
  - [x] Jenkins CI/CD
- [x] Docker + Docker Compose structure for each service

---

## Phase 1 — Core CRUD (Local Development)

- [x] Doctor Service CRUD
- [x] Patient Service CRUD
- [x] Appointment Service CRUD
- [x] Shared API gateway routes (local Nginx reverse proxy)
- [x] Local PostgreSQL databases for each service
- [x] Flyway migrations (V1 schema, V2 seed data)
- [x] Local frontend consuming backend APIs
- [x] Basic authentication (JWT via auth-service)

---

## Phase 2 — Local Infrastructure Simulation

- [x] Local Nginx reverse proxy
- [x] Local microservices network (docker-compose networks)
- [x] Local multi-DB setup (patientdb, doctordb, appointmentdb)
- [x] Local Key Vault setup (HashiCorp Vault dev mode)
- [x] Local Kafka cluster (single-node)
- [x] Local Kafka Streaming (reading dummy data)
- [x] Local monitoring (Prometheus + Grafana)

---

## Phase 3 — Deployment to University VMs

- [x] Deploy Frontend VM
- [x] Deploy Backend VM (microservices + docker-compose)
- [x] Deploy Data VM (PostgreSQL cluster)
- [x] Deploy Gateway VM (Nginx reverse proxy)
- [x] Configure firewall + routing (IPFire GREEN/LAN network)
- [x] Frontend → Gateway → Backend pipeline fully functional on VMs

---

## Phase 4 — Security Layer (Key Vault)

- [x] Install HashiCorp Vault on Security VM
- [x] Move secrets to Vault:
  - [x] DB passwords
  - [x] Kafka credentials
  - [x] JWT secret
- [x] Connect Spring Boot to Vault
- [x] Secure communication (HTTPS / TLS)

---

## Phase 5 — Real-Time Engine (Kafka + Streams)

- [x] Kafka producer (Ingest Wearables Service)
- [x] Kafka consumer (Notification Service)
- [x] Kafka Streams reads sensor stream
- [x] Kafka Streams publishes alerts back to Kafka
- [x] Alerts consumed by Health Alert Engine
- [x] Doctor UI receives alert notification

---

## Phase 6 — Smartwatch Simulation

- [x] Python simulator (`scripts/wearable-simulator/`)
- [x] Sends heart rate / oxygen / fall detection JSON payloads
- [x] Configurable frequency (`--interval`)
- [x] Stress test scenarios (`--stress --workers 20`, 100+ messages/sec)
- [x] Live terminal dashboard (throughput, latency, per-patient stats)

---

## Phase 7 — Big Data Analytics Pipeline

- [x] PySpark scraping jobs (job1 — download CSV)
- [x] Hadoop HDFS (Bronze layer — job2, partitioned Parquet)
- [x] MongoDB Silver layer (job3, incremental with state management)
- [x] Postgres DW Gold layer (job4, KPI aggregation)
- [x] ETL: Spark → Bronze → Silver → Gold (Airflow DAG)
- [ ] Power BI dashboards (doctors, admin analytics)

---

## Phase 8 — CI/CD Automation with Jenkins

- [x] Jenkins pipeline for backend (build → test → push → deploy)
- [x] Jenkins pipeline for frontend
- [x] Jenkins pipeline for gateway configs
- [x] Secrets via Vault (never in Jenkins)
- [x] Auto-redeploy on VM after push to master

---

## Phase 9 — Polishing, Observability & Documentation

- [x] Logging (Loki + Promtail → Grafana)
- [x] Monitoring dashboards (Prometheus + Grafana)
- [x] Final documentation:
  - [x] Architecture docs (README.md + mermaid diagrams)
  - [x] Deployment guide (docs/deployment.md)
  - [x] API documentation (docs/api.md)
- [x] Cleanup & optimization

---

## Final Goal

- [x] Full microservices platform deployed across multiple VMs
- [x] CRUD medical system functional
- [x] Real-time wearable alerts operational
- [x] Big data analytics pipeline complete
- [x] Secure secret management implemented
- [x] CI/CD automation working
- [x] Professional dev/ops architecture documented
