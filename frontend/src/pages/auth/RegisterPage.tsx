import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi, type RegisterRequest } from '../../lib/api';

type UserRole = 'PATIENT' | 'DOCTOR';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [role, setRole] = useState<UserRole>('PATIENT');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Common Fields
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '', // Common enough to keep here
  });

  // Patient Specific
  const [dob, setDob] = useState('');

  // Doctor Specific
  const [doctorData, setDoctorData] = useState({
    specialty: '',
    rppsNumber: '',
    address: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    // 1. Build the payload based on Role
    const payload: RegisterRequest = {
      ...formData,
      role,
      // If Patient, add DOB
      ...(role === 'PATIENT' && { dob }),
      // If Doctor, add Doctor Fields
      ...(role === 'DOCTOR' && doctorData),
    };

    try {
      await authApi.register(payload);
      // 2. Success! Redirect to Login
      alert('Registration successful! Please log in.');
      navigate('/login');
    } catch (err: any) {
      console.error(err);
      setError('Registration failed. Please check your inputs.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
          Create your account
        </h2>
        <p className="mt-2 text-center text-sm text-gray-600">
          Or{' '}
          <Link to="/login" className="font-medium text-blue-600 hover:text-blue-500">
            sign in to your existing account
          </Link>
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">

          {/* Role Switcher */}
          <div className="flex justify-center mb-6">
            <span className="relative z-0 inline-flex shadow-sm rounded-md">
              <button
                type="button"
                onClick={() => setRole('PATIENT')}
                className={`relative inline-flex items-center px-4 py-2 rounded-l-md border text-sm font-medium focus:z-10 focus:outline-none focus:ring-1 focus:ring-blue-500 ${
                  role === 'PATIENT'
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                }`}
              >
                I am a Patient
              </button>
              <button
                type="button"
                onClick={() => setRole('DOCTOR')}
                className={`-ml-px relative inline-flex items-center px-4 py-2 rounded-r-md border text-sm font-medium focus:z-10 focus:outline-none focus:ring-1 focus:ring-blue-500 ${
                  role === 'DOCTOR'
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                }`}
              >
                I am a Doctor
              </button>
            </span>
          </div>

          <form className="space-y-6" onSubmit={handleSubmit}>
            {error && (
              <div className="bg-red-50 border-l-4 border-red-400 p-4">
                <p className="text-sm text-red-700">{error}</p>
              </div>
            )}

            {/* --- Common Fields --- */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">First Name</label>
                <input
                  type="text"
                  required
                  value={formData.firstName}
                  onChange={(e) => setFormData({...formData, firstName: e.target.value})}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Last Name</label>
                <input
                  type="text"
                  required
                  value={formData.lastName}
                  onChange={(e) => setFormData({...formData, lastName: e.target.value})}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">Email address</label>
              <input
                type="email"
                required
                value={formData.email}
                onChange={(e) => setFormData({...formData, email: e.target.value})}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">Password</label>
              <input
                type="password"
                required
                minLength={6}
                value={formData.password}
                onChange={(e) => setFormData({...formData, password: e.target.value})}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">Phone</label>
              <input
                type="tel"
                value={formData.phone}
                onChange={(e) => setFormData({...formData, phone: e.target.value})}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              />
            </div>

            {/* --- Patient Specific --- */}
            {role === 'PATIENT' && (
              <div>
                <label className="block text-sm font-medium text-gray-700">Date of Birth</label>
                <input
                  type="date"
                  required
                  value={dob}
                  onChange={(e) => setDob(e.target.value)}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                />
              </div>
            )}

            {/* --- Doctor Specific --- */}
            {role === 'DOCTOR' && (
              <div className="space-y-6 border-t border-gray-200 pt-6">
                <h3 className="text-sm font-medium text-gray-900">Professional Details</h3>

                <div>
                  <label className="block text-sm font-medium text-gray-700">Specialty</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Cardiology"
                    value={doctorData.specialty}
                    onChange={(e) => setDoctorData({...doctorData, specialty: e.target.value})}
                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700">RPPS Number (Professional ID)</label>
                  <input
                    type="text"
                    required
                    value={doctorData.rppsNumber}
                    onChange={(e) => setDoctorData({...doctorData, rppsNumber: e.target.value})}
                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700">Clinic Address</label>
                  <textarea
                    rows={3}
                    value={doctorData.address}
                    onChange={(e) => setDoctorData({...doctorData, address: e.target.value})}
                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  />
                </div>
              </div>
            )}

            <div>
              <button
                type="submit"
                disabled={loading}
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-blue-400"
              >
                {loading ? 'Creating Account...' : 'Register'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}