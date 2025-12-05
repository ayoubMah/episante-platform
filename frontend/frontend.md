# Frontend – React + TypeScript + Axios

This is the admin + doctor + patient dashboard interface for EpiSanté.

---

## Responsibilities

### 1. Provide UI for:
- Doctors CRUD
- Patients CRUD
- Appointments CRUD
- Appointment details view
- Login / JWT session management

### 2. Handle JWT authentication
- Store token in `localStorage`
- Inject Authorization header into Axios
- Protect routes using `<Navigate />`

### 3. Call backend APIs
- `/api/auth/login`
- `/api/doctors`
- `/api/patients`
- `/api/appointments`

---

## Implemented Structure

### ✔ Global Components
- `Header` (navigation)
- Protected routing planned

### ✔ Pages
- `LoginPage`
- HomePage
- Doctors:
  - `DoctorList`
  - `DoctorForm`
- Patients:
  - `PatientList`
  - `PatientForm`
- Appointments:
  - `AppointmentList`
  - `AppointmentForm`
  - `AppointmentDetails`

### ✔ API Client
- `/lib/api.ts`
- Axios instance with Authorization header

---

## 🚀 Next Steps

### 1. Add ProtectedRoute component
Cleaner way to restrict routes.

### 2. Role-based UI
- ADMIN: access to everything
- DOCTOR: show only their appointments
- PATIENT: show only their profile + appointments

### 3. Make it beautiful
- Add Tailwind components
- Responsive layouts
- Dashboard graphs

### 4. Better State Management
- Zustand or Redux Toolkit for global auth state

---

