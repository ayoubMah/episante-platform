import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { appointmentApi, type Appointment, type PageResponse } from "../../lib/api";
import { useAuth } from "../../lib/AuthContext";
import Pagination from "../../components/Pagination";

export default function AppointmentList() {
  const { userRole } = useAuth();
  const canCreate = userRole === 'ADMIN' || userRole === 'PATIENT';
  const [pageData, setPageData] = useState<PageResponse<Appointment> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAppointments();
  }, [page]);

  const loadAppointments = async () => {
    setLoading(true);
    try {
      const data = await appointmentApi.getAll(page);
      setPageData(data);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading appointments...</div>;

  const appointments = pageData?.content ?? [];

  return (
    <div className="max-w-5xl">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Appointments</h1>
        {canCreate && (
          <Link
            to="/appointments/new"
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Create Appointment
          </Link>
        )}
      </div>

      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3">Doctor</th>
              <th className="px-6 py-3">Patient</th>
              <th className="px-6 py-3">Time</th>
              <th className="px-6 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 bg-white">
            {appointments.map(appt => (
              <tr key={appt.id}>
                <td className="px-6 py-4">{appt.doctorId}</td>
                <td className="px-6 py-4">{appt.patientId}</td>
                <td className="px-6 py-4">
                  {new Date(appt.startTime).toLocaleString()}
                </td>
                <td className="px-6 py-4 text-right">
                  <Link
                    to={`/appointments/${appt.id}/details`}
                    className="text-blue-600 hover:text-blue-900 mr-4"
                  >
                    Details
                  </Link>
                </td>
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
