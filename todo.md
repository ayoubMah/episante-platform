## ✅ Phase 0 — Architecture & Foundations

### 0.1 — Define Functional Scope

- [ ] Identify core features for MVP:
  - [ ] Doctor management
  - [ ] Patient management
  - [ ] Appointment booking
  - [ ] Wearable ingestion + alerting
  - [ ] Analytics dashboard scope
- [ ] Define non-functional requirements:
  - [ ] Multi-VM deployment
  - [ ] High availability (simulation)
  - [ ] Secure secrets storage
  - [ ] CI/CD pipeline

### 0.2 — Define Global Architecture

- [ ] Create a complete architecture map:
  - [ ] Frontend stack (React + Vite + Tailwind)
  - [ ] Backend microservices (Spring Boot 3 + Java 21)
  - [ ] Databases (PostgreSQL per service)
  - [ ] Streaming (Kafka)
  - [ ] Realtime compute (Spark Streaming)
  - [ ] Big Data pipeline (HDFS, MongoDB, Postgres DWH)
  - [ ] Key Vault (HashiCorp Vault)
  - [ ] Gateway (Nginx Reverse Proxy)
  - [ ] Firewall (IPFire)
- [ ] Confirm VM topology:
  - [ ] Frontend VM
  - [ ] Backend VM
  - [ ] Data VM
  - [ ] Kafka VM
  - [ ] BigData VM
  - [ ] Gateway VM
  - [ ] Firewall VM (already exists)

### 0.3 — Define Network & IP Plan

- [ ] Set static IPs for each VM:
  - [ ] frontend-vm → 172.20.0.xx
  - [ ] backend-vm → 172.20.0.xx
  - [ ] data-vm → 172.20.0.xx
  - [ ] kafka-vm → 172.20.0.xx
  - [ ] bigdata-vm → 172.20.0.xx
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

- [ ] Backend stack:
  - [ ] Java 21
  - [ ] Spring Boot 3.2+
  - [ ] Flyway
  - [ ] Postgres 16
  - [ ] WebFlux or MVC (choose)
- [ ] Frontend stack:
  - [ ] React 18
  - [ ] Vite
  - [ ] Tailwind CSS
  - [ ] TypeScript
- [ ] Streaming:
  - [ ] Kafka latest
  - [ ] Kafka UI (optional)
- [ ] Big Data:
  - [ ] Spark 3.5
  - [ ] Hadoop HDFS
  - [ ] MongoDB 6
  - [ ] Postgres DWH
- [ ] DevOps:
  - [ ] Docker & Docker Compose
  - [ ] Jenkins
  - [ ] HashiCorp Vault OSS
  - [ ] Nginx reverse proxy

### 0.6 — Prepare VM Deployment Strategy

- [ ] VM creation strategy (academic infra):
  - [ ] Decide CPU/RAM per VM
  - [ ] Enable SSH access
  - [ ] Prepare scripts for installation
- [ ] OS setup:
  - [ ] Install Ubuntu 22.04 on each VM
  - [ ] Install Docker & Docker Compose
  - [ ] Install Java 21 on backend VM
  - [ ] Install Nginx on gateway VM

### 0.7 — Draw All Diagrams

- [ ] Architecture Diagram
- [ ] VM Topology
- [ ] Network Flow
- [ ] Data Pipeline
- [ ] CI/CD Flow
- [ ] Export diagrams as PNG/SVG
- [ ] Add to docs folder

### 0.8 — Define Standards

- [ ] API naming conventions
- [ ] Branching strategy
- [ ] Commit conventions
- [ ] Code style (backend + frontend)
- [ ] Log formats
- [ ] Secrets policy

### 0.9 — Pre-Phase Validation

- [ ] All diagrams validated
- [ ] All repos created
- [ ] VM plan ready
- [ ] Tech stack locked
- [ ] Network/IP plan validated
- [ ] Architecture approved
