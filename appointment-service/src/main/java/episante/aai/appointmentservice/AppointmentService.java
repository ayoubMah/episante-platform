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

        // 3. Check if doctor already booked for this time slot
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
    // ------------------------------------------------------
    // UPDATE APPOINTMENT
    // ------------------------------------------------------
    public AppointmentResponseDTO update(UUID id, CreateAppointmentRequestDTO request) {

        // 1. Load existing appointment
        Appointment existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // 2. Validate: cannot modify past appointments
        if (existing.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Cannot update past appointments.");
        }

        // 3. Validate doctor remains the same OR if changed, validate again
        if (!existing.getDoctorId().equals(request.getDoctorId())) {
            // Validate new doctor
            try {
                doctorClient.getDoctorById(request.getDoctorId());
            } catch (FeignException.NotFound e) {
                throw new IllegalArgumentException("Doctor not found: " + request.getDoctorId());
            }
        }

        // 4. Validate patient remains the same OR if changed, validate again
        if (!existing.getPatientId().equals(request.getPatientId())) {
            try {
                patientClient.getPatientById(request.getPatientId());
            } catch (FeignException.NotFound e) {
                throw new IllegalArgumentException("Patient not found: " + request.getPatientId());
            }
        }

        // 5. Prevent overlapping bookings for the doctor
        boolean overlapping = repository.existsOverlappingAppointment(
                request.getDoctorId(),
                request.getStartTime(),
                request.getEndTime(),
                id // ignore this appointment itself
        );

        if (overlapping) {
            throw new IllegalStateException("Doctor is already booked for this time range.");
        }

        // 6. Update fields
        existing.setDoctorId(request.getDoctorId());
        existing.setPatientId(request.getPatientId());
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setUpdatedAt(OffsetDateTime.now());

        // 7. Save
        Appointment saved = repository.save(existing);

        // 8. Convert to DTO
        return toResponse(saved);
    }


    // ------------------------------------------------------
    // DELETE APPOINTMENT
    // ------------------------------------------------------
    public void delete(UUID id) {

        Appointment existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Business rule:
        // Allow deleting only upcoming or planned appointments
        if (existing.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Cannot delete past appointments.");
        }

        repository.delete(existing);
    }

    // Helper method to extract patientId (used for @PreAuthorize)
    public UUID getPatientId(UUID appointmentId) {
        return repository.findById(appointmentId)
                .map(Appointment::getPatientId)
                .orElse(null);
    }

    // Helper method to extract doctorId (used for @PreAuthorize)
    public UUID getDoctorId(UUID appointmentId) {
        return repository.findById(appointmentId)
                .map(Appointment::getDoctorId)
                .orElse(null);
    }

}
