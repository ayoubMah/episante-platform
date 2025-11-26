import { BrowserRouter as Router, Routes, Route, NavLink, Link } from 'react-router-dom';

// Import all page components
import HomePage from './pages/HomePage';
import DoctorList from './pages/doctors/DoctorList';
import DoctorForm from './pages/doctors/DoctorForm';
import PatientList from './pages/patients/PatientList';
// Make sure you create this PatientForm.tsx file next
import PatientForm from './pages/patients/PatientForm';
import AppointmentDetails from './pages/appointments/AppointmentDetails';
import AppointmentList from './pages/appointments/AppointmentList';
import AppointmentForm from './pages/appointments/AppointmentDetails';


function App() {
  return (
    <Router>
      <div className="min-h-screen bg-gray-100">
        <nav className="bg-white shadow-sm sticky top-0 z-50">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16">
              <div className="flex">
                {/* Logo / Brand Name */}
                <div className="flex-shrink-0 flex items-center">
                  <Link to="/" className="text-2xl font-bold text-blue-600">
                    EpiSanté
                  </Link>
                </div>
                {/* Main Navigation Links */}
                <div className="hidden sm:ml-8 sm:flex sm:space-x-8">
                  <NavLink
                    to="/doctors"
                    className={({ isActive }) =>
                      `inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium ${
                        isActive
                          ? 'border-blue-500 text-gray-900'
                          : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
                      }`
                    }
                  >
                    Doctors
                  </NavLink>
                  <NavLink
                    to="/patients"
                    className={({ isActive }) =>
                      `inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium ${
                        isActive
                          ? 'border-blue-500 text-gray-900'
                          : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
                      }`
                    }
                  >
                    Patients
                  </NavLink>
                </div>
              </div>
            </div>
          </div>
        </nav>

        {/* Main Content Area */}
        <main className="py-10">
          <div className="max-w-7xl mx-auto sm:px-6 lg:px-8">
            <Routes>
              {/* Homepage Route */}
              <Route path="/" element={<HomePage />} />

              {/* Doctor Routes */}
              <Route path="/doctors" element={<DoctorList />} />
              <Route path="/doctors/new" element={<DoctorForm />} />
              <Route path="/doctors/edit/:id" element={<DoctorForm />} />

              {/* Patient Routes */}
              <Route path="/patients" element={<PatientList />} />
              <Route path="/patients/new" element={<PatientForm />} />
              <Route path="/patients/edit/:id" element={<PatientForm />} />
              {/* Appointment Routes */}
              <Route path="/appointments" element={<AppointmentList />} />
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
