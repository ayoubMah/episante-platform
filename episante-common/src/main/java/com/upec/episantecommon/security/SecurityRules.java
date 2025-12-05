package com.upec.episantecommon.security;

import java.util.UUID;

import static com.upec.episantecommon.security.SecurityUtils.*;

public class SecurityRules {

    // Check if the current user owns this appointment as patient
    public boolean isOwnerPatient(UUID patientId) {
        return isPatient() && getCurrentUserId().equals(patientId);
    }

    // Check if the current doctor is assigned to this appointment
    public boolean isOwnerDoctor(UUID doctorId) {
        return isDoctor() && getCurrentUserId().equals(doctorId);
    }

    // Generic permission for any entity linked to a patient and doctor
    public boolean canView(UUID patientId, UUID doctorId) {
        if (isAdmin()) return true;
        if (isOwnerPatient(patientId)) return true;
        if (isOwnerDoctor(doctorId)) return true;
        return false;
    }

    public boolean canEdit(UUID patientId, UUID doctorId) {
        if (isAdmin()) return true;
        if (isOwnerPatient(patientId)) return true;
        if (isOwnerDoctor(doctorId)) return true;
        return false;
    }

    public boolean canDelete(UUID patientId, UUID doctorId) {
        if (isAdmin()) return true;
        if (isOwnerPatient(patientId)) return true;
        if (isOwnerDoctor(doctorId)) return true;
        return false;
    }

    // For things created by patient or admin
    public boolean canCreate() {
        return isPatient() || isAdmin();
    }

    // For patient-service endpoints
    public boolean canViewPatient(UUID patientId) {
        return isOwnerPatient(patientId)
                || isDoctor()
                || isAdmin();
    }

    public boolean isSelf(UUID patientId) {
        return isOwnerPatient(patientId);
    }
}
