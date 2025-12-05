# Patient Service – Documentation

The `patient-service` manages medical patient profiles.  
It trusts identity tokens from auth-service.

---

## Responsibilities

### 1. Store patient profile data
- Personal info:
    - `id` (UUID from auth-service)
    - `firstName`
    - `lastName`
    - `email`
    - `phone`
    - `dob`
    - `gender`
    - timestamps

### 2. Provide patient read/write APIs
- CRUD operations
- Search options (future)

### 3. Validate patient existence for appointment-service

---

## Implemented Structure

### ✔ Entities
- `Patient`

### ✔ Repositories
- `PatientRepository`

### ✔ Services
- `PatientService`

### ✔ Controllers
- Public API: `/api/patients`
- Internal API (no auth): `/internal/patients/create`

### ✔ Security
- JWT validation filter
- Allow `/internal/**` without auth (used by auth-service)
- Protect all other routes

---

## 🚀 Next Steps

### 1. RBAC Permission Model
- PATIENT: Can view only themselves
- DOCTOR: Can view only patients of their appointments
- ADMIN: Can view all

### 2. Search API
- `/api/patients/search?query=...`

### 3. Soft Delete
- Instead of real delete, mark patient inactive

---

