# EpiSante — API Reference

All endpoints are accessed through the API Gateway at `https://localhost:8443`.
Authentication uses Bearer JWT tokens. Include `Authorization: Bearer <token>` in headers.

---

## Auth Service (`/api/auth`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Login, returns JWT token |

### POST /api/auth/register

```json
{
  "email": "alice@example.com",
  "password": "secret123",
  "role": "PATIENT",
  "firstName": "Alice",
  "lastName": "Dupont",
  "dob": "1990-05-15",
  "phone": "+33612345678"
}
```

For DOCTOR role, include `specialty`, `rppsNumber`, `address` instead of `dob`/`phone`.

Response: `201 Created`

### POST /api/auth/login

```json
{ "email": "alice@example.com", "password": "secret123" }
```

Response:
```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

---

## Doctor Service (`/api/doctors`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| GET | `/api/doctors?page=0&size=10` | Public | — | List doctors (paginated) |
| GET | `/api/doctors/{id}` | Public | — | Get doctor by ID |
| POST | `/api/doctors` | JWT | ADMIN | Create doctor profile |
| PUT | `/api/doctors/{id}` | JWT | ADMIN, Owner DOCTOR | Update doctor |
| DELETE | `/api/doctors/{id}` | JWT | ADMIN | Delete doctor |

### Response format (paginated)

```json
{
  "content": [
    { "id": "uuid", "firstName": "Dr.", "lastName": "House", "specialty": "Cardiology", "email": "house@hospital.com" }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "last": false
}
```

---

## Patient Service (`/api/patients`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| GET | `/api/patients?page=0&size=10` | JWT | ADMIN, DOCTOR | List patients (paginated) |
| GET | `/api/patients/{id}` | JWT | ADMIN, DOCTOR, Owner PATIENT | Get patient by ID |
| PUT | `/api/patients/{id}` | JWT | ADMIN, Owner PATIENT | Update patient |
| DELETE | `/api/patients/{id}` | JWT | ADMIN | Delete patient |

### Patient response

```json
{
  "id": "uuid",
  "firstName": "Alice",
  "lastName": "Dupont",
  "email": "alice@example.com",
  "phone": "+33612345678",
  "dob": "1990-05-15",
  "gender": "FEMALE",
  "doctorId": "uuid"
}
```

---

## Appointment Service (`/api/appointments`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| GET | `/api/appointments?page=0&size=10` | JWT | ALL | List appointments (filtered by role, paginated) |
| GET | `/api/appointments/{id}` | JWT | Owner/ADMIN | Get appointment |
| GET | `/api/appointments/{id}/details` | JWT | Owner/ADMIN | Get enriched details (doctor + patient names) |
| POST | `/api/appointments` | JWT | PATIENT, ADMIN | Create appointment |
| PUT | `/api/appointments/{id}` | JWT | Owner/ADMIN | Update appointment |
| DELETE | `/api/appointments/{id}` | JWT | Owner/ADMIN | Delete appointment |

Role-based filtering on GET all:
- **ADMIN**: sees all appointments
- **DOCTOR**: sees only their appointments
- **PATIENT**: sees only their appointments

### Create/Update request

```json
{
  "doctorId": "uuid",
  "patientId": "uuid",
  "startTime": "2026-06-01T09:00:00+02:00",
  "endTime": "2026-06-01T09:30:00+02:00"
}
```

### Details response

```json
{
  "id": "uuid",
  "doctorId": "uuid",
  "patientId": "uuid",
  "doctorFullName": "Dr. House",
  "doctorSpecialty": "Cardiology",
  "patientFullName": "Alice Dupont",
  "startTime": "2026-06-01T09:00:00+02:00",
  "endTime": "2026-06-01T09:30:00+02:00",
  "status": "PLANNED"
}
```

---

## Wearable Ingest Service (`/api/wearable`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/wearable/metrics` | Public | Ingest wearable telemetry |

### Request

```json
{
  "patientId": "550e8400-e29b-41d4-a716-446655440000",
  "heartRate": 118,
  "bloodPressureSystolic": 145,
  "bloodPressureDiastolic": 95,
  "spO2": 97,
  "temperature": 37.2,
  "timestamp": "2026-05-25T10:30:00Z",
  "deviceId": "watch-001"
}
```

Response: `201 Created`

Published to Kafka topic `wearable.telemetry.raw` (key: patientId).

---

## Health Alert Engine (`/api/alerts`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/alerts` | Public | Get all buffered alerts (last 1000) |
| GET | `/api/alerts/doctor/{doctorId}` | Public | Get alerts for a doctor |
| GET | `/api/alerts/patient/{patientId}` | Public | Get alerts for a patient |

### Alert response

```json
{
  "alertId": "uuid",
  "patientId": "uuid",
  "doctorId": "uuid",
  "alertType": "HIGH_HEART_RATE",
  "severity": "WARNING",
  "message": "Heart rate 138 bpm exceeds threshold 100 bpm",
  "actualValue": 138.0,
  "thresholdUsed": 100.0,
  "metricName": "heartRate",
  "timestamp": "2026-05-25T10:30:05Z"
}
```

Alert types: `HIGH_HEART_RATE`, `LOW_HEART_RATE`, `HIGH_BP_SYSTOLIC`, `HIGH_BP_DIASTOLIC`, `LOW_SPO2`, `HIGH_TEMPERATURE`, `LOW_TEMPERATURE`

Severity levels: `INFO`, `WARNING`, `CRITICAL`

---

## Internal Endpoints (not routed through gateway)

These are used for inter-service communication only:

| Service | Path | Purpose |
|---------|------|---------|
| Patient | `POST /internal/patients/profile` | Create patient profile (called by auth-service) |
| Patient | `POST /internal/patients/batch` | Batch patient lookup (called by health-alert-engine) |
| Doctor | `POST /internal/doctors/profile` | Create doctor profile (called by auth-service) |

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `appointment.created` | Appointment Service | Notification Service | Booking event |
| `wearable.telemetry.raw` | Ingest Wearables | Health Alert Engine | Raw vital signs |
| `health.alerts.patient` | Health Alert Engine | Notification Service | Patient alert |
| `health.alerts.doctor` | Health Alert Engine | Notification Service + Frontend | Doctor alert |

---

## Pagination

All list endpoints support pagination via query parameters:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | 0 | Page number (zero-based) |
| `size` | 10 | Items per page |

Response wrapper:

```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "last": false
}
```

---

## Error Responses

All errors follow this format:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Patient not found: uuid"
}
```

Common status codes:
- `400` — Bad request (validation error, overlap)
- `401` — Unauthorized (missing/invalid JWT)
- `403` — Forbidden (insufficient role)
- `404` — Resource not found
- `409` — Conflict (duplicate resource)
