import { Link } from 'react-router-dom';
import { FiSearch, FiUser, FiCalendar, FiPlusCircle, FiCheckCircle } from 'react-icons/fi';

export default function HomePage() {
  return (
    <div className="bg-white">
      {/* Hero Section */}
      <section className="bg-blue-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 text-center">
          <h1 className="text-4xl md:text-5xl font-extrabold text-gray-900">
            Your Health, Simplified.
          </h1>
          <p className="mt-4 max-w-2xl mx-auto text-lg text-gray-600">
            EpiSanté is your trusted partner for seamless healthcare management. Find doctors, book appointments, and manage your health journey with ease.
          </p>
          <div className="mt-8 flex justify-center gap-4 flex-wrap">
            <Link
              to="/patients"
              className="inline-flex items-center justify-center px-6 py-3 border border-transparent text-base font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700"
            >
              Find a Patient
            </Link>
            <Link
              to="/doctors"
              className="inline-flex items-center justify-center px-6 py-3 border border-transparent text-base font-medium rounded-md text-blue-700 bg-blue-100 hover:bg-blue-200"
            >
              Manage Doctors
            </Link>
            <Link
              to="/appointments"
              className="inline-flex items-center justify-center px-6 py-3 border border-transparent text-base font-medium rounded-md text-white bg-green-600 hover:bg-green-700"
            >
              Manage Appointments
            </Link>

          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h2 className="text-3xl font-extrabold text-gray-900">How It Works</h2>
            <p className="mt-4 text-lg text-gray-600">
              Accessing healthcare has never been easier.
            </p>
          </div>
          <div className="mt-12 grid gap-10 md:grid-cols-3">
            <div className="text-center">
              <div className="flex items-center justify-center h-12 w-12 rounded-md bg-blue-600 text-white mx-auto">
                <FiSearch size={24} />
              </div>
              <h3 className="mt-6 text-lg font-medium text-gray-900">1. Find Your Doctor</h3>
              <p className="mt-2 text-base text-gray-600">
                Search our extensive network of specialists by name, specialty, or location.
              </p>
            </div>
            <div className="text-center">
              <div className="flex items-center justify-center h-12 w-12 rounded-md bg-blue-600 text-white mx-auto">
                <FiCalendar size={24} />
              </div>
              <h3 className="mt-6 text-lg font-medium text-gray-900">2. Book an Appointment</h3>
              <p className="mt-2 text-base text-gray-600">
                View real-time availability and book your appointment online, 24/7.
              </p>
            </div>
            <div className="text-center">
              <div className="flex items-center justify-center h-12 w-12 rounded-md bg-blue-600 text-white mx-auto">
                <FiUser size={24} />
              </div>
              <h3 className="mt-6 text-lg font-medium text-gray-900">3. Manage Your Health</h3>
              <p className="mt-2 text-base text-gray-600">
                Keep track of your appointments and medical history in one secure place.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* For Patients CTA */}
      <section className="bg-gray-50 py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid md:grid-cols-2 gap-12 items-center">
          <div>
            <h2 className="text-3xl font-extrabold text-gray-900">For Our Patients</h2>
            <p className="mt-4 text-lg text-gray-600">
              Take control of your healthcare journey. With EpiSanté, you get access to top-rated doctors and can manage your appointments effortlessly.
            </p>
            <ul className="mt-6 space-y-4">
              <li className="flex items-start">
                <FiCheckCircle className="flex-shrink-0 h-6 w-6 text-green-500" />
                <span className="ml-3 text-base text-gray-600">Secure and private patient portals.</span>
              </li>
              <li className="flex items-start">
                <FiCheckCircle className="flex-shrink-0 h-6 w-6 text-green-500" />
                <span className="ml-3 text-base text-gray-600">Automated appointment reminders.</span>
              </li>
              <li className="flex items-start">
                <FiCheckCircle className="flex-shrink-0 h-6 w-6 text-green-500" />
                <span className="ml-3 text-base text-gray-600">Easy access to your medical history.</span>
              </li>
            </ul>
            <div className="mt-8">
              <Link
                to="/patients"
                className="inline-flex items-center justify-center px-6 py-3 border border-transparent text-base font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700"
              >
                Manage Patient Records
              </Link>
            </div>
          </div>
          <div className="hidden md:block">
            {/* Replace with a high-quality image. You can get free ones from Unsplash or Pexels. */}
            <img
              className="rounded-lg shadow-xl"
              src="https://images.unsplash.com/photo-1576091160550-2173dba999ef?q=80&w=2070"
              alt="Doctor with a patient on a tablet"
            />
          </div>
        </div>
      </section>

      {/* For Doctors CTA */}
      <section className="bg-white py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid md:grid-cols-2 gap-12 items-center">
           <div className="hidden md:block">
            <img
              className="rounded-lg shadow-xl"
              src="https://images.unsplash.com/photo-1550831107-1553da8c8464?q=80&w=1887"
              alt="Doctor reviewing files"
            />
          </div>
          <div>
            <h2 className="text-3xl font-extrabold text-gray-900">For Healthcare Professionals</h2>
            <p className="mt-4 text-lg text-gray-600">
              Streamline your practice and focus on what matters most: your patients. Our platform simplifies administrative tasks and improves patient engagement.
            </p>
            <ul className="mt-6 space-y-4">
              <li className="flex items-start">
                <FiPlusCircle className="flex-shrink-0 h-6 w-6 text-green-500" />
                <span className="ml-3 text-base text-gray-600">Effortless patient and appointment management.</span>
              </li>
              <li className="flex items-start">
                <FiPlusCircle className="flex-shrink-0 h-6 w-6 text-green-500" />
                <span className="ml-3 text-base text-gray-600">Reduce no-shows with automated reminders.</span>
              </li>
              <li className="flex items-start">
                <FiPlusCircle className="flex-shrink-0 h-6 w-6 text-green-500" />
                <span className="ml-3 text-base text-gray-600">Increase your visibility to new patients.</span>
              </li>
            </ul>
            <div className="mt-8">
              <Link
                to="/doctors/new"
                className="inline-flex items-center justify-center px-6 py-3 border border-transparent text-base font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700"
              >
                Add a New Doctor
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
