package episante.aai.patientservice;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityRules {

    // Called for endpoints like PUT /patients/{id}
    public boolean isSelf(UUID targetId) {
        UUID currentId = SecurityUtils.getCurrentUserId();
        return currentId != null && currentId.equals(targetId);
    }

    // Called for GET /patients/{id}
    public boolean canViewPatient(UUID patientId) {

        // ADMIN can view all
        if (SecurityUtils.isAdmin()) return true;

        // PATIENT can view themselves
        if (SecurityUtils.isPatient()) {
            return SecurityUtils.getCurrentUserId().equals(patientId);
        }

        // DOCTOR logic (MVP: allow all)
        if (SecurityUtils.isDoctor()) {
            return true; // production: check appointments
        }

        return false;
    }

    // Called for PUT /doctors/{id}
    public boolean isDoctorSelf(UUID doctorId) {
        return SecurityUtils.isDoctor() &&
                SecurityUtils.getCurrentUserId().equals(doctorId);
    }

    // Called for GET /appointments/{id}
    public boolean canAccessAppointment(UUID ownerId, UUID doctorId) {

        // ADMIN
        if (SecurityUtils.isAdmin()) return true;

        // PATIENT owners
        if (SecurityUtils.isPatient() &&
                SecurityUtils.getCurrentUserId().equals(ownerId)) return true;

        // DOCTOR assigned
        if (SecurityUtils.isDoctor() &&
                SecurityUtils.getCurrentUserId().equals(doctorId)) return true;

        return false;
    }
}


