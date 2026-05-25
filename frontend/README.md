# Frontend

Admin, doctor, and patient dashboard interface for EpiSante.
Built with React + TypeScript + Vite + Tailwind CSS + Axios.

---

## Responsibilities

### 1. Provide UI for
- Doctors / Patients / Appointments CRUD
- Appointment details view
- Login / JWT session management
- Real-time health alert dashboard

### 2. Handle JWT authentication
- Store token in `localStorage`
- Inject Authorization header into Axios
- Protect routes using `<Navigate />`

### 3. Call backend APIs
- `/api/auth/login`, `/api/doctors`, `/api/patients`, `/api/appointments`, `/api/alerts`

---

## Structure

- **Components**: `Header` (navigation)
- **Pages**: `LoginPage`, `HomePage`, `DoctorList`, `DoctorForm`, `PatientList`, `PatientForm`, `AppointmentList`, `AppointmentForm`, `AppointmentDetails`, `AlertsPage`
- **API Client**: `src/lib/api.ts` — Axios instance with Authorization header

---

## Next Steps

- ProtectedRoute component for cleaner route guarding
- Role-based UI (Admin: all, Doctor: their appointments, Patient: their profile)
- Responsive Tailwind layouts and dashboard graphs
- Global state management (Zustand or Redux Toolkit)
