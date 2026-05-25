# EpiSante — Deployment Guide

## VM Topology

| VM | IP | Role | Services |
|----|-----|------|----------|
| **Gateway** | 172.20.0.60 | Entry point | Nginx (HTTPS), TLS termination |
| **Backend** | 172.20.0.20 | Application | Auth, Doctor, Patient, Appointment, Notification |
| **Data** | 172.20.0.40 | Persistence | PostgreSQL (4 instances), Kafka |
| **Streaming** | 172.20.0.50 | Real-time | Ingest Wearables, Health Alert Engine |
| **Security** | 172.20.0.30 | Secrets | HashiCorp Vault |
| **Frontend** | 172.20.0.10 | UI | React SPA (served by Nginx) |
| **CICD** | 172.20.0.80 | Automation | Jenkins |
| **Firewall** | 172.20.0.1 | Network | IPFire (GREEN/LAN) |

---

## Prerequisites (all VMs)

- Docker Engine 24+
- Docker Compose v2
- SSH access between CICD VM and all other VMs
- Git

---

## Local Development (Single Machine)

### 1. Build shared library

```bash
cd episante-common
./mvnw clean install -DskipTests
```

### 2. Generate TLS certificates

```bash
cd gateway
sh generate-certs.sh
```

### 3. Create environment file

```bash
cp infra/env/postgres.env.example infra/compose/.env
# Edit .env with real credentials
```

### 4. Start everything

```bash
cd infra/compose
docker-compose up -d --build
```

### 5. Verify

| Service | URL | Expected |
|---------|-----|----------|
| Frontend | https://localhost:8443 | Login page |
| API Gateway | https://localhost:8443/api/auth/login | 405 (method not allowed) |
| Vault | http://localhost:8200 | Vault UI |
| Prometheus | http://localhost:9090/targets | All targets UP |
| Grafana | http://localhost:3001 | Dashboard |
| Jenkins | http://localhost:8080 | Setup wizard |

---

## Multi-VM Deployment

### Gateway VM

```bash
cd infra/compose
docker-compose -f docker-compose.gateway.yml up -d
```

Exposes ports 80 (HTTP redirect) and 443 (HTTPS).

### Backend VM

Deploy each service individually or use the compose override:

```bash
docker-compose -f docker-compose.backend.yml up -d
```

### Data VM

```bash
docker-compose -f docker-compose.data.yml up -d
```

### Kafka VM

```bash
docker-compose -f docker-compose.kafka.yml up -d
```

### Frontend VM

```bash
docker-compose -f docker-compose.frontend.yml up -d
```

---

## Network & Firewall

### IPFire Rules (GREEN zone)

| Source | Destination | Port | Protocol | Purpose |
|--------|------------|------|----------|---------|
| Any | Gateway | 80, 443 | TCP | HTTP/HTTPS |
| Gateway | Backend | 9091-9097 | TCP | API routing |
| Gateway | Frontend | 80 | TCP | SPA serving |
| Backend | Data | 5432 | TCP | PostgreSQL |
| Backend | Kafka | 9092 | TCP | Event streaming |
| Backend | Security | 8200 | TCP | Vault secrets |
| Streaming | Kafka | 9092 | TCP | Telemetry pipeline |
| Streaming | Backend | 9091 | TCP | Patient batch lookup |
| CICD | All | 22 | TCP | SSH deploy |

### DNS / Hosts

Each VM should resolve service names. In Docker Compose, the `episante_net` bridge network handles this automatically. For multi-VM, add entries to `/etc/hosts` or use DNS.

---

## Secrets Management

All secrets are stored in **HashiCorp Vault** (KV v1 engine at `secret/episante`).

Stored secrets:
- `jwt-secret` — JWT signing key
- `db-password-patient` — Patient DB password
- `db-password-doctor` — Doctor DB password
- `db-password-appointment` — Appointment DB password
- `db-password-auth` — Auth DB password

Spring Boot services auto-fetch from Vault on startup via `spring.config.import: vault://`.

**Never store secrets in**: Git, Jenkins credentials, application.yaml, environment variables in CI logs.

---

## TLS Certificates

### Development (self-signed)

```bash
cd gateway
sh generate-certs.sh
```

### Production

Replace `gateway/certs/server.crt` and `gateway/certs/server.key` with certificates from your CA. Update `nginx.conf` if using a chain file:

```nginx
ssl_certificate     /etc/nginx/certs/fullchain.pem;
ssl_certificate_key /etc/nginx/certs/privkey.pem;
```

---

## Monitoring

| Tool | Port | Purpose |
|------|------|---------|
| **Prometheus** | 9090 | Metrics scraping (actuator/prometheus) |
| **Grafana** | 3001 | Dashboards (admin/admin) |
| **Loki** | 3100 | Log aggregation |
| **Promtail** | — | Log shipping from Docker containers |

Grafana auto-provisions with Prometheus and Loki datasources. The "EpiSante Overview" dashboard ships pre-configured.

---

## CI/CD with Jenkins

Jenkins runs on port 8080 with JDK 21. The `Jenkinsfile` at the repo root defines the pipeline:

1. **Build**: `episante-common` first (shared library)
2. **Test**: Parallel compilation of all 6 Java services
3. **Frontend**: `npm ci` + TypeScript check + `npm run build`
4. **Docker**: Parallel image builds for all services
5. **Deploy**: Auto-deploy on master via `docker-compose up -d`

Jenkins uses `VAULT_ADDR` to fetch secrets at runtime — no credentials stored in Jenkins.

---

## Rollback

```bash
cd infra/compose
docker-compose down
docker-compose up -d --build
```

For a specific service:

```bash
docker-compose up -d --build --no-deps <service-name>
```
