import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

import HomePage from './pages/HomePage';
import DoctorList from './pages/doctors/DoctorList';
import DoctorForm from './pages/doctors/DoctorForm';
import PatientList from './pages/patients/PatientList';
import PatientForm from './pages/patients/PatientForm';
import AppointmentDetails from './pages/appointments/AppointmentDetails';
import AppointmentList from './pages/appointments/AppointmentList';
import AppointmentForm from './pages/appointments/AppointmentForm';
import LoginPage from './pages/auth/LoginPage';

import Header from "./components/Header";

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-gray-100">

        {/* GLOBAL NAVBAR */}
        <Header />

        {/* Main Content Area */}
        <main className="py-10">
          <div className="max-w-7xl mx-auto sm:px-6 lg:px-8">
            <Routes>

              {/* Authentication Route */}
              <Route path="/login" element={<LoginPage />} />

              {/* Homepage */}
              <Route path="/" element={<HomePage />} />

              {/* Doctors */}
              <Route path="/doctors" element={<DoctorList />} />
              <Route path="/doctors/new" element={<DoctorForm />} />
              <Route path="/doctors/edit/:id" element={<DoctorForm />} />

              {/* Patients */}
              <Route path="/patients" element={<PatientList />} />
              <Route path="/patients/new" element={<PatientForm />} />
              <Route path="/patients/edit/:id" element={<PatientForm />} />

              {/* Appointments (protected) */}
              <Route
                path="/appointments"
                element={
                  localStorage.getItem("accessToken") ? (
                    <AppointmentList />
                  ) : (
                    <Navigate to="/login" />
                  )
                }
              />
              <Route path="/appointments/new" element={<AppointmentForm />} />
              <Route path="/appointments/edit/:id" element={<AppointmentForm />} />
              <Route path="/appointments/:id/details" element={<AppointmentDetails />} />

            </Routes>
          </div>
        </main>

      </div>
    </Router>
  );
}

export default App;
