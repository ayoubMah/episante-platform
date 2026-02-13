import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../lib/AuthContext";

export default function Header() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <header className="bg-white shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center">
          <Link to="/" className="text-2xl font-bold text-blue-600">
            EpiSanté
          </Link>
        </div>

        <nav className="flex items-center space-x-4">
          <Link to="/doctors" className="text-gray-600 hover:text-gray-900">
            Doctors
          </Link>
          <Link to="/patients" className="text-gray-600 hover:text-gray-900">
            Patients
          </Link>

          {isAuthenticated ? (
            <>
              <Link
                to="/appointments"
                className="text-gray-600 hover:text-gray-900"
              >
                Appointments
              </Link>
              <button
                onClick={handleLogout}
                className="ml-4 px-4 py-2 text-sm font-medium text-white bg-red-600 rounded hover:bg-red-700"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="ml-4 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded hover:bg-blue-700"
              >
                Login
              </Link>
              <Link
                to="/register"
                className="ml-2 px-4 py-2 text-sm font-medium text-blue-700 bg-blue-100 rounded hover:bg-blue-200"
              >
                Register
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
