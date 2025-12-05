import axios from 'axios';

const api = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// 🔥 Attach JWT automatically using an interceptor
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;


export interface Doctor {
  id?: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  specialty: string;
  rpps?: string;
  clinicAddress?: string;
}

export interface Patient {
  id?: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  // LocalDate in Java expects "YYYY-MM-DD" string
  dob?: string;
  // Java Enum requires exact uppercase match
  gender?: 'MALE' | 'FEMALE';
}
export interface Appointment {
  id?: string;
  doctorId: string;
  patientId: string;
  startTime: string;
  endTime: string;
  status: string;
}

export interface AppointmentDetails {
  id: string;
  doctorId: string;
  patientId: string;
  doctorFullName: string | null;
  doctorSpecialty: string | null;
  patientFullName: string;
  startTime: string;
  endTime: string;
  status: string;
}


export const doctorApi = {
  getAll: async () => (await api.get<Doctor[]>('/doctors')).data,
  getOne: async (id: string) => (await api.get<Doctor>(`/doctors/${id}`)).data,
  create: async (data: Doctor) => (await api.post<Doctor>('/doctors', data)).data,
  update: async (id: string, data: Doctor) => (await api.put<Doctor>(`/doctors/${id}`, data)).data,
  delete: async (id: string) => (await api.delete(`/doctors/${id}`)),
};

export const patientApi = {
  getAll: async () => (await api.get<Patient[]>('/patients')).data,
  getOne: async (id: string) => (await api.get<Patient>(`/patients/${id}`)).data,
  create: async (data: Patient) => (await api.post<Patient>('/patients', data)).data,
  update: async (id: string, data: Patient) => (await api.put<Patient>(`/patients/${id}`, data)).data,
  delete: async (id: string) => (await api.delete(`/patients/${id}`)),
};

export const appointmentApi = {
  getAll: async () => (await api.get<Appointment[]>('/appointments')).data,
  getOne: async (id: string) => (await api.get<Appointment>(`/appointments/${id}`)).data,
  getDetails: async (id: string) =>
    (await api.get<AppointmentDetails>(`/appointments/${id}/details`)).data,
  create: async (data: Appointment) =>
    (await api.post<Appointment>('/appointments', data)).data,
  update: async (id: string, data: Appointment) =>
    (await api.put<Appointment>(`/appointments/${id}`, data)).data,
  delete: async (id: string) => (await api.delete(`/appointments/${id}`)),
};
