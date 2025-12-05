# Doctor Service – Documentation

The `doctor-service` manages doctors, their specialties, and clinic information.

---

## Responsibilities

### 1. Store doctor profile data
- `id` (UUID from auth-service)
- `firstName`
- `lastName`
- `specialty`
- `rpps`
- `clinicAddress`
- timestamps

### 2. Provide doctor CRUD operations

### 3. Validate doctor for appointment-service

---

## Implemented Structure

### ✔ Entities
- `Doctor`

### ✔ Repositories
- `DoctorRepository`

### ✔ Services
- `DoctorService`

### ✔ Controllers
- `/api/doctors` (protected)
- `/internal/doctors/create` (used by auth-service)

### ✔ Security
- JWT validation filter
- `/internal/**` bypass security

---

## 🚀 Next Steps

### 1. Doctor Availability Calendar
- Available slots
- Working hours
- Break times

### 2. RBAC
- DOCTOR role can edit only their own profile
- ADMIN controls everything

### 3. Doctor Search
- `/api/doctors/search?specialty=Cardiology`

---

