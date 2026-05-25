# Auth Service

Handles authentication, authorization, and JWT issuance.
Stores only identity and access control information — not patient or doctor profile data.

---

## Responsibilities

### 1. Store identities
- `User` entity: id (UUID), email, password (BCrypt), role (ADMIN/DOCTOR/PATIENT), active flag

### 2. Register users and propagate profile creation
- Generates UUID and saves to `app_users`
- Calls `patient-service` or `doctor-service` internal endpoints to create the profile
- Shared UUID strategy across services

### 3. Issue JWT tokens
- Claims: `sub` (email), `id` (UUID), `role`, expiration (24h)
- JWT secret loaded from HashiCorp Vault

### 4. Login flow
- Verify password via `AuthenticationManager`, generate and return JWT

---

## Structure

- **Entities**: `User`, `Role` (enum)
- **DTOs**: `RegisterRequest`, `LoginRequest`, `AuthResponse`, `DoctorProfileRequest`, `PatientProfileRequest`
- **Services**: `JwtService`, `AuthService`, `CustomUserDetailsService`
- **Controllers**: `POST /api/auth/register`, `POST /api/auth/login`
- **Feign Clients**: `PatientClient`, `DoctorClient`
- **Security**: Stateless config, BCrypt encoder, shared JWT signing key

---

## Next Steps

- RBAC: Admin manages all, Doctor owns their appointments, Patient sees their own
- Refresh tokens for longer sessions
- Email verification (optional)
