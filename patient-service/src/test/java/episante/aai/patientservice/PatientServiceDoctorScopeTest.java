package episante.aai.patientservice;

import com.upec.episantecommon.security.UserContext;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// S-6: a doctor only sees patients they treat or have an appointment with
@ExtendWith(MockitoExtension.class)
class PatientServiceDoctorScopeTest {

    @Mock private PatientRepository repo;
    @Mock private AppointmentClient appointmentClient;

    @InjectMocks private PatientService service;

    private final UUID doctor = UUID.randomUUID();
    private final UUID otherDoctor = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 10);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- single patient ----------

    @Test
    void treatingDoctor_canViewPatient() {
        loginAs(doctor, "DOCTOR");
        when(repo.findById(patientId)).thenReturn(Optional.of(patient(doctor)));

        assertThat(service.isPatientOfCurrentDoctor(patientId)).isTrue();
    }

    @Test
    void doctorWithAppointment_canViewPatient() {
        loginAs(doctor, "DOCTOR");
        when(repo.findById(patientId)).thenReturn(Optional.of(patient(otherDoctor)));
        when(appointmentClient.getMyPatientIds()).thenReturn(List.of(patientId));

        assertThat(service.isPatientOfCurrentDoctor(patientId)).isTrue();
    }

    @Test
    void unrelatedDoctor_cannotViewPatient() {
        loginAs(doctor, "DOCTOR");
        when(repo.findById(patientId)).thenReturn(Optional.of(patient(otherDoctor)));
        when(appointmentClient.getMyPatientIds()).thenReturn(List.of(UUID.randomUUID()));

        assertThat(service.isPatientOfCurrentDoctor(patientId)).isFalse();
    }

    @Test
    void appointmentServiceDown_failsClosed() {
        loginAs(doctor, "DOCTOR");
        when(repo.findById(patientId)).thenReturn(Optional.of(patient(otherDoctor)));
        when(appointmentClient.getMyPatientIds()).thenThrow(mock(FeignException.class));

        assertThat(service.isPatientOfCurrentDoctor(patientId)).isFalse();
    }

    @Test
    void unknownPatient_isNotVisible() {
        loginAs(doctor, "DOCTOR");
        when(repo.findById(patientId)).thenReturn(Optional.empty());

        assertThat(service.isPatientOfCurrentDoctor(patientId)).isFalse();
    }

    @Test
    void nonDoctor_isNeverPatientOfCurrentDoctor() {
        loginAs(UUID.randomUUID(), "PATIENT");

        assertThat(service.isPatientOfCurrentDoctor(patientId)).isFalse();
        verify(repo, never()).findById(patientId);
    }

    // ---------- list ----------

    @Test
    void admin_listsAllPatients() {
        loginAs(UUID.randomUUID(), "ADMIN");
        Page<Patient> all = new PageImpl<>(List.of(patient(doctor), patient(otherDoctor)));
        when(repo.findAll(pageable)).thenReturn(all);

        assertThat(service.findAllForCurrentUser(pageable)).isSameAs(all);
    }

    @Test
    void doctor_listsOnlyOwnAndAppointmentPatients() {
        loginAs(doctor, "DOCTOR");
        List<UUID> withAppointment = List.of(patientId);
        Page<Patient> mine = new PageImpl<>(List.of(patient(doctor)));
        when(appointmentClient.getMyPatientIds()).thenReturn(withAppointment);
        when(repo.findByDoctorIdOrIdIn(doctor, withAppointment, pageable)).thenReturn(mine);

        assertThat(service.findAllForCurrentUser(pageable)).isSameAs(mine);
        verify(repo, never()).findAll(pageable);
    }

    @Test
    void doctor_withoutAppointments_listsOnlyTreatedPatients() {
        loginAs(doctor, "DOCTOR");
        Page<Patient> mine = new PageImpl<>(List.of(patient(doctor)));
        when(appointmentClient.getMyPatientIds()).thenThrow(mock(FeignException.class));
        when(repo.findByDoctorId(doctor, pageable)).thenReturn(mine);

        assertThat(service.findAllForCurrentUser(pageable)).isSameAs(mine);
        verify(repo, never()).findAll(pageable);
    }

    // ---------- helpers ----------

    private void loginAs(UUID id, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                id + "@test", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(new UserContext(id, id + "@test", role));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Patient patient(UUID doctorId) {
        Patient p = new Patient();
        p.setId(patientId);
        p.setEmail("p@test.com");
        p.setDoctorId(doctorId);
        return p;
    }
}
