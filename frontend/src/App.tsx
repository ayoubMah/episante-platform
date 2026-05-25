import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import HomePage from "./pages/HomePage";
import DoctorList from "./pages/doctors/DoctorList";
import DoctorForm from "./pages/doctors/DoctorForm";
import PatientList from "./pages/patients/PatientList";
import PatientForm from "./pages/patients/PatientForm";
import AlertsPage from "./pages/alerts/AlertsPage";
import AppointmentDetails from "./pages/appointments/AppointmentDetails";
import AppointmentList from "./pages/appointments/AppointmentList";
import AppointmentForm from "./pages/appointments/AppointmentForm";
import LoginPage from "./pages/auth/LoginPage";
import RegisterPage from "./pages/auth/RegisterPage";

import Header from "./components/Header";
import ProtectedRoute from "./components/ProtectedRoute";
import { AuthProvider } from "./lib/AuthContext";

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="min-h-screen bg-gray-100">
          <Header />
          <main className="py-10">
            <div className="max-w-7xl mx-auto sm:px-6 lg:px-8">
              <Routes>
                {/* Public */}
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/" element={<HomePage />} />

                {/* Doctors — Admin & Patient can view, Admin can create/edit */}
                <Route path="/doctors" element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'PATIENT']}>
                    <DoctorList />
                  </ProtectedRoute>
                } />
                <Route path="/doctors/new" element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <DoctorForm />
                  </ProtectedRoute>
                } />
                <Route path="/doctors/edit/:id" element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <DoctorForm />
                  </ProtectedRoute>
                } />

                {/* Patients — Admin & Doctor can view, Admin can edit */}
                <Route path="/patients" element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                    <PatientList />
                  </ProtectedRoute>
                } />
                <Route path="/patients/new" element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <PatientForm />
                  </ProtectedRoute>
                } />
                <Route path="/patients/edit/:id" element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <PatientForm />
                  </ProtectedRoute>
                } />

                {/* Appointments — all roles */}
                <Route path="/appointments" element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR', 'PATIENT']}>
                    <AppointmentList />
                  </ProtectedRoute>
                } />

                {/* Create appointment — Patient & Admin only */}
                <Route path="/appointments/new" element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'PATIENT']}>
                    <AppointmentForm />
                  </ProtectedRoute>
                } />

                {/* Edit appointment — all roles (backend checks ownership) */}
                <Route path="/appointments/edit/:id" element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR', 'PATIENT']}>
                    <AppointmentForm />
                  </ProtectedRoute>
                } />

                <Route path="/appointments/:id/details" element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR', 'PATIENT']}>
                    <AppointmentDetails />
                  </ProtectedRoute>
                } />

                {/* Alerts — Doctor & Admin */}
                <Route path="/alerts" element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                    <AlertsPage />
                  </ProtectedRoute>
                } />
              </Routes>
            </div>
          </main>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;
