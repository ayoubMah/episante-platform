import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { patientApi, type Patient } from '../../lib/api';

export default function PatientList() {
  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPatients();
  }, []);

  const loadPatients = async () => {
    setLoading(true);
    try {
      const data = await patientApi.getAll();
      setPatients(data);
    } catch (error) {
      console.error('Failed to load patients:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure?')) {
      try {
        await patientApi.delete(id);
        loadPatients();
      } catch (error) {
        alert('Failed to delete');
      }
    }
  };

  if (loading) return <div className="p-8">Loading...</div>;

  return (
    <div className="max-w-6xl mx-auto">
       <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Patients</h1>
        <Link to="/patients/new" className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          Add Patient
        </Link>
      </div>

      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Gender</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">DOB</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {patients.map((p) => (
              <tr key={p.id}>
                <td className="px-6 py-4 text-sm font-medium text-gray-900">{p.firstName} {p.lastName}</td>
                <td className="px-6 py-4 text-sm text-gray-500">{p.email}</td>
                <td className="px-6 py-4 text-sm text-gray-500">{p.gender}</td>
                <td className="px-6 py-4 text-sm text-gray-500">{p.dob}</td>
                <td className="px-6 py-4 text-right text-sm font-medium space-x-4">
                  <Link to={`/patients/edit/${p.id}`} className="text-indigo-600 hover:text-indigo-900">Edit</Link>
                  <button onClick={() => handleDelete(p.id!)} className="text-red-600 hover:text-red-900">Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}