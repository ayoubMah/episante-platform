import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/AuthContext';

export default function Header() {
  const { isAuthenticated, userRole, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="bg-white shadow-sm sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">

        {/* Logo / Home Link */}
        <Link to="/" className="text-2xl font-bold text-blue-600 hover:text-blue-700 transition-colors">
          EpiSanté
        </Link>

        <nav className="flex items-center space-x-4">

          {/* --- AUTHENTICATED USERS --- */}
          {isAuthenticated && (
            <>
              {/* Doctors Link (Visible to Patients & Admin) */}
              {(userRole === 'PATIENT' || userRole === 'ADMIN') && (
                <Link to="/doctors" className="text-gray-600 hover:text-blue-600 font-medium">
                  Doctors
                </Link>
              )}

              {/* Patients Link (Visible to Doctors & Admin) */}
              {(userRole === 'DOCTOR' || userRole === 'ADMIN') && (
                <Link to="/patients" className="text-gray-600 hover:text-blue-600 font-medium">
                  Patients
                </Link>
              )}

              {/* Appointments Link (Everyone) */}
              <Link to="/appointments" className="text-gray-600 hover:text-blue-600 font-medium">
                Appointments
              </Link>

              {/* Logout Button */}
              <button
                onClick={() => { logout(); navigate('/login'); }}
                className="ml-4 px-3 py-1 text-sm font-medium text-red-600 border border-red-200 rounded hover:bg-red-50 transition-colors"
              >
                Logout ({userRole})
              </button>
            </>
          )}

          {/* --- GUESTS (NOT LOGGED IN) --- */}
          {!isAuthenticated && (
            <div className="flex items-center space-x-2">
              {/* Register Link */}
              <Link
                to="/register"
                className="px-4 py-2 text-sm font-medium text-blue-600 hover:bg-blue-50 rounded transition-colors"
              >
                Register
              </Link>

              {/* Login Button */}
              <Link
                to="/login"
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded hover:bg-blue-700 shadow-sm transition-colors"
              >
                Login
              </Link>
            </div>
          )}
        </nav>
      </div>
    </header>
  );
}