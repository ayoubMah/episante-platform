package episante.aai.appointmentservice;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityRules {

    // Check if the current user owns this appointment as patient
    public boolean isOwnerPatient(UUID patientId) {
        return SecurityUtils.isPatient() &&
                SecurityUtils.getCurrentUserId().equals(patientId);
    }

    // Check if the current doctor is assigned to this appointment
    public boolean isOwnerDoctor(UUID doctorId) {
        return SecurityUtils.isDoctor() &&
                SecurityUtils.getCurrentUserId().equals(doctorId);
    }

    public boolean canView(UUID patientId, UUID doctorId) {
        if (SecurityUtils.isAdmin()) return true;
        if (isOwnerPatient(patientId)) return true;
        if (isOwnerDoctor(doctorId)) return true;
        return false;
    }

    public boolean canEdit(UUID patientId, UUID doctorId) {
        if (SecurityUtils.isAdmin()) return true;
        if (isOwnerPatient(patientId)) return true;
        if (isOwnerDoctor(doctorId)) return true;
        return false;
    }

    public boolean canDelete(UUID patientId, UUID doctorId) {
        if (SecurityUtils.isAdmin()) return true;
        if (isOwnerPatient(patientId)) return true;
        if (isOwnerDoctor(doctorId)) return true;
        return false;
    }

    public boolean canCreate() {
        return SecurityUtils.isPatient() || SecurityUtils.isAdmin();
    }
}