# Appointment Service

Handles appointment booking, validation, and aggregation with other services.

---

## Responsibilities

### 1. Create appointment
- Validates patient exists, doctor exists, no overlapping time slots

### 2. Update appointment
- Overlap exclusion on update

### 3. Delete appointment

### 4. Appointment details aggregator
- Enriches with doctor name/specialty and patient name

---

## Structure

- **Entities**: `Appointment`, `AppointmentStatus`
- **Repositories**: `AppointmentRepository` (with overlap detection queries)
- **Services**: `AppointmentService`
- **Controllers**: `/api/appointments` — create, update, delete, get all, get by id, get enriched details (`/details`)
- **Feign Clients**: `DoctorClient`, `PatientClient`
- **Security**: JWT validation filter

---

## Next Steps

- RBAC: Doctor sees only their appointments, Patient sees only theirs, Admin sees all
- Pagination: `/api/appointments?page=0&size=10`
- Email/SMS reminders via notification service
