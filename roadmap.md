#  EPISANTE — Project Checklist

##  Phase 0 — Architecture & Foundations

- [ ] Microservices list & responsibilities
- [ ] VM topology
- [ ] Network diagram (gateway, backend, data, Kafka, big data)
- [ ] Chosen technologies documented:
  - [ ] Spring Boot, Java 21
  - [ ] PostgreSQL
  - [ ] React + Vite + Tailwind
  - [ ] Kafka, Spark Streaming
  - [ ] Hadoop, MongoDB, Postgres DW
  - [ ] HashiCorp Vault
  - [ ] Jenkins CI/CD
- [ ] Docker + Docker Compose structure for each service

---

##  Phase 1 — Core CRUD (Local Development)

- [ ] Doctor Service CRUD
- [ ] Patient Service CRUD
- [ ] Appointment Service CRUD
- [ ] Shared API gateway routes (local Nginx reverse proxy)
- [ ] Local PostgreSQL databases for each service
- [ ] Flyway migrations (V1 schema, V2 seed data)
- [ ] Local frontend consuming backend APIs
- [ ] Basic authentication (optional)

---

##  Phase 2 — Local Infrastructure Simulation

- [ ] Local Nginx reverse proxy
- [ ] Local microservices network (docker-compose networks)
- [ ] Local multi-DB setup (patientdb, doctordb, appointmentdb)
- [ ] Local Key Vault setup (HashiCorp Vault dev mode)
- [ ] Local Kafka cluster (single-node)
- [ ] Local Spark Streaming (reading dummy data)
- [ ] Local monitoring (optional: Prometheus + Grafana)

---

##  Phase 3 — Deployment to University VMs

- [ ] Deploy Frontend VM
- [ ] Deploy Backend VM (microservices + docker-compose)
- [ ] Deploy Data VM (PostgreSQL cluster)
- [ ] Deploy Gateway VM (Nginx reverse proxy)
- [ ] Configure firewall + routing (IPFire GREEN/LAN network)
- [ ] Frontend → Gateway → Backend pipeline fully functional on VMs

---

## Phase 4 — Security Layer (Key Vault)

- [ ] Install HashiCorp Vault on Security VM
- [ ] Move secrets to Vault:
  - [ ] DB passwords
  - [ ] Kafka credentials
  - [ ] JWT secret
- [ ] Connect Spring Boot to Vault
- [ ] Secure communication (HTTPS / TLS)

---

## Phase 5 — Real-Time Engine (Kafka + Spark)

- [ ] Kafka producer (INGEST service)
- [ ] Kafka consumer (ALERT service)
- [ ] Spark Streaming reads sensor stream
- [ ] Spark publishes alerts back to Kafka
- [ ] Alerts consumed by Health Alert Engine
- [ ] Doctor UI receives alert notification

---

## Phase 6 — Smartwatch Simulation

- [ ] Python or Java simulator
- [ ] Sends heart rate / oxygen / fall detection JSON payloads
- [ ] Configurable frequency
- [ ] Stress test scenarios (100+ messages/sec)
- [ ] Visualization dashboard (optional)

---

## Phase 7 — Big Data Analytics Pipeline

- [ ] PySpark scraping jobs
- [ ] Hadoop HDFS (Bronze layer)
- [ ] MongoDB Silver layer
- [ ] Postgres DW Gold layer
- [ ] ETL: Spark → Bronze → Silver → Gold
- [ ] Power BI dashboards (doctors, admin analytics)

---

## Phase 8 — CI/CD Automation with Jenkins

- [ ] Jenkins pipeline for backend (build → test → push → deploy)
- [ ] Jenkins pipeline for frontend
- [ ] Jenkins pipeline for gateway configs
- [ ] Secrets via Vault (never in Jenkins)
- [ ] Auto-redeploy on VM after push to GitHub

---

## Phase 9 — Polishing, Observability & Documentation

- [ ] Logging (ELK or Loki optional)
- [ ] Monitoring dashboards
- [ ] Final documentation:
  - [ ] Architecture.md
  - [ ] roadmap.md
  - [ ] todo.md
  - [ ] deployment.md
  - [ ] API documentation
- [ ] Cleanup & optimization

---

##  Final Goal

- [ ] Full microservices platform deployed across multiple VMs
- [ ] CRUD medical system functional
- [ ] Real-time wearable alerts operational
- [ ] Big data analytics pipeline complete
- [ ] Secure secret management implemented
- [ ] CI/CD automation working
- [ ] Professional dev/ops architecture documented