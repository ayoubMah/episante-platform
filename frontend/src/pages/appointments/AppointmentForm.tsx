import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { appointmentApi, doctorApi, patientApi } from "../../lib/api";
import type { Appointment, Doctor, Patient } from "../../lib/api";


export default function AppointmentForm() {
  const { id } = useParams();
  const navigate = useNavigate();

const [doctors, setDoctors] = useState<Doctor[]>([]);
const [patients, setPatients] = useState<Patient[]>([]);


  const [form, setForm] = useState<Appointment>({
    doctorId: "",
    patientId: "",
    startTime: "",
    endTime: "",
    status: "PLANNED",
  });

  useEffect(() => {
    loadDoctors();
    loadPatients();
    if (id) loadAppointment();
  }, [id]);

  const loadDoctors = async () => setDoctors(await doctorApi.getAll());
  const loadPatients = async () => setPatients(await patientApi.getAll());

  const loadAppointment = async () => {
    const data = await appointmentApi.getOne(id!);
    setForm(data);
  };

  const handleSubmit = async (e: any) => {
    e.preventDefault();

    if (id) await appointmentApi.update(id, form);
    else await appointmentApi.create(form);

    navigate("/appointments");
  };

  return (
    <div className="max-w-xl mx-auto bg-white p-6 shadow rounded">
      <h1 className="text-2xl font-bold mb-4">
        {id ? "Edit Appointment" : "Create Appointment"}
      </h1>

      <form onSubmit={handleSubmit} className="space-y-4">

        {/* Doctor */}
        <div>
          <label className="font-medium">Doctor</label>
          <select
            value={form.doctorId}
            onChange={(e) => setForm({ ...form, doctorId: e.target.value })}
            className="w-full border p-2 rounded"
            required
          >
            <option value="">Select Doctor</option>
            {doctors.map((d: Doctor) => (
              <option key={d.id} value={d.id}>
                {d.firstName} {d.lastName} ({d.specialty})
              </option>
            ))}
          </select>
        </div>

        {/* Patient */}
        <div>
          <label className="font-medium">Patient</label>
          <select
            value={form.patientId}
            onChange={(e) => setForm({ ...form, patientId: e.target.value })}
            className="w-full border p-2 rounded"
            required
          >
            <option value="">Select Patient</option>
            {patients.map((p: Patient) => (
              <option key={p.id} value={p.id}>
                {p.firstName} {p.lastName}
              </option>
            ))}
          </select>
        </div>

        {/* Start Time */}
        <div>
          <label className="font-medium">Start Time</label>
          <input
            type="datetime-local"
            value={form.startTime}
            onChange={(e) => setForm({ ...form, startTime: e.target.value })}
            className="w-full border p-2 rounded"
            required
          />
        </div>

        {/* End Time */}
        <div>
          <label className="font-medium">End Time</label>
          <input
            type="datetime-local"
            value={form.endTime}
            onChange={(e) => setForm({ ...form, endTime: e.target.value })}
            className="w-full border p-2 rounded"
            required
          />
        </div>

        <button className="px-4 py-2 bg-blue-600 text-white rounded">
          {id ? "Update" : "Create"}
        </button>
      </form>
    </div>
  );
}
