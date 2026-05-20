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

## Step 4 — Extend Patient Service (Add `doctorId`)

### 4.1 Database Migration (Flyway)

New migration: `V3__add_doctor_id.sql`

```sql
ALTER TABLE patients ADD COLUMN doctor_id UUID;

-- Optional: add foreign key reference
-- ALTER TABLE patients ADD CONSTRAINT fk_patient_doctor
--   FOREIGN KEY (doctor_id) REFERENCES doctors(id);
```

### 4.2 Update `Patient.java`

```java
@Column(name = "doctor_id")
private UUID doctorId;
```

### 4.3 Update `PatientResponseDTO.java`

Add `private UUID doctorId;`

### 4.4 Expose internal endpoint for batch patient profile lookup

The Health Alert Engine needs to enrich metrics with patient demographics. Add:

```java
// In PatientInternalController.java
@PostMapping("/internal/patients/batch")
public List<PatientResponseDTO> getPatientsByIds(@RequestBody List<UUID> ids) {
    return service.findAllByIds(ids);
}
```

Add `findAllByIds` to `PatientService`:

```java
public List<Patient> findAllByIds(List<UUID> ids) {
    return repo.findAllById(ids);
}
```

---

## Step 5 — Health Alert Engine (Java, Port 9097)

**Location**: `health-alert-engine/` (already scaffolded)

This is the core of the system. It uses **Kafka Streams** to process metrics in real time.

### 5.1 Update `pom.xml`

Add dependencies:
- `spring-kafka` (includes Kafka Streams)
- `spring-boot-starter-web` (for health endpoint)
- `episante-common`
- `lombok`
- `spring-boot-starter-actuator`

### 5.2 Application Properties

```properties
server.port=9097
spring.kafka.bootstrap-servers=kafka:9092
spring.kafka.streams.application-id=health-alert-engine

# Input topic
wearable.topic.raw=wearable.telemetry.raw

# Output topics
alert.topic.patient=health.alerts.patient
alert.topic.doctor=health.alerts.doctor

# Patient service URL (for enrichment)
patient.service.url=http://patient-service:9091
```

### 5.3 Demographic Threshold Engine

```java
package com.upec.healthe alertengine;

import com.upec.episantecommon.enums.Gender;
import java.time.LocalDate;
import java.time.Period;

public class ThresholdEngine {

    public record Thresholds(
        int hrMin, int hrMax,
        int bpSystolicMin, int bpSystolicMax,
        int bpDiastolicMin, int bpDiastolicMax,
        int spO2Min,
        double tempMin, double tempMax
    ) {}

    public Thresholds getThresholds(LocalDate dob, Gender gender) {
        int age = Period.between(dob, LocalDate.now()).getYears();

        int hrMin, hrMax;
        int bpSysMin, bpSysMax;
        int bpDiaMin, bpDiaMax;
        int spo2Min = 95;
        double tempMin = 36.0;
        double tempMax = 37.8;

        if (age < 2) {
            // Infant
            hrMin = 100; hrMax = 160;
            bpSysMin = 70; bpSysMax = 100;
            bpDiaMin = 40; bpDiaMax = 60;
        } else if (age < 12) {
            // Child
            hrMin = 70; hrMax = 130;
            bpSysMin = 80; bpSysMax = 110;
            bpDiaMin = 50; bpDiaMax = 70;
        } else if (age < 18) {
            // Adolescent
            hrMin = 60; hrMax = 110;
            bpSysMin = 90; bpSysMax = 120;
            bpDiaMin = 60; bpDiaMax = 80;
        } else if (age < 60) {
            // Adult
            hrMin = 60; hrMax = 100;
            bpSysMin = 90; bpSysMax = 130;
            bpDiaMin = 60; bpDiaMax = 85;
        } else {
            // Elderly (60+)
            hrMin = 60; hrMax = 90;
            bpSysMin = 100; bpSysMax = 140;
            bpDiaMin = 60; bpDiaMax = 90;

            // Elderly women tend to have slightly higher BP
            if (gender == Gender.FEMALE) {
                bpSysMax = 145;
            }
        }

        // Gender-specific HR adjustments
        if (gender == Gender.FEMALE && age >= 12) {
            hrMax += 5; // Women tend to have slightly higher resting HR
        }

        return new Thresholds(hrMin, hrMax, bpSysMin, bpSysMax, bpDiaMin, bpDiaMax, spo2Min, tempMin, tempMax);
    }
}
```

### 5.4 Kafka Streams Topology

```java
package com.upec.healthalertengine;

import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Function;

@Configuration
public class AlertStreamProcessor {

    @Bean
    public Function<KStream<String, WearableMetricEvent>, KStream<String, HealthAlertEvent>> process() {
        return input -> input
            // 1. Enrich with patient profile (via state store populated from patient-service)
            //    In practice, use a KTable backed by a compacted topic or GlobalKTable
            .mapValues(this::enrichAndEvaluate)
            .filter((key, alert) -> alert != null);
    }

    private HealthAlertEvent enrichAndEvaluate(WearableMetricEvent metric) {
        // Fetch patient profile (from local cache / state store)
        // PatientProfile profile = patientStore.get(metric.getPatientId());
        // if (profile == null) return null;

        // Thresholds thresholds = thresholdEngine.getThresholds(profile.getDob(), profile.getGender());

        // Evaluate each metric against thresholds
        // Return HealthAlertEvent if any threshold exceeded, null otherwise
    }
}
```

### 5.5 Patient Profile State Store

Two approaches:

**A) GlobalKTable + compacted topic** (recommended):
- Patient service publishes profile changes to a compacted Kafka topic `patient.profiles`
- Alert engine consumes as a GlobalKTable for fast lookups

**B) REST cache** (simpler for Phase 1):
- Maintain a local `ConcurrentHashMap<UUID, PatientProfile>` in the alert engine
- On startup, fetch all patients from `patient-service/internal/patients/batch`
- Periodically refresh (or subscribe to updates)

### 5.6 Output Topics

```java
@Bean
public NewTopic patientAlertTopic() {
    return TopicBuilder.name("health.alerts.patient").partitions(3).replicas(1).build();
}

@Bean
public NewTopic doctorAlertTopic() {
    return TopicBuilder.name("health.alerts.doctor").partitions(3).replicas(1).build();
}
```

### 5.7 Alert Rules Engine

| Metric | Threshold Logic | Severity |
|---|---|---|
| Heart Rate | `hr < hrMin` → BRADYCARDIA, `hr > hrMax` → TACHYCARDIA | `>hrMax+20` → CRITICAL, else WARNING |
| BP Systolic | `> bpSysMax` → HYPERTENSION | `> bpSysMax+20` → CRITICAL, else WARNING |
| BP Diastolic | `> bpDiaMax` → HYPERTENSION | `> bpDiaMax+15` → CRITICAL |
| SpO2 | `< spO2Min` → LOW_OXYGEN | `< 90` → CRITICAL, `< 95` → WARNING |
| Temperature | `> tempMax` → FEVER | `> 39.5` → CRITICAL, `> 38` → WARNING |
| Temperature | `< tempMin` → HYPOTHERMIA | `< 35` → CRITICAL |
| Any disconnect | No data for 5 min → DEVICE_DISCONNECTED | WARNING |

### 5.8 Deduplication / Alert Throttling

To avoid alert storms, implement a **cooldown window** per patient per alert type:

```java
// Use a state store (windowed) to track last alert time per (patientId, alertType)
// Only emit a new alert if 5 minutes have passed since the last one
```

Implement with Kafka Streams `windowedBy` + `suppress`:

```java
.groupBy((key, alert) -> alert.getPatientId() + ":" + alert.getAlertType())
.windowedBy(TimeWindows.of(Duration.ofMinutes(5)).grace(Duration.ofMinutes(1)))
.reduce((agg, current) -> current)
.suppress(Suppressed.untilWindowCloses(unbounded()))
.toStream()
...
```

---

## Step 6 — Extend Notification Service (Go)

**Location**: `notification-service/`

### 6.1 Add second Kafka consumer for `health.alerts.patient` and `health.alerts.doctor`

```go
// New struct
type HealthAlertEvent struct {
    AlertID      string  `json:"alertId"`
    PatientID    string  `json:"patientId"`
    DoctorID     string  `json:"doctorId"`
    AlertType    string  `json:"alertType"`
    Severity     string  `json:"severity"`
    Message      string  `json:"message"`
    ActualValue  float64 `json:"actualValue"`
    ThresholdUsed float64 `json:"thresholdUsed"`
    MetricName   string  `json:"metricName"`
    Timestamp    string  `json:"timestamp"`
}
```

### 6.2 Consumer logic

- Create a second reader for topic `health.alerts.patient`
- Create a third reader for topic `health.alerts.doctor` (or use same group)
- On receiving an alert:
  - For patient topic: `[MOCK] Sending SMS to patient {patientId}: {message}`
  - For doctor topic: `[MOCK] Sending email to doctor {doctorId}: Patient {patientId} alert - {message}`

Better approach: consume from a **single topic** `health.alerts` with a `targetAudience` field (PATIENT, DOCTOR, BOTH).

### 6.3 Update `main.go`

```go
func main() {
    // ... existing appointment.created consumer ...

    // New: health alerts consumer
    go consumeHealthAlerts(ctx, brokerAddress)

    // Wait for shutdown
    <-ctx.Done()
}

func consumeHealthAlerts(ctx context.Context, brokerAddress string) {
    reader := kafka.NewReader(kafka.ReaderConfig{
        Brokers: []string{brokerAddress},
        GroupID: "notification-group-go",
        Topic:   "health.alerts",
        // ...
    })

    for {
        msg, err := reader.ReadMessage(ctx)
        // ... parse HealthAlertEvent ...
        handleHealthAlert(event)
    }
}

func handleHealthAlert(event HealthAlertEvent) {
    // Route based on target audience
    log.Printf("🚨 [ALERT] %s | Patient=%s Doctor=%s | %s (value=%.1f, threshold=%.1f)",
        event.Severity, event.PatientID, event.DoctorID,
        event.Message, event.ActualValue, event.ThresholdUsed)

    // Mock send to patient
    log.Printf("📱 [MOCK SMS] Patient %s: %s", event.PatientID, event.Message)

    // Mock send to doctor
    log.Printf("📧 [MOCK EMAIL] Doctor %s: Alert for patient %s - %s",
        event.DoctorID, event.PatientID, event.Message)
}
```

---

## Step 7 — Infrastructure Updates

### 7.1 Kafka Topics (auto-created or manual)

| Topic | Partitions | Purpose |
|---|---|---|
| `wearable.telemetry.raw` | 3 | Raw metrics from ingest service |
| `patient.profiles` | 3 | Compacted topic for patient profile state |
| `health.alerts` | 3 | Alert events (patient + doctor audience) |
| `health.alerts.patient` | 3 | Alerts targeted to patients (alternative) |
| `health.alerts.doctor` | 3 | Alerts targeted to doctors (alternative) |

### 7.2 Docker Compose — Add new services

#### Ingest Wearables Service

```yaml
ingest-wearables-service:
  build:
    context: ../../
    dockerfile: ingest-wearables-service/Dockerfile
  container_name: ingest-wearables-service
  depends_on:
    kafka:
      condition: service_healthy
  ports:
    - "9096:9096"
  environment:
    SERVER_PORT: 9096
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
  networks:
    - episante_net
```

#### Health Alert Engine

```yaml
health-alert-engine:
  build:
    context: ../../
    dockerfile: health-alert-engine/Dockerfile
  container_name: health-alert-engine
  depends_on:
    kafka:
      condition: service_healthy
    patient-service:
      condition: service_started
  ports:
    - "9097:9097"
  environment:
    SERVER_PORT: 9097
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    PATIENT_SERVICE_URL: http://patient-service:9091
  networks:
    - episante_net
```

### 7.3 Nginx Gateway

Add to `gateway/nginx.conf`:

```nginx
location /api/wearable {
    proxy_pass http://ingest-wearables-service:9096;
}
```

### 7.4 Notification Service — Update Docker Compose

The notification service already has the correct Kafka broker env var. Just rebuild.

---

## Step 8 — Extend Frontend (Doctor Alert Dashboard)

### 8.1 WebSocket or Polling

For real-time alert display, add a WebSocket endpoint to the alert engine (or a REST endpoint):

```java
@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    @GetMapping("/doctor/{doctorId}")
    public List<HealthAlertEvent> getAlertsForDoctor(@PathVariable UUID doctorId) {
        // Query from an in-memory buffer / database
    }
}
```

### 8.2 Frontend Components

- `AlertBadge` — shows unread alert count in navbar for doctors
- `AlertPanel` — sidebar/list of recent alerts with severity color-coding
- Auto-refresh via `setInterval` or WebSocket connection

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
| `patient-service` | Add doctorId, batch endpoint | Pending |
| `ingest-wearables-service` | Full implementation | ✅ Done |
| `health-alert-engine` | Full implementation (Kafka Streams) | Pending |
| `notification-service` | Add health alert consumer | Pending |
| `frontend` | Doctor alert dashboard | Pending |
| `infra/compose` | Add new services to docker-compose | Pending |
| `gateway` | Add wearable route to nginx.conf | Pending |
| `scripts/wearable-simulator` | Wearable simulator (Python) | ✅ Done |

---

## Timeline Estimate

| Step | Effort | Status | Dependencies |
|---|---|---|---|---|
| Step 1: Shared events & enums | 1h | ✅ Done | None |
| Step 2: Wearable simulator | 3h | ✅ Done | None |
| Step 3: Ingest service | 2h | ✅ Done | Step 1 |
| Step 4: Patient service extension | 1h | Pending | None |
| Step 5: Health alert engine | 6h | Pending | Step 1, Step 4 |
| Step 6: Notification extension | 2h | Pending | Step 1 |
| Step 7: Docker & infra | 2h | Pending | Steps 3, 5, 6 |
| Step 8: Frontend alerts | 4h | Pending | Step 5 |
| Step 9: Testing | 3h | Pending | All above |
| **Total** | **~24h** | **6h done** | |
