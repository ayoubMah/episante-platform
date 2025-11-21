package episante.aai.appointmentservice;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PatientClient patientClient;
    private final DoctorClient doctorClient;

    public AppointmentResponseDTO create(CreateAppointmentRequestDTO request) {

        // 1. Validate Patient
        try {
            patientClient.getPatientById(request.getPatientId());
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Patient not found with ID: " + request.getPatientId());
        } catch (FeignException e) {
            throw new RuntimeException("Patient Service is down or unreachable");
        }

        // 2. Validate Doctor (Same logic)
        try {
            doctorClient.getDoctorById(request.getDoctorId());
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Doctor not found with ID: " + request.getDoctorId());
        }

        // 3. Create the appointment
        Appointment appointment = new Appointment();
        appointment.setDoctorId(request.getDoctorId());
        appointment.setPatientId(request.getPatientId());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());
        appointment.setStatus(request.getStatus());

        // 4️⃣ Save it to DB
        Appointment saved = repository.save(appointment);

        // 5️⃣ Return mapped DTO
        return toResponse(saved);
    }

    public AppointmentResponseDTO getById(UUID id) {
        Appointment appointment = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        return toResponse(appointment);
    }

    private AppointmentResponseDTO toResponse(Appointment a) {
        AppointmentResponseDTO resp = new AppointmentResponseDTO();
        resp.setId(a.getId());
        resp.setDoctorId(a.getDoctorId());
        resp.setPatientId(a.getPatientId());
        resp.setStartTime(a.getStartTime());
        resp.setEndTime(a.getEndTime());
        resp.setStatus(a.getStatus());
        return resp;
    }
}
