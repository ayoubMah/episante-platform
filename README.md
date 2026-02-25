# EpiSante — Medical Microservices Platform

EpiSante is a production-grade, distributed medical platform inspired by Doctolib. It leverages a modern microservices architecture to ensure high availability, data isolation, and scalable real-time health monitoring.

## System Architecture

![alt text](/docs/image.png)

### Core Architectural Principles

- **Database-per-Service**: Each microservice owns its own PostgreSQL instance. No shared schemas. Direct cross-database joins are strictly forbidden.

- **Event-Driven Architecture (EDA)**: Administrative workflows (like booking notifications) are decoupled using **Apache Kafka**, ensuring non-blocking, resilient processing.

- **Polyglot Microservices**: **Java/Spring Boot** powers the heavy transactional domain (ACID compliance), while **Go (Golang)** handles high-concurrency asynchronous tasks (Notifications, IoT ingestion).

- **Security-First**: Centralized configuration and secrets management via **HashiCorp Vault**. Authentication is stateless via shared JWT filters.

- **Shared Library**: Common DTOs, Event Contracts, and Security configurations are centralized in a custom Maven dependency (`episante-common`).

## Tech Stack


| **Layer**                   | **Technologies**                                                |
| --------------------------- | --------------------------------------------------------------- |
| **Backend (Transactional)** | Java 21, Spring Boot 3, Spring Security, Spring Cloud OpenFeign |
| **Backend (Async/IoT)**     | Go (Golang), segmentio/kafka-go                                 |
| **Data & Messaging**        | PostgreSQL 16, Apache Kafka (KRaft Mode), Flyway                |
| **Big Data / Telemetry**    | Apache Spark, MQTT (Planned Phase 2)                            |
| **Security**                | HashiCorp Vault (KV Secrets Engine), JWT                        |
| **Frontend**                | React, Vite, TypeScript, TailwindCSS, Axios                     |
| **Infrastructure**          | Docker Compose, Nginx (API Gateway)                             |


## Project Structure

```
episante/
├── appointment-service/         # Java: Booking management & Kafka Producer
├── auth-service/                # Java: Identity, JWT generation
├── doctor-service/              # Java: Doctor profiles & availability
├── episante-common/             # Java: Shared Library (DTOs, Events, Security Filters)
├── frontend/                    # React: TypeScript SPA (Vite)
├── gateway/                     # Nginx: Reverse Proxy & API Gateway routing
├── health-alert-engine/         # Go: Real-time telemetry alerts (Phase 2)
├── infra/
│   └── compose/                 # Docker Compose, .env, and orchestration
├── ingest-wearables-service/    # Go: High-throughput IoT wearable ingestion (Phase 2)
├── notification-service/        # Go: Kafka Consumer for email/SMS alerts
└── patient-service/             # Java: Patient records & PII
```

## Quick Start

### 1. Prerequisites

* Docker & Docker Compose
* Java 21 (for local builds)
* Go 1.24
* Node.js 20+

### 2. Prepare the Environment

Ensure your local Maven cache has the latest shared library:

Bash

```bash
cd episante-common
mvn clean install -DskipTests
```

### 3. Launch Infrastructure

Navigate to the compose directory and spin up the entire isolated network:

Bash

```bash
cd infra/compose
docker-compose up -d --build
```

### 4. Access the Platform

- **Web Portal (Frontend)**: `http://localhost:5173`

- **API Gateway (Backend Entrypoint)**: `http://localhost:8089/api/`

- **HashiCorp Vault UI**: `http://localhost:8200` (Token: `episante-super-secret-root-token`)


## Service Inventory


|**Service**|**Internal Port**|**Gateway Route**|**Primary Responsibility**|
|---|---|---|---|
|**Gateway (Nginx)**|8089|`/`|Single entry point, CORS, traffic routing|
|**Auth Service**|9094|`/api/auth/`|Authentication, JWT issuance|
|**Patient Service**|9091|`/api/patients/`|Patient CRUD, Medical History|
|**Doctor Service**|9092|`/api/doctors/`|Doctor CRUD, Specialties|
|**Appt Service**|9093|`/api/appointments/`|Scheduling, overlap validation, Event publishing|
|**Notif Service**|N/A|_Internal Kafka_|Listens to `appointment.created`, sends emails (Go)|
