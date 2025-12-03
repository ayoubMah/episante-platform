# 🏥 **EpiSante — Microservices Medical Platform**

_A fully containerized, production-ready microservices system inspired by Doctolib._

EpiSante is a **distributed medical platform** built using a **microservices architecture**, where **each service runs in its own Docker container**, and each service has **its own dedicated PostgreSQL database** to ensure isolation, scalability, and clean DevOps practices.

---


```mermaid
flowchart TB

%% USER + FIREWALL
subgraph FIREWALL["Firewall VM - Reverse Proxy"]
    RP[Reverse Proxy / Gateway]
end
UB[User Browser] --> RP

%% FRONTEND
subgraph FRONT["Frontend VM"]
    FE[React Frontend]
end
RP --> FE

%% BACKEND
subgraph BACKEND["Backend VM - Microservices"]
    subgraph MS["Spring Boot Services"]
        PATIENT[Patient Service]
        DOCTOR[Doctor Service]
        APPT[Appointment Service]
        INGEST[Ingest Wearables]
        ALERT[Health Alert Engine]
    end
    PATIENT_DB[(Patient DB)]
    DOCTOR_DB[(Doctor DB)]
    APPT_DB[(Appointments DB)]
    INGEST_DB[(Wearables DB)]
    ALERT_DB[(Alerts DB)]
end
FE --> MS
PATIENT --> PATIENT_DB
DOCTOR --> DOCTOR_DB
APPT --> APPT_DB
INGEST --> INGEST_DB
ALERT --> ALERT_DB

%% KEY VAULT
subgraph VAULT["Key Vault"]
    KV[Secrets: DB Pass, JWT, Kafka]
end
KV --> PATIENT
KV --> DOCTOR
KV --> APPT
KV --> INGEST
KV --> ALERT

%% KAFKA
subgraph KAFKA_VM["Kafka VM"]
    KAFKA[Kafka Cluster]
end
INGEST -->|publish readings| KAFKA
KAFKA -->|consume to detect issues| ALERT
ALERT --> DOCTOR

%% SPARK (Realtime)
subgraph SPARK_RT["Spark VM - Realtime"]
    SPARK[Apache Spark Streaming]
end
KAFKA -->|realtime stream| SPARK
SPARK -->|publish alerts| KAFKA

%% BIG DATA (Anir)
subgraph SCRAPING["PySpark Scraping"]
    SCRAPER[PySpark Jobs]
end

subgraph HDFS_BRONZE["Hadoop - Bronze Layer"]
    HDFS[(HDFS Raw Data)]
end

subgraph SILVER["MongoDB - Silver Layer"]
    SILVER_DB[(Cleaned Data)]
end

subgraph GOLD["Postgres - Gold Layer"]
    GOLD_DB[(Analytical Warehouse)]
end

subgraph BI["Power BI"]
    PBI[Dashboards]
end

%% Links: Microservices <-> Big Data

%% Real-time medical readings exported to Big Data
KAFKA -->|mirror topic| HDFS

%% Alerts stored for ML training
ALERT -->|export alerts| HDFS

%% Scraper (web/API) -> Demo data -> Bronze
SCRAPER --> HDFS

%% Bronze -> Silver -> Gold
HDFS -->|Spark ETL| SILVER_DB
SILVER_DB -->|Spark ETL| GOLD_DB

%% BI reading from Gold layer
GOLD_DB --> PBI

%% Backend reading analytics
DOCTOR -->|analytics API| GOLD_DB
```

---

# 🚀 **Main Technologies**

- **Spring Boot 3+** (backend microservices)

- **React + Vite + Tailwind** (frontend)

- **PostgreSQL (one per service)**

- **Docker & Docker Compose**

- **Nginx Reverse Proxy** (gateway)

- **Flyway** (DB migrations)

- **Prometheus + Grafana** (monitoring)

- **Loki** (logs)

- **Mermaid diagrams** (architecture documentation)


---

# 📦 **Architecture Overview**

EpiSante adopts a **real microservices architecture**:

- Each business domain = **one microservice**

- Each microservice = **one isolated PostgreSQL database**

- Communication internal = **REST over Docker network**

- External traffic routed through **Nginx Gateway**

- Everything containerized


---

# 🧱 **Project Structure**

```css
episante/
│
├── patient-service/
│   ├── src/main/java
│   ├── src/main/resources/
│   │     ├── application.yaml
│   │     └── db/migration/
│   │            ├── V1__init.sql
│   │            └── V2__seed_patients.sql
│   └── Dockerfile
│
├── doctor-service/
├── appointment-service/
├── auth-service/
├── notification-service/
│
├── frontend/
│
├── gateway/
│   └── nginx.conf
│
├── postgres/
│   ├── patient-db/
│   ├── doctor-db/
│   ├── appointment-db/
│   ├── auth-db/
│   └── notification-db/
│
├── infra/
│   ├── docker-compose.yml
│   ├── network/
│   ├── certbot/
│   └── scripts/
│
└── monitoring/
    ├── grafana/
    ├── prometheus/
    └── loki/
```

---

# 🔗 **Container Communication (Mermaid Diagram)**

This diagram shows how each component talks to others inside Docker.
```mermaid
flowchart TD
    %% Clients
    User((User Browser))

    %% External Proxy
    Nginx[NGINX Reverse Proxy<br/>gateway]

    %% Frontend
    Frontend[Frontend React<br/>Container]

    %% Services
    PatientSvc[Patient Service<br/>Spring Boot]
    DoctorSvc[Doctor Service<br/>Spring Boot]
    AppointmentSvc[Appointment Service<br/>Spring Boot]
    AuthSvc[Auth Service<br/>Spring Boot]
    NotifSvc[Notification Service<br/>Spring Boot]

    %% Databases
    PatientDB[(patient-db<br/>PostgreSQL)]
    DoctorDB[(doctor-db<br/>PostgreSQL)]
    AppointmentDB[(appointment-db<br/>PostgreSQL)]
    AuthDB[(auth-db<br/>PostgreSQL)]
    NotifDB[(notification-db<br/>PostgreSQL)]

    %% Connections
    User -->|HTTPS| Nginx
    Nginx --> Frontend
    Nginx -->|/api/patients| PatientSvc
    Nginx -->|/api/doctors| DoctorSvc
    Nginx -->|/api/appointments| AppointmentSvc
    Nginx -->|/api/auth| AuthSvc
    Nginx -->|/api/notifications| NotifSvc

    %% Microservices to DB
    PatientSvc --> PatientDB
    DoctorSvc --> DoctorDB
    AppointmentSvc --> AppointmentDB
    AuthSvc --> AuthDB
    NotifSvc --> NotifDB

```

---

# 🗄️ **Database Isolation Philosophy**

Each microservice **owns its own schema and database**:

|Microservice|Database|Purpose|
|---|---|---|
|patient-service|patientdb|Patients info|
|doctor-service|doctordb|Doctors info|
|appointment-service|appointmentdb|Appointment logic|
|auth-service|authdb|Users + roles|
|notification-service|notifdb|Email/SMS queue|

This architecture provides:

- Loose coupling

- Independent scaling

- Better security

- Easier CI/CD

- Failure isolation

- True microservices practice


---

# 🐳 **Running the Project**

Go to the infra folder:
```bash
cd infra
docker compose up -d

```

This command starts:

- All microservices

- All PostgreSQL DBs

- Nginx gateway

- Monitoring stack

- Frontend


To see running containers:

```bash
docker ps
```

To see logs of a specific service:

```bash
docker logs patient-service
docker logs patient-db
```

---

# 🧪 **Database Initialization**

Every database is initialized automatically through:

```bash
postgres/<service>-db/init/*.sql
```

These scripts run _only on first DB creation_ and handle:

- Schema creation

- Seed data

- Migration versioning (compatible with Flyway)


---

# 📡 **Service Endpoints**

|Service|Port|Example|
|---|---|---|
|patient-service|9091|`/api/patients`|
|doctor-service|9092|`/api/doctors`|
|appointment-service|9093|`/api/appointments`|
|auth-service|9094|`/api/auth/login`|
|notification-service|9095|`/api/notify`|
|frontend|3000|React app|
|nginx|80/443|gateway|

---

# 📊 **Monitoring**

### After running:

```bash
http://localhost:3000  - Grafana
http://localhost:9090  - Prometheus
http://localhost:3100  - Loki
```

Dashboards:

- JVM metrics

- Spring Boot metrics

- Database metrics

- Container metrics


---

# 🛡️ **Reverse Proxy (Nginx)**

All traffic enters through the gateway:

```bash
/api/patients → patient-service
/api/doctors → doctor-service
```

This ensures:

- HTTPS termination

- Rate limiting (optional)

- Authentication (optional)

- Logging and auditability


---

# 🧩 **Next Steps**

Planned extensions:

- Add Kafka for event-driven communication

- Add Redis for caching

- Integrate Keycloak for authentication

- Add Spark or Python ML service