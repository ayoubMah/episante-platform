# Auth Service – Documentation

The `auth-service` is responsible for authentication, authorization, and JWT issuance.  
It does NOT store patient or doctor profile data.  
It stores only identity and access control information.

---

## ✅ Responsibilities

### 1. Store identities
- `User` entity with:
    - `id` (UUID, shared with other services)
    - `email` (unique)
    - `password` (BCrypt)
    - `role` (ADMIN, DOCTOR, PATIENT)
    - `active` (boolean)

### 2. Register users and propagate profile creation
- When a user registers:
    - Auth generates UUID.
    - Auth saves to `app_users`.
    - Auth calls:
        - `patient-service/internal/patients/create`
        - `doctor-service/internal/doctors/create`
    - Using **shared UUID strategy**.

### 3. Issue JWT tokens
- JWT includes:
    - `sub` → email
    - `id` → shared UUID
    - `role` → PATIENT / DOCTOR / ADMIN
    - expiration (24h)
- JWT secret loaded from `application.yaml`.

### 4. Login flow
- Verify password using `AuthenticationManager`
- Generate token with claims
- Return token to frontend

---

## 🧱 Structure Implemented

### ✔ Entities
- `User`
- `Role` (enum)

### ✔ Repositories
- `UserRepository`

### ✔ DTOs
- `RegisterRequest`
- `LoginRequest`
- `AuthResponse`
- `DoctorProfileRequest`
- `PatientProfileRequest`

### ✔ Services
- `JwtService`
- `AuthService`
- `CustomUserDetailsService`

### ✔ Controllers
- `AuthController`
    - `POST /api/auth/register`
    - `POST /api/auth/login`

### ✔ Feign Clients
- `PatientClient`
- `DoctorClient`

### ✔ Security
- Stateless security config
- BCrypt encoder
- Shared JWT signing key

---

## 🚀 Next Steps

### 👇 Phase 2 — Authorization rules (RBAC)
- Admin can manage all
- Doctor can only modify their own patients’ appointments
- Patient can see their own appointments

### 👇 Phase 3 — Refresh Tokens
- Add longer session support without re-login

### 👇 Phase 4 — Email verification (optional)
- Send verification email via SMTP or SendGrid

---

