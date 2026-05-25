import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { doctorApi, type Doctor, type PageResponse } from '../../lib/api';
import { useAuth } from '../../lib/AuthContext';
import Pagination from '../../components/Pagination';

export default function DoctorList() {
  const { userRole } = useAuth();
  const isAdmin = userRole === 'ADMIN';
  const [pageData, setPageData] = useState<PageResponse<Doctor> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDoctors();
  }, [page]);

  const loadDoctors = async () => {
    setLoading(true);
    try {
      const data = await doctorApi.getAll(page);
      setPageData(data);
    } catch (error) {
      console.error('Failed to load doctors:', error);
      alert('Failed to load doctors');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this doctor?')) {
      try {
        await doctorApi.delete(id);
        loadDoctors();
      } catch (error) {
        console.error('Failed to delete doctor:', error);
        alert('Failed to delete doctor');
      }
    }
  };

  if (loading) {
    return <div>Loading doctors...</div>;
  }

  const doctors = pageData?.content ?? [];

  return (
    <div className="max-w-4xl">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Doctors</h1>
        {isAdmin && (
          <Link to="/doctors/new" className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
            Add Doctor
          </Link>
        )}
      </div>

      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Specialty</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
              {isAdmin && <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {doctors.map((doctor) => (
              <tr key={doctor.id}>
                <td className="px-6 py-4 whitespace-nowrap">{doctor.firstName} {doctor.lastName}</td>
                <td className="px-6 py-4 whitespace-nowrap">{doctor.specialty}</td>
                <td className="px-6 py-4 whitespace-nowrap">{doctor.email}</td>
                {isAdmin && (
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <Link to={`/doctors/edit/${doctor.id}`} className="text-indigo-600 hover:text-indigo-900 mr-4">Edit</Link>
                    <button onClick={() => handleDelete(doctor.id!)} className="text-red-600 hover:text-red-900">
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
        {pageData && (
          <Pagination
            page={pageData.page}
            totalPages={pageData.totalPages}
            totalElements={pageData.totalElements}
            onPageChange={setPage}
          />
        )}
      </div>
    </div>
  );
}
