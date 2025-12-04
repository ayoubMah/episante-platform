package com.upec.episantecommon.enums;

public enum Role {
    ADMIN,          // Full access to all services, dashboards, configs
    DOCTOR,         // Can view patients, appointments, alerts
    PATIENT,        // Can view own data, appointments
}
