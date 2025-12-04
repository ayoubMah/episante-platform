## ✅ Phase 0 — Architecture & Foundations

### 0.1 — Define Functional Scope

- [x] Identify core features for MVP:
  - [x] Doctor management
  - [x] Patient management
  - [x] Appointment booking
  - [x] Wearable ingestion + alerting
  - [x] Analytics dashboard scope
- [x] Define non-functional requirements:
  - [x] Multi-VM deployment
  - [x] High availability (simulation)
  - [x] Secure secrets storage
  - [x] CI/CD pipeline

### 0.2 — Define Global Architecture

- [ ] Create a complete architecture map:
  - [x] Frontend stack (React 19 + Vite 7 + Tailwind 3.4 and TS 5.9)
  - [x] Backend microservices (Spring Boot 4 + Java 21 and javac 21)
  - [ ] Databases (PostgreSQL per service)
  - [ ] Streaming (Kafka)
  - [ ] Realtime compute (Spark Streaming)
  - [ ] Big Data pipeline (HDFS, MongoDB, Postgres DWH)
  - [ ] Key Vault (Docker Secrets)
  - [ ] Gateway (Nginx Reverse Proxy)
  - [ ] Firewall (IPFire)
- [x] Confirm VM topology:
  - [x] Frontend VM
  - [x] Backend VM
  - [x] Data VM
  - [ ] Kafka VM
  - [x] BigData VM
  - [ ] Gateway VM
  - [x] Firewall VM (already exists)

### 0.3 — Define Network & IP Plan

- [x] Set static IPs for each VM:
  - [x] frontend-vm → 172.20.0.xx
  - [x] backend-vm → 172.20.0.xx
  - [x] data-vm → 172.20.0.xx
  - [ ] kafka-vm → 172.20.0.xx
  - [x] bigdata-vm → 172.20.0.xx
  - [ ] gateway-vm → 172.20.0.xx
- [ ] Define subnets:
  - [ ] GREEN network
  - [ ] DMZ (optional)
- [ ] Define firewall rules:
  - [ ] Allow frontend → gateway
  - [ ] Allow gateway → backend
  - [ ] Allow backend → data
  - [ ] Allow backend ↔ Kafka
  - [ ] Allow Spark → Kafka

### 0.4 — Setup Repositories & Structure

- [ ] GitHub repo structure:
  - [ ] frontend/
  - [ ] backend/:
    - [ ] patient-service
    - [ ] doctor-service
    - [ ] appointment-service
    - [ ] ingest-service
    - [ ] alert-engine
  - [ ] infra/:
    - [ ] docker-compose files
    - [ ] gateway configs
    - [ ] keyvault setup
    - [ ] kafka setup
  - [ ] bigdata/:
    - [ ] spark jobs
    - [ ] pyspark scraper
  - [ ] docs/ (diagrams, roadmap, todo)
- [ ] Add documentation templates:
  - [ ] README.md
  - [ ] architecture.md
  - [ ] roadmap.md
  - [ ] todo.md

### 0.5 — Choose Technologies & Versions

- [x] Backend stack:
  - [x] Java 21
  - [x] Spring Boot 4
  - [x] Flyway
  - [x] Postgres 16
  - [ ] WebFlux or MVC (choose)
- [x] Frontend stack:
  - [x] React 19
  - [x] Vite
  - [x] Tailwind CSS
  - [x] TypeScript
- [ ] Streaming:
  - [x] Kafka latest
- [ ] Big Data:
  - [ ] Spark 3.5
  - [ ] Hadoop HDFS
  - [ ] MongoDB 6
  - [ ] Postgres DWH
- [ ] DevOps:
  - [x] Docker & Docker Compose
  - [x] Jenkins
  - [x] Docker Secrets for key vault
  - [x] Nginx reverse proxy


### 0.6 — Draw All Diagrams

- [x] Architecture Diagram
- [ ] VM Topology
- [ ] Network Flow
- [ ] Data Pipeline
- [ ] CI/CD Flow
- [ ] Export diagrams as PNG/SVG
- [ ] Add to docs folder

### 0.7 — Define Standards

- [ ] API naming conventions
- [ ] Branching strategy
- [ ] Commit conventions
- [ ] Code style (backend + frontend)
- [ ] Log formats
- [ ] Secrets policy

### 0.8 — Pre-Phase Validation

- [ ] All diagrams validated
- [ ] All repos created
- [ ] VM plan ready
- [ ] Tech stack locked
- [ ] Network/IP plan validated
- [ ] Architecture approved
