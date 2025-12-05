# Appointment Service – Documentation

The `appointment-service` handles appointment booking, validation, and aggregation with other services.

---

## Responsibilities

### 1. Create appointment
- Validate:
    - Patient exists
    - Doctor exists
    - No overlapping appointments

### 2. Update appointment
- Rewrite logic with overlap exclusion

### 3. Delete appointment

### 4. Provide appointment details aggregator
- Doctor name + specialty
- Patient name
- Times + status

---

## Implemented Structure

### ✔ Entities
- `Appointment`
- `AppointmentStatus`

### ✔ Repositories
- `AppointmentRepository`
    - overlap detection (create)
    - overlap detection (update)

### ✔ Services
- `AppointmentService`

### ✔ Controllers
- `/api/appointments`
    - create
    - update
    - delete
    - get all
    - get by id
    - get enriched details (`/details`)

### ✔ Feign Clients
- `DoctorClient`
- `PatientClient`

### ✔ Security
- JWT validation filter
- RBAC coming next

---

## 🚀 Next Steps

### 1. RBAC
- DOCTOR can access only appointments assigned to them
- PATIENT can access only their appointments
- ADMIN can access all

### 2. Pagination
- `/api/appointments?page=0&size=10`

### 3. Notifications (future)
- Email reminder
- SMS reminder

---

