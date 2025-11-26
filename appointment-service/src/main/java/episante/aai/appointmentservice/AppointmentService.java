package episante.aai.appointmentservice;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PatientClient patientClient;
    private final DoctorClient doctorClient;

    public List<AppointmentResponseDTO> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }


    public AppointmentResponseDTO create(CreateAppointmentRequestDTO request) {

        // 1. Validate Patient
        try {
            patientClient.getPatientById(request.getPatientId());
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Patient not found with ID: " + request.getPatientId());
        } catch (FeignException e) {
            throw new RuntimeException("Patient Service is down or unreachable");
        }

        // 2. Validate Doctor
        try {
            doctorClient.getDoctorById(request.getDoctorId());
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Doctor not found with ID: " + request.getDoctorId());
        } catch (FeignException e) {
            throw new RuntimeException("Doctor Service is down or unreachable");
        }

        // 3. Check if doctor already booked
        if (repository.existsOverlappingAppointment(
                request.getDoctorId(),
                request.getStartTime(),
                request.getEndTime()
        )) {
            throw new IllegalStateException("Doctor is already booked for this time slot.");
        }

        // 4. Create appointment
        Appointment appointment = new Appointment();
        appointment.setDoctorId(request.getDoctorId());
        appointment.setPatientId(request.getPatientId());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());
        appointment.setStatus(AppointmentStatus.PLANNED);

        // Handle timestamps
        appointment.setCreatedAt(OffsetDateTime.now());
        appointment.setUpdatedAt(OffsetDateTime.now());

        // 5. Save
        Appointment saved = repository.save(appointment);

        // 6. Return response
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

    public AppointmentDetailsDTO getAppointmentDetails(UUID id) {
        // 1. Read appointment from local DB
        Appointment appointment = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        AppointmentDetailsDTO dto = new AppointmentDetailsDTO();
        dto.setId(appointment.getId());
        dto.setStartTime(appointment.getStartTime());
        dto.setEndTime(appointment.getEndTime());
        dto.setStatus(appointment.getStatus());

        dto.setDoctorId(appointment.getDoctorId());
        dto.setPatientId(appointment.getPatientId());

        // 2. Fetch Patient info
        try {
            PatientResponseDTO patient = patientClient.getPatientById(appointment.getPatientId());
            dto.setPatientFullName(patient.getFirstName() + " " + patient.getLastName());
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Patient not found with ID: " + appointment.getPatientId());
        } catch (FeignException e) {
            throw new RuntimeException("Patient Service unavailable");
        }

        // 3. Fetch Doctor info with Graceful Degradation
        try {
            DoctorResponseDTO doctor = doctorClient.getDoctorById(appointment.getDoctorId());
            dto.setDoctorFullName(doctor.getFirstName() + " " + doctor.getLastName());
            dto.setDoctorSpecialty(doctor.getSpecialty());
        } catch (FeignException.NotFound e) {
            // ID invalid → this is a data integrity error
            dto.setDoctorFullName("Unknown");
            dto.setDoctorSpecialty(null);
        } catch (FeignException e) {
            // Entire Doctor service is DOWN → degrade gracefully
            dto.setDoctorFullName("Unknown");
            dto.setDoctorSpecialty("Unavailable");
        }

        return dto;
    }

}
