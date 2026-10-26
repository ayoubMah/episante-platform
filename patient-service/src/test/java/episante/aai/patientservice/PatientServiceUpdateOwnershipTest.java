package episante.aai.patientservice;

import com.upec.episantecommon.dto.PatientProfileRequest;
import com.upec.episantecommon.exception.ApiException;
import com.upec.episantecommon.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// S-5: a patient must not reassign their own doctor or change their login email
@ExtendWith(MockitoExtension.class)
class PatientServiceUpdateOwnershipTest {

    @Mock private PatientRepository repo;

    @InjectMocks private PatientService service;

    private final UUID me = UUID.randomUUID();
    private final UUID myDoctor = UUID.randomUUID();
    private final UUID otherDoctor = UUID.randomUUID();
    private final String myEmail = "me@test.com";

    private Patient existing;

    @BeforeEach
    void setUp() {
        existing = new Patient();
        existing.setId(me);
        existing.setFirstName("Old");
        existing.setLastName("Name");
        existing.setEmail(myEmail);
        existing.setDoctorId(myDoctor);

        when(repo.findById(me)).thenReturn(Optional.of(existing));
        lenient().when(repo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- PATIENT ----------

    @Test
    void patient_changingDoctor_isForbidden() {
        loginAs(me, "PATIENT");

        assertThatThrownBy(() -> service.update(me, request(myEmail, otherDoctor)))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(403);
        verify(repo, never()).save(any());
    }

    @Test
    void patient_changingEmail_isForbidden() {
        loginAs(me, "PATIENT");

        assertThatThrownBy(() -> service.update(me, request("new@test.com", myDoctor)))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(403);
        verify(repo, never()).save(any());
    }

    @Test
    void patient_omittingDoctorAndEmail_keepsExistingValues() {
        loginAs(me, "PATIENT");

        Patient saved = service.update(me, request(null, null));

        assertThat(saved.getDoctorId()).isEqualTo(myDoctor);
        assertThat(saved.getEmail()).isEqualTo(myEmail);
        assertThat(saved.getFirstName()).isEqualTo("New");
    }

    @Test
    void patient_sendingSameDoctorAndEmail_updatesOtherFields() {
        loginAs(me, "PATIENT");

        Patient saved = service.update(me, request("ME@test.com", myDoctor));

        assertThat(saved.getDoctorId()).isEqualTo(myDoctor);
        assertThat(saved.getEmail()).isEqualTo(myEmail);
        assertThat(saved.getFirstName()).isEqualTo("New");
    }

    // ---------- ADMIN ----------

    @Test
    void admin_canChangeDoctorAndEmail() {
        loginAs(UUID.randomUUID(), "ADMIN");

        Patient saved = service.update(me, request("new@test.com", otherDoctor));

        assertThat(saved.getDoctorId()).isEqualTo(otherDoctor);
        assertThat(saved.getEmail()).isEqualTo("new@test.com");
    }

    // ---------- helpers ----------

    private void loginAs(UUID id, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                id + "@test", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(new UserContext(id, id + "@test", role));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private PatientProfileRequest request(String email, UUID doctorId) {
        PatientProfileRequest r = new PatientProfileRequest();
        r.setFirstName("New");
        r.setLastName("Name");
        r.setEmail(email);
        r.setDoctorId(doctorId);
        return r;
    }
}
