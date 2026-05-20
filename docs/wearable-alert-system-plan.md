# Wearable Health Alert System — Implementation Plan

## Overview

Real-time pipeline: **Wearable Simulator** → **Ingest Service** (Kafka Producer) → **Health Alert Engine** (Kafka Streams) → **Notification Service** (Kafka Consumer) → Patient & Doctor alerts.

Alerts are **demographic-aware**: thresholds adapt based on age, gender, and the patient's assigned doctor is notified alongside the patient.

---

## Architecture Flow

```
┌──────────────────┐     ┌──────────────────────┐      ┌─────────────────────────┐
│  Wearable        │────▶│  Ingest Wearables   │────▶│  Kafka Topic            │
│  Simulator       │HTTP │  Service (Java)      │     │  wearable.telemetry.raw │
│  (Python/Java)   │     │  Port: 9096          │     └────────────┬────────────┘
└──────────────────┘     └──────────────────────┘                   │
                                                                    ▼
                                                     ┌──────────────────────────┐
                                                     │  Health Alert Engine     │
                                                     │  (Spring Boot + Kafka    │
                                                     │   Streams) Port: 9097    │
                                                     │                          │
                                                     │  • Enrich with patient   │
                                                     │    profile (age, gender, │
                                                     │    doctorId)             │
                                                     │  • Apply demographic     │
                                                     │    thresholds            │
                                                     │  • Generate alerts       │
                                                     └──────────────┬───────────┘
                                                                    │
                                          ┌─────────────────────────┤
                                          │                         │
                                          ▼                         ▼
                               ┌──────────────────┐     ┌──────────────────┐
                               │ Kafka Topic      │     │ Kafka Topic      │
                               │ health.alerts    │     │ health.alerts    │
                               │ (patient)        │     │ (doctor)         │
                               └────────┬─────────┘     └────────┬─────────┘
                                        │                        │
                                        ▼                        ▼
                               ┌──────────────────┐     ┌──────────────────┐
                               │ Notification     │     │ Frontend         │
                               │ Service (Go)     │     │ (WebSocket/SSE)  │
                               │ • Mock email     │     │ • Doctor alert   │
                               │ • Mock SMS       │     │   dashboard      │
                               └──────────────────┘     └──────────────────┘
```

---

## Step 1 — Define Shared Event Schemas (episante-common) ✅ DONE

### Created files

| File | Path |
|---|---|
| `WearableMetricEvent.java` (record) | `episante-common/src/main/java/.../event/WearableMetricEvent.java` |
| `HealthAlertEvent.java` (record) | `episante-common/src/main/java/.../event/HealthAlertEvent.java` |
| `AlertSeverity.java` (enum) | `episante-common/src/main/java/.../enums/AlertSeverity.java` |

### Modified files

| File | Change |
|---|---|
| `AlertType.java` | Added `LOW_TEMPERATURE`, `HIGH_BP_SYSTOLIC`, `HIGH_BP_DIASTOLIC` |
| `PatientResponseDTO.java` | Added `private UUID doctorId` |
| `PatientProfileRequest.java` | Added `private UUID doctorId` |

---

## Step 2 — Wearable Simulator (Python) ✅ DONE

**Location**: `scripts/wearable-simulator/`

### Created files

| File | Purpose |
|---|---|
| `simulator.py` | Generates realistic vital signs & sends via HTTP |
| `requirements.txt` | `requests`, `faker`, `numpy` |

### Behavior
- **5 hardcoded patients**: child (8F), adult (35M, 45M, 28F), elderly (72F)
- Demographic baselines: HR, BP, SpO2, temperature vary by age group & gender
- `--anomaly-rate` flag injects abnormal readings (tachycardia, hypertension, desaturation, fever)
- `--interval` flag controls batch frequency (default 2s)
- `--patients N` flag limits count
- Default target: `http://localhost:9096/api/wearable/metrics`
- Real-time console output with anomaly markers

---

## Step 3 — Ingest Wearables Service (Java, Port 9096) ✅ DONE

**Location**: `ingest-wearables-service/`

### Files created/modified

| File | Type |
|---|---|
| `pom.xml` | **Updated** — Spring Boot 3.3.5, added web/kafka/actuator/episante-common/lombok |
| `application.properties` | **Updated** — port 9096, Kafka bootstrap, topic name, actuator |
| `IngestWearablesServiceApplication.java` | **Updated** — `@EnableConfigurationProperties(WearableTopicProperties.class)` |
| `config/WearableTopicProperties.java` | **New** — `@ConfigurationProperties(prefix = "ingest.wearable")` |
| `config/KafkaTopicConfig.java` | **New** — auto-creates `wearable.telemetry.raw` (3 partitions) |
| `producer/WearableMetricProducer.java` | **New** — `KafkaTemplate<String, WearableMetricEvent>` with async callback |
| `controller/WearableController.java` | **New** — `POST /api/wearable/metrics` → publishes to Kafka |

### API

```
POST /api/wearable/metrics
Content-Type: application/json

{
  "patientId": "550e8400-e29b-41d4-a716-446655440000",
  "heartRate": 118,
  "bloodPressureSystolic": 145,
  "bloodPressureDiastolic": 95,
  "spO2": 97,
  "temperature": 37.2,
  "timestamp": "2026-05-19T10:30:00Z",
  "deviceId": "watch-001"
}
→ 201 Created
```

### Kafka
- Topic: `wearable.telemetry.raw` (3 partitions)
- Key: patientId (UUID as string) — ensures all metrics per patient are ordered

---

## Step 4 — Extend Patient Service (Add `doctorId`) ✅ DONE

### Changes made

| File | Change |
|---|---|
| `db/migration/V3__add_doctor_id.sql` | **New** — `ALTER TABLE patients ADD COLUMN doctor_id UUID` |
| `Patient.java` | Added `@Column(name = "doctor_id") private UUID doctorId` |
| `PatientService.java` | Added `findAllByIds(List<UUID>)`, `createProfile`/`update` now handle `gender` + `doctorId` |
| `PatientResponseDTO.java` | Already had `doctorId` from Step 1 |
| `PatientProfileRequest.java` | Added `private String gender` field |
| `PatientController.java` | `toDTO` now maps `doctorId` |
| `PatientInternalController.java` | **New** `POST /internal/patients/batch`, `toDTO` now maps all fields |

---

## Step 5 — Health Alert Engine (Java, Port 9097) ✅ DONE

**Location**: `health-alert-engine/`

### Created files

| File | Description |
|---|---|
| `pom.xml` | **Updated** — Spring Boot 3.3.5, added web/kafka-streams/spring-kafka/episante-common/lombok/actuator |
| `application.properties` | **Updated** — port 9097, Kafka Streams config, topic names, patient service URL |
| `AppConfig.java` | **New** — `RestTemplate` + `ThresholdEngine` beans |
| `ThresholdEngine.java` | **New** — Demographic thresholds per age group (infant/child/adolescent/adult/elderly) with gender adjustments |
| `PatientProfile.java` | **New** — Record holding patientId, dob, gender, doctorId |
| `PatientProfileFetcher.java` | **New** — REST client fetching profiles from `patient-service/internal/patients/batch` with `ConcurrentHashMap` cache |
| `AlertStreamTopology.java` | **New** — Kafka Streams topology: reads from `wearable.telemetry.raw`, enriches with profile, evaluates all metrics against thresholds, deduplicates (5-min cooldown), produces to `health.alerts.patient` + `health.alerts.doctor` |
| `AlertDedupCache.java` | **New** — In-memory cooldown per (patientId, alertType) |
| `KafkaTopicConfig.java` | **New** — Auto-creates both output topics (3 partitions each) |
| `AlertController.java` | **New** — `GET /api/alerts/{doctor,patient}/{id}` backed by in-memory ring buffer (1000 alerts) |
| `Dockerfile` | **New** — Multi-stage build |

### Approach

- **Patient profile cache**: REST cache (Phase 1) — fetches on demand from patient-service
- **Alert dedup**: 5-minute cooldown per (patientId, alertType) using in-memory `ConcurrentHashMap`
- **Output**: Branched stream to `health.alerts.patient` (always) and `health.alerts.doctor` (when doctorId present)

---

## Step 6 — Extend Notification Service (Go) ✅ DONE

**Location**: `notification-service/main.go`

### Changes made

| File | Change |
|---|---|
| `main.go` | Added `HealthAlertEvent` struct, `consumeHealthAlerts()` goroutine for both `health.alerts.patient` and `health.alerts.doctor` topics, graceful shutdown via `sync.WaitGroup` |

### Behavior

- **3 concurrent consumers** via goroutines + `sync.WaitGroup`:
  - `appointment.created` (existing) — mock email
  - `health.alerts.patient` — mock SMS to patient
  - `health.alerts.doctor` — mock email to doctor
- All share the same consumer group (`notification-group-go`)
- Graceful shutdown on SIGINT/SIGTERM

---

## Step 7 — Infrastructure Updates ✅ DONE

### Changes made

| File | Change |
|---|---|
| `ingest-wearables-service/Dockerfile` | **New** — multi-stage build (builds episante-common first, then service) |
| `infra/compose/docker-compose.yml` | **Updated** — added `ingest-wearables-service` (port 9096) and `health-alert-engine` (port 9097), added ingest-wearables to gateway `depends_on` |
| `gateway/nginx.conf` | **Updated** — added `/api/wearable` → `http://ingest-wearables-service:9096` |

### Kafka Topics

| Topic | Partitions | Created By |
|---|---|---|
| `wearable.telemetry.raw` | 3 | Ingest service `KafkaTopicConfig` |
| `health.alerts.patient` | 3 | Health alert engine `KafkaTopicConfig` |
| `health.alerts.doctor` | 3 | Health alert engine `KafkaTopicConfig` |

---

## Step 8 — Extend Frontend (Doctor Alert Dashboard) ✅ DONE

### Created/Modified files

| File | Change |
|---|---|
| `src/lib/api.ts` | Added `HealthAlertEvent` interface + `alertApi` (getDoctorAlerts, getPatientAlerts) |
| `src/pages/alerts/AlertsPage.tsx` | **New** — minimal dashboard with 5s polling, severity color-coding (CRITICAL=red, WARNING=yellow, INFO=blue), auto-sorted by severity |
| `src/App.tsx` | Added `/alerts` route for `ADMIN`/`DOCTOR` roles |
| `src/components/Header.tsx` | Added "Alerts" nav link for `DOCTOR`/`ADMIN` users |

### Nginx

| File | Change |
|---|---|
| `gateway/nginx.conf` | Added `/api/alerts` → `health-alert-engine:9097` |

---

## Pipeline Verification ✅ DONE

**Date**: 2026-05-20

The entire pipeline was verified end-to-end:
1. Existing historical data on `wearable.telemetry.raw` was reprocessed successfully
2. The engine stayed in **RUNNING** state (no ERROR transitions)
3. 5 patient profiles were fetched from patient-service via REST batch endpoint
4. Demographic thresholds were applied (age/gender-aware)
5. WARNING alerts were generated for elevated heart rate, blood pressure, and temperature
6. Alerts were published to both `health.alerts.patient` and `health.alerts.doctor` topics
7. Notification service consumed both topics and printed mock email/SMS
8. Frontend at `/alerts` displayed the alerts with severity color-coding

### Key Issue: JSON Type Header Deserialization

**Problem**: `JsonSerde` from Spring Kafka was failing with `The class 'wearableMetricEvent' is not in the trusted packages`. The root cause was twofold:

1. **Type header mismatch**: The producer (ingest-wearables-service) wrote a `_type` header containing `wearableMetricEvent` (decapitalized simple class name). The deserializer in the Streams topology couldn't resolve this short name to `com.upec.episantecommon.event.WearableMetricEvent`.
2. **Explicit serde bypasses properties**: Setting `spring.kafka.streams.properties.spring.json.trusted.packages=*` in `application.properties` worked for the default serde, but the topology created manual `new JsonSerde<>(WearableMetricEvent.class)` instances that didn't inherit this config.

**Fix**: Configure each `JsonSerde` instance to ignore the type header entirely:

```java
var serdeProps = new HashMap<String, Object>();
serdeProps.put("spring.json.use.type.headers", false);

var metricSerde = new JsonSerde<>(WearableMetricEvent.class);
metricSerde.configure(serdeProps, false);
```

This tells the `JsonDeserializer` to skip the `_type` header and deserialize directly to the specified target class.

---

## Step 9 — Testing Strategy

### 9.1 Unit Tests
- `ThresholdEngineTest` — verify correct thresholds for all age/gender combinations
- `AlertStreamProcessorTest` — use TopologyTestDriver from Kafka Streams

### 9.2 Integration Tests
- Start Kafka with Testcontainers
- Send metrics via ingest service REST API
- Verify alerts appear in output topics

### 9.3 Simulator Stress Test
- Run simulator with `--patients 100 --anomaly-rate 0.1`
- Monitor throughput: 100+ msgs/sec through the pipeline
- Measure end-to-end latency (metric → alert → notification)

---

## Summary of Services to Modify/Create

| Service | Action | Status |
|---|---|---|
| `episante-common` | Add event classes & AlertSeverity enum | ✅ Done |
| `patient-service` | Add doctorId, batch endpoint | ✅ Done |
| `ingest-wearables-service` | Full implementation | ✅ Done |
| `health-alert-engine` | Full implementation (Kafka Streams) | ✅ Done |
| `notification-service` | Add health alert consumer | ✅ Done |
| `frontend` | Doctor alert dashboard | ✅ Done |
| `infra/compose` | Add new services to docker-compose | ✅ Done |
| `gateway` | Add wearable route to nginx.conf | ✅ Done |
| `scripts/wearable-simulator` | Wearable simulator (Python) | ✅ Done |

---

## Timeline Estimate

| Step | Effort | Status | Dependencies |
|---|---|---|---|---|
| Step 1: Shared events & enums | 1h | ✅ Done | None |
| Step 2: Wearable simulator | 3h | ✅ Done | None |
| Step 3: Ingest service | 2h | ✅ Done | Step 1 |
| Step 4: Patient service extension | 1h | ✅ Done | None |
| Step 5: Health alert engine | 6h | ✅ Done | Step 1, Step 4 |
| Step 6: Notification extension | 2h | ✅ Done | Step 1 |
| Step 7: Docker & infra | 2h | ✅ Done | Steps 3, 5, 6 |
| Step 8: Frontend alerts | 4h | ✅ Done | Step 5 |
| Step 9: Testing | 3h | Pending | All above |
| **Total** | **~24h** | **21h done** | |
