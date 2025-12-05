package episante.aai.appointmentservice;

import com.upec.episantecommon.dto.*;
import com.upec.episantecommon.enums.AppointmentStatus;
import com.upec.episantecommon.exception.BadRequestException;
import com.upec.episantecommon.exception.NotFoundException;
import com.upec.episantecommon.security.SecurityUtils;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service("appointmentService")
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PatientClient patientClient;
    private final DoctorClient doctorClient;

    // ---------------------------
    // GET ALL (smart filtering)
    // ---------------------------
    public List<AppointmentResponseDTO> getAllForCurrentUser() {

        UUID userId = SecurityUtils.getCurrentUserId();

        if (SecurityUtils.isAdmin()) {
            return repository.findAll().stream().map(this::toResponse).toList();
        }

        if (SecurityUtils.isDoctor()) {
            return repository.findByDoctorId(userId).stream().map(this::toResponse).toList();
        }

        if (SecurityUtils.isPatient()) {
            return repository.findByPatientId(userId).stream().map(this::toResponse).toList();
        }

        throw new AccessDeniedException("Unauthorized");
    }

    // ---------------------------
    // CREATE
    // ---------------------------
    public AppointmentResponseDTO create(CreateAppointmentRequestDTO request) {

        validateAppointmentRequest(request);

        validatePatient(request.getPatientId());
        validateDoctor(request.getDoctorId());

        // Prevent double booking
        if (repository.existsOverlappingAppointment(
                request.getDoctorId(), request.getStartTime(), request.getEndTime()
        )) {
            throw new BadRequestException("Doctor is already booked for this time slot.");
        }

        Appointment a = new Appointment();
        a.setDoctorId(request.getDoctorId());
        a.setPatientId(request.getPatientId());
        a.setStartTime(request.getStartTime());
        a.setEndTime(request.getEndTime());
        a.setStatus(AppointmentStatus.PLANNED);

        a.setCreatedAt(OffsetDateTime.now());
        a.setUpdatedAt(OffsetDateTime.now());

        Appointment saved = repository.save(a);
        return toResponse(saved);
    }

    // ---------------------------
    // GET BY ID
    // ---------------------------
    public AppointmentResponseDTO getById(UUID id) {
        Appointment a = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
        return toResponse(a);
    }

    // ---------------------------
    // DETAILS (with doctor + patient info)
    // ---------------------------
    public AppointmentDetailsDTO getAppointmentDetails(UUID id) {

        Appointment a = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));

        AppointmentDetailsDTO dto = new AppointmentDetailsDTO();
        dto.setId(a.getId());
        dto.setStartTime(a.getStartTime());
        dto.setEndTime(a.getEndTime());
        dto.setStatus(a.getStatus());
        dto.setDoctorId(a.getDoctorId());
        dto.setPatientId(a.getPatientId());

        // Patient
        try {
            PatientResponseDTO patient = patientClient.getPatientById(a.getPatientId());
            dto.setPatientFullName(patient.getFirstName() + " " + patient.getLastName());
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Patient not found: " + a.getPatientId());
        }

        // Doctor (graceful)
        try {
            DoctorResponseDTO doc = doctorClient.getDoctorById(a.getDoctorId());
            dto.setDoctorFullName(doc.getFirstName() + " " + doc.getLastName());
            dto.setDoctorSpecialty(doc.getSpecialty());
        } catch (Exception e) {
            dto.setDoctorFullName("Unknown");
            dto.setDoctorSpecialty("Unavailable");
        }

        return dto;
    }

    // ---------------------------
    // UPDATE
    // ---------------------------
    public AppointmentResponseDTO update(UUID id, CreateAppointmentRequestDTO request) {

        Appointment existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));

        if (existing.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Cannot update past appointments.");
        }

        validateAppointmentRequest(request);

        if (!existing.getDoctorId().equals(request.getDoctorId())) {
            validateDoctor(request.getDoctorId());
        }

        if (!existing.getPatientId().equals(request.getPatientId())) {
            validatePatient(request.getPatientId());
        }

        boolean overlap = repository.existsOverlappingAppointment(
                request.getDoctorId(),
                request.getStartTime(),
                request.getEndTime(),
                id
        );

        if (overlap) {
            throw new BadRequestException("Doctor is booked during this time.");
        }

        existing.setDoctorId(request.getDoctorId());
        existing.setPatientId(request.getPatientId());
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setUpdatedAt(OffsetDateTime.now());

        Appointment saved = repository.save(existing);
        return toResponse(saved);
    }

    // ---------------------------
    // DELETE
    // ---------------------------
    public void delete(UUID id) {
        Appointment existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));

        if (existing.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Cannot delete past appointments.");
        }

        repository.delete(existing);
    }

    // ---------------------------
    // VALIDATION & HELPERS
    // ---------------------------
    private void validateAppointmentRequest(CreateAppointmentRequestDTO req) {
        if (req.getStartTime().isAfter(req.getEndTime())) {
            throw new BadRequestException("Start time must be before end time.");
        }
    }

    private void validatePatient(UUID id) {
        try {
            patientClient.getPatientById(id);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Patient not found: " + id);
        }
    }

    private void validateDoctor(UUID id) {
        try {
            doctorClient.getDoctorById(id);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Doctor not found: " + id);
        }
    }

    public UUID getPatientId(UUID appId) {
        return repository.findById(appId)
                .map(Appointment::getPatientId)
                .orElse(null);
    }

    public UUID getDoctorId(UUID appId) {
        return repository.findById(appId)
                .map(Appointment::getDoctorId)
                .orElse(null);
    }

    private AppointmentResponseDTO toResponse(Appointment a) {
        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setId(a.getId());
        dto.setDoctorId(a.getDoctorId());
        dto.setPatientId(a.getPatientId());
        dto.setStartTime(a.getStartTime());
        dto.setEndTime(a.getEndTime());
        dto.setStatus(a.getStatus());
        return dto;
    }
}