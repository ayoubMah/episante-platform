import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import type { JSX } from "react/jsx-dev-runtime";

// Pages
import HomePage from "./pages/HomePage";
import DoctorList from "./pages/doctors/DoctorList";
import PatientList from "./pages/patients/PatientList";
import AppointmentDetails from "./pages/appointments/AppointmentDetails";
import AppointmentList from "./pages/appointments/AppointmentList";
import AppointmentForm from "./pages/appointments/AppointmentForm";
import LoginPage from "./pages/auth/LoginPage";
import RegisterPage from "./pages/auth/RegisterPage";

// Components & Lib
import Header from "./components/Header";
import { AuthProvider, useAuth } from "./lib/AuthContext";

// ✅ 1. Moved RoleRoute here (cleaner)
// This ensures that if you are not authorized, you get a clear UI feedback
function RoleRoute({ children, allowedRoles }: { children: JSX.Element, allowedRoles: string[] }) {
  const { userRole, isAuthenticated } = useAuth();

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  // If role is loaded but not allowed
  if (userRole && !allowedRoles.includes(userRole)) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-center">
          <h1 className="text-4xl font-bold text-red-600 mb-4">403</h1>
          <p className="text-xl text-gray-600">Access Denied</p>
          <p className="text-gray-500 mt-2">You do not have permission to view this page.</p>
        </div>
      </div>
    );
  }

  return children;
}

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="min-h-screen bg-gray-100">
          <Header />
          <main className="py-10">
            <div className="max-w-7xl mx-auto sm:px-6 lg:px-8">
              <Routes>
                {/* --- Public Routes --- */}
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/" element={<HomePage />} />

                {/* --- DOCTORS (Viewable by Admin & Patient) --- */}
                <Route path="/doctors" element={
                  <RoleRoute allowedRoles={['ADMIN', 'PATIENT']}>
                    <DoctorList />
                  </RoleRoute>
                } />

                {/* --- PATIENTS (Viewable by Admin & Doctor) --- */}
                <Route path="/patients" element={
                  <RoleRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                    <PatientList />
                  </RoleRoute>
                } />

                {/* --- APPOINTMENTS (List: Everyone) --- */}
                <Route path="/appointments" element={
                  <RoleRoute allowedRoles={['ADMIN', 'DOCTOR', 'PATIENT']}>
                    <AppointmentList />
                  </RoleRoute>
                } />

                {/* --- APPOINTMENTS (Create: ONLY PATIENT & ADMIN) --- */}
                {/* 🔒 Prevent Doctors from seeing the Create Form */}
                <Route path="/appointments/new" element={
                  <RoleRoute allowedRoles={['ADMIN', 'PATIENT']}>
                    <AppointmentForm />
                  </RoleRoute>
                } />

                {/* --- APPOINTMENTS (Edit: Everyone involved) --- */}
                {/* Note: Backend SecurityRules.canEdit handles the ownership check */}
                <Route path="/appointments/edit/:id" element={
                  <RoleRoute allowedRoles={['ADMIN', 'DOCTOR', 'PATIENT']}>
                    <AppointmentForm />
                  </RoleRoute>
                } />

                <Route path="/appointments/:id/details" element={
                   <RoleRoute allowedRoles={['ADMIN', 'DOCTOR', 'PATIENT']}>
                    <AppointmentDetails />
                  </RoleRoute>
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