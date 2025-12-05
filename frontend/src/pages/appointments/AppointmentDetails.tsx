import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { appointmentApi, type AppointmentDetails } from "../../lib/api";

export default function AppointmentDetails() {
  const { id } = useParams();
  const [details, setDetails] = useState<AppointmentDetails | null>(null);

  useEffect(() => {
    loadDetails();
  }, [id]);

  const loadDetails = async () => {
    const data = await appointmentApi.getDetails(id!);
    setDetails(data);
  };

  if (!details) return <div>Loading...</div>;

  return (
    <div className="max-w-3xl">
      <h1 className="text-3xl font-bold text-gray-900 mb-4">
        Appointment Details
      </h1>

      <div className="bg-white shadow p-6 rounded-lg space-y-4">
        <p>
          <strong>Doctor:</strong> {details.doctorFullName} (
          {details.doctorSpecialty})
        </p>

        <p>
          <strong>Patient:</strong> {details.patientFullName}
        </p>

        <p>
          <strong>Time:</strong>{" "}
          {new Date(details.startTime).toLocaleString()} -{" "}
          {new Date(details.endTime).toLocaleString()}
        </p>

        <p>
          <strong>Status:</strong> {details.status}
        </p>

        <div className="pt-4 flex gap-4">
          <Link
            to={`/appointments/edit/${details.id}`}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Edit Appointment
          </Link>
          <Link
            to="/appointments"
            className="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400"
          >
            Back
          </Link>
        </div>
      </div>
    </div>
  );
}
