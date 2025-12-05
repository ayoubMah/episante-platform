import { Link, NavLink } from "react-router-dom";

export default function Header() {
  const isLogged = Boolean(localStorage.getItem("accessToken"));

  return (
    <header className="bg-white shadow-sm sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">

        {/* Logo */}
        <Link to="/" className="text-2xl font-bold text-blue-600">
          EpiSanté
        </Link>

        {/* Navigation */}
        <nav className="hidden md:flex gap-6">
          <NavLink
            to="/doctors"
            className={({ isActive }) =>
              `text-sm font-medium ${
                isActive ? "text-blue-600" : "text-gray-600 hover:text-gray-900"
              }`
            }
          >
            Doctors
          </NavLink>

          <NavLink
            to="/patients"
            className={({ isActive }) =>
              `text-sm font-medium ${
                isActive ? "text-blue-600" : "text-gray-600 hover:text-gray-900"
              }`
            }
          >
            Patients
          </NavLink>

          <NavLink
            to="/appointments"
            className={({ isActive }) =>
              `text-sm font-medium ${
                isActive ? "text-blue-600" : "text-gray-600 hover:text-gray-900"
              }`
            }
          >
            Appointments
          </NavLink>
        </nav>

        {/* Right side */}
        <div>
          {!isLogged ? (
            <Link
              to="/login"
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              Login
            </Link>
          ) : (
            <button
              onClick={() => {
                localStorage.removeItem("accessToken");
                window.location.href = "/login";
              }}
              className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
            >
              Logout
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
