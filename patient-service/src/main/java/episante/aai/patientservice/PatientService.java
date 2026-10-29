package episante.aai.patientservice;

import com.upec.episantecommon.dto.PatientProfileRequest;
import com.upec.episantecommon.exception.DuplicateResourceException;
import com.upec.episantecommon.exception.NotFoundException;
import com.upec.episantecommon.security.SecurityUtils;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import com.upec.episantecommon.enums.Gender;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository repo;
    private final AppointmentClient appointmentClient;

    public PatientService(PatientRepository repo, AppointmentClient appointmentClient) {
        this.repo = repo;
        this.appointmentClient = appointmentClient;
    }

    public List<Patient> findAll() {
        return repo.findAll();
    }

    public Page<Patient> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    // --- SCOPE (S-6): admin sees everyone, a doctor only their treated / appointment patients ---
    public Page<Patient> findAllForCurrentUser(Pageable pageable) {
        if (SecurityUtils.isAdmin()) {
            return repo.findAll(pageable);
        }

        UUID doctorId = SecurityUtils.getCurrentUserId();
        List<UUID> withAppointment = patientIdsWithAppointment();
        if (withAppointment.isEmpty()) {
            return repo.findByDoctorId(doctorId, pageable);
        }
        return repo.findByDoctorIdOrIdIn(doctorId, withAppointment, pageable);
    }

    // Used by @PreAuthorize: is this patient treated by, or booked with, the current doctor?
    public boolean isPatientOfCurrentDoctor(UUID patientId) {
        if (!SecurityUtils.isDoctor()) return false;

        Patient patient = repo.findById(patientId).orElse(null);
        if (patient == null) return false;

        return SecurityUtils.getCurrentUserId().equals(patient.getDoctorId())
                || patientIdsWithAppointment().contains(patientId);
    }

    // Fail closed: if appointment-service is unreachable, only the treating-doctor link counts
    private List<UUID> patientIdsWithAppointment() {
        try {
            return appointmentClient.getMyPatientIds();
        } catch (FeignException e) {
            log.warn("Could not fetch the doctor's appointment patients (status {})", e.status());
            return List.of();
        }
    }

    public Patient findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + id));
    }

    public List<Patient> findAllByIds(List<UUID> ids) {
        return repo.findAllById(ids);
    }

    @Transactional
    public Patient createProfile(PatientProfileRequest req) {
        if (repo.existsById(req.getId())) {
            throw new DuplicateResourceException("Profile already exists");
        }

        Patient p = new Patient();
        p.setId(req.getId());
        p.setFirstName(req.getFirstName());
        p.setLastName(req.getLastName());
        p.setEmail(req.getEmail());
        p.setPhone(req.getPhone());

        // Safe Date Parsing
        if (req.getDob() != null) {
            try {
                p.setDob(LocalDate.parse(req.getDob()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD");
            }
        }

        if (req.getGender() != null) {
            p.setGender(Gender.valueOf(req.getGender()));
        }

        p.setDoctorId(req.getDoctorId());

        return repo.save(p);
    }

    @Transactional
    public Patient update(UUID id, PatientProfileRequest req) {
        Patient existing = findById(id);

        existing.setFirstName(req.getFirstName());
        existing.setLastName(req.getLastName());
        existing.setEmail(req.getEmail());
        existing.setPhone(req.getPhone());

        if (req.getDob() != null) {
            existing.setDob(LocalDate.parse(req.getDob()));
        }

        if (req.getGender() != null) {
            existing.setGender(Gender.valueOf(req.getGender()));
        }

        existing.setDoctorId(req.getDoctorId());

        return repo.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new NotFoundException("Patient not found");
        repo.deleteById(id);
    }
}