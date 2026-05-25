# Patient Service

Manages medical patient profiles. Trusts identity tokens from auth-service.

---

## Responsibilities

### 1. Store patient profile data
- Fields: id (UUID from auth-service), firstName, lastName, email, phone, dob, gender, doctorId, timestamps

### 2. Provide patient read/write APIs
- CRUD operations
- Batch endpoint for internal lookups

### 3. Validate patient existence for appointment-service

---

## Structure

- **Entities**: `Patient`
- **Repositories**: `PatientRepository`
- **Services**: `PatientService`
- **Controllers**: `/api/patients` (protected), `/internal/patients/create` and `/internal/patients/batch` (no auth)
- **Security**: JWT validation filter, `/internal/**` bypasses auth

---

## Next Steps

- RBAC: Patient views only themselves, Doctor views only their patients, Admin views all
- Search API: `/api/patients/search?query=...`
- Soft delete instead of hard delete
