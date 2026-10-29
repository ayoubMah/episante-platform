package episante.aai.appointmentservice;

import com.upec.episantecommon.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// S-6: patient-service asks which patients the current doctor has appointments with
@ExtendWith(MockitoExtension.class)
class AppointmentServiceMyPatientsTest {

    @Mock private AppointmentRepository repository;
    @Mock private PatientClient patientClient;
    @Mock private DoctorClient doctorClient;

    @InjectMocks private AppointmentService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsPatientsOfTheDoctorInTheToken() {
        UUID doctor = UUID.randomUUID();
        List<UUID> patients = List.of(UUID.randomUUID(), UUID.randomUUID());
        loginAs(doctor, "DOCTOR");
        when(repository.findDistinctPatientIdsByDoctorId(doctor)).thenReturn(patients);

        assertThat(service.getPatientIdsForCurrentDoctor()).isEqualTo(patients);
    }

    private void loginAs(UUID id, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                id + "@test", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(new UserContext(id, id + "@test", role));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
