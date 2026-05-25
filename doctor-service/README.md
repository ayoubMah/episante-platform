# Doctor Service

Manages doctors, their specialties, and clinic information.

---

## Responsibilities

### 1. Store doctor profile data
- Fields: id (UUID from auth-service), firstName, lastName, specialty, rpps, clinicAddress, timestamps

### 2. Provide doctor CRUD operations

### 3. Validate doctor existence for appointment-service

---

## Structure

- **Entities**: `Doctor`
- **Repositories**: `DoctorRepository`
- **Services**: `DoctorService`
- **Controllers**: `/api/doctors` (protected), `/internal/doctors/create` (used by auth-service)
- **Security**: JWT validation filter, `/internal/**` bypasses auth

---

## Next Steps

- Doctor availability calendar (slots, working hours, break times)
- RBAC: Doctor edits only their own profile, Admin controls everything
- Search API: `/api/doctors/search?specialty=Cardiology`
