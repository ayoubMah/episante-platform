import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
} from "react-router-dom";

import HomePage from "./pages/HomePage";
import DoctorList from "./pages/doctors/DoctorList";
import DoctorForm from "./pages/doctors/DoctorForm";
import PatientList from "./pages/patients/PatientList";
import PatientForm from "./pages/patients/PatientForm";
import AppointmentDetails from "./pages/appointments/AppointmentDetails";
import AppointmentList from "./pages/appointments/AppointmentList";
import AppointmentForm from "./pages/appointments/AppointmentForm";
import LoginPage from "./pages/auth/LoginPage";

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
                {/* Public Routes */}
                <Route path="/login" element={<LoginPage />} />
                <Route path="/" element={<HomePage />} />
                <Route path="/doctors/*" element={<DoctorList />} />{" "}
                {/* Simplified for brevity */}
                {/* Protected Routes */}
                <Route element={<ProtectedRoute />}>
                  <Route path="/appointments" element={<AppointmentList />} />
                  <Route
                    path="/appointments/new"
                    element={<AppointmentForm />}
                  />
                  <Route
                    path="/appointments/edit/:id"
                    element={<AppointmentForm />}
                  />
                  <Route
                    path="/appointments/:id/details"
                    element={<AppointmentDetails />}
                  />
                  {/* Patients should probably be protected too? */}
                  <Route path="/patients" element={<PatientList />} />
                </Route>
              </Routes>
            </div>
          </main>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;
