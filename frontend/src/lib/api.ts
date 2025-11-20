import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

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