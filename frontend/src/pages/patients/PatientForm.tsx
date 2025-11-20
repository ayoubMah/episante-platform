import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { patientApi, type Patient } from '../../lib/api';

export default function PatientForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [formData, setFormData] = useState<Patient>({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    dob: '',
    gender: 'MALE' // Default value
  });

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEdit && id) {
      patientApi.getOne(id)
        .then(setFormData)
        .catch(() => alert('Error loading patient'));
    }
  }, [id, isEdit]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      isEdit
        ? await patientApi.update(id!, formData)
        : await patientApi.create(formData);
      navigate('/patients');
    } catch (err) {
      console.error(err);
      alert('Failed to save patient. Check console.');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  return (
    <div className="max-w-2xl mx-auto mt-10">
      <h1 className="text-2xl font-bold mb-6 text-gray-800">{isEdit ? 'Edit Patient' : 'New Patient'}</h1>

      <form onSubmit={handleSubmit} className="bg-white shadow rounded-lg p-6 space-y-6">
        <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">First Name</label>
              <input name="firstName" value={formData.firstName} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md p-2" required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Last Name</label>
              <input name="lastName" value={formData.lastName} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md p-2" required />
            </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">Email</label>
          <input name="email" type="email" value={formData.email} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md p-2" required />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700">Phone</label>
            <input name="phone" value={formData.phone || ''} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md p-2" />
          </div>

          <div>
             <label className="block text-sm font-medium text-gray-700">Date of Birth</label>
             {/* Input type='date' automatically formats to YYYY-MM-DD, exactly what Java LocalDate wants */}
             <input type="date" name="dob" value={formData.dob || ''} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md p-2" />
          </div>
        </div>

        <div>
            <label className="block text-sm font-medium text-gray-700">Gender</label>
            <select name="gender" value={formData.gender || 'MALE'} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md p-2">
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
            </select>
        </div>

        <div className="flex justify-end gap-4">
            <button type="button" onClick={() => navigate('/patients')} className="px-4 py-2 border rounded text-gray-600 hover:bg-gray-50">Cancel</button>
            <button type="submit" disabled={loading} className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
                {loading ? 'Saving...' : 'Save'}
            </button>
        </div>
      </form>
    </div>
  );
}