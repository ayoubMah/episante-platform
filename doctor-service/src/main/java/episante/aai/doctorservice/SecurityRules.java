package episante.aai.doctorservice;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityRules {

    // --- DOCTOR PERMISSIONS ---

    // A doctor can only update their own profile
    public boolean isDoctorSelf(UUID doctorId) {
        return SecurityUtils.isDoctor() &&
                SecurityUtils.getCurrentUserId().equals(doctorId);
    }

    // A patient can view any doctor
    // A doctor can view any doctor
    // Only admin has full access to modify/delete
    public boolean canViewDoctor(UUID doctorId) {
        return true; // Fully public doctor viewing for now (Doctolib style)
    }

    public boolean canEditDoctor(UUID doctorId) {
        if (SecurityUtils.isAdmin()) return true;
        return SecurityUtils.isDoctor() &&
                SecurityUtils.getCurrentUserId().equals(doctorId);
    }

    public boolean canDeleteDoctor(UUID doctorId) {
        return SecurityUtils.isAdmin();
    }
}
