# EpiSante — Medical Microservices Platform

EpiSante is a production-grade, distributed medical platform inspired by Doctolib. It leverages a modern microservices architecture to ensure high availability, data isolation, and scalable real-time health monitoring.

## System Architecture

![alt text](/docs/image.png)

### Core Architectural Principles

* **Database-per-Service**: Each microservice owns its own PostgreSQL instance. No shared schemas.
* **Security-First**: Centralized secrets management via HashiCorp Vault.
* **Polyglot Streaming**: High-velocity wearable data is ingested via Go, processed by Apache Spark, and managed by Kafka.
* **Operational Observability**: Full-stack monitoring using Prometheus, Grafana, and Loki.

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Backend** | Java 21, Spring Boot 4, Spring Security, OpenFeign, Go (Ingest/Alerts) |
| **Data & Streaming** | PostgreSQL, Apache Kafka, Apache Spark, Flyway |
| **Security** | HashiCorp Vault (Secrets & Transit Encryption), JWT |
| **Frontend** | React, Vite, TypeScript, TailwindCSS, TanStack Query |
| **Infrastructure** | Docker Compose, Nginx (Gateway), Prometheus, Grafana |

## Project Structure

```
episante/
├── backend/                # Spring Boot & Go Microservices
│   ├── auth-service/       # JWT & Identity Management
│   ├── patient-service/    # Patient Records & PII
│   ├── ingest-service/     # IoT/Wearable Data Ingest (Go)
│   └── ...                 # Other domain services
├── frontend/               # React TypeScript SPA
├── infra/                  # Orchestration & Configuration
│   ├── docker-compose.yml  # System-wide orchestration
│   ├── gateway/            # Nginx Reverse Proxy configs
│   └── vault/              # Security policies and setup
└── streaming/              # Spark jobs and Kafka definitions
```

## Quick Start

### 1. Prerequisites

* Docker & Docker Compose
* Java 21 (for local builds)
* Node.js 20+

### 2. Launch Infrastructure

Navigate to the infrastructure directory and spin up the environment:

```bash
cd infra
docker-compose up -d
```

### 3. Access the Platform

* **Web Portal**: `http://localhost:3000`
* **API Gateway**: `http://localhost:80/api/v1/`
* **Monitoring (Grafana)**: `http://localhost:3001`
* **Vault UI**: `http://localhost:8200`

## Service Inventory

| Service | Port | Primary Responsibility |
|---------|------|------------------------|
| **Gateway** | 80/443 | Traffic Routing & SSL Termination |
| **Auth** | 8081 | Authentication & RBAC |
| **Patient** | 8082 | Patient Profiles & Medical History |
| **Appt** | 8083 | Schedule & Booking Management |
| **Ingest** | 8085 | Real-time Wearable Telemetry (High Throughput) |
| **Alerts** | 8086 | Anomaly Detection & Notifications |

## Database Philosophy

We enforce strict service isolation. Communication between services occurs exclusively via **OpenFeign** (Sync) or **Kafka** (Async). Direct cross-database joins are strictly forbidden to ensure each service can be scaled or migrated independently.