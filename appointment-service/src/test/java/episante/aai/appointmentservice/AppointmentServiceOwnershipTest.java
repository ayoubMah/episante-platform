package episante.aai.appointmentservice;

import com.upec.episantecommon.dto.CreateAppointmentRequestDTO;
import com.upec.episantecommon.enums.AppointmentStatus;
import com.upec.episantecommon.event.AppointmentCreatedEvent;
import com.upec.episantecommon.exception.ApiException;
import com.upec.episantecommon.security.UserContext;
import episante.aai.appointmentservice.config.KafkaTopicsProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// S-4: a patient/doctor must not create or move appointments for someone else
@ExtendWith(MockitoExtension.class)
class AppointmentServiceOwnershipTest {

    @Mock private AppointmentRepository repository;
    @Mock private PatientClient patientClient;
    @Mock private DoctorClient doctorClient;
    @Mock private KafkaTemplate<String, AppointmentCreatedEvent> kafkaTemplate;
    @Mock private KafkaTopicsProperties kafkaTopics;

    @InjectMocks private AppointmentService service;

    private final UUID me = UUID.randomUUID();
    private final UUID otherPatient = UUID.randomUUID();
    private final UUID doctor = UUID.randomUUID();
    private final UUID otherDoctor = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- CREATE ----------

    @Test
    void patientCreate_forOtherPatient_isBookedForThemselves() {
        loginAs(me, "PATIENT");
        stubSave();

        service.create(request(otherPatient, doctor));

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(me);
    }

    @Test
    void patientCreate_withoutPatientId_isBookedForThemselves() {
        loginAs(me, "PATIENT");
        stubSave();

        service.create(request(null, doctor));

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(me);
    }

    @Test
    void adminCreate_keepsRequestedPatient() {
        loginAs(UUID.randomUUID(), "ADMIN");
        stubSave();

        service.create(request(otherPatient, doctor));

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(otherPatient);
    }

    // ---------- UPDATE ----------

    @Test
    void patientUpdate_changingPatient_isForbidden() {
        loginAs(me, "PATIENT");
        when(repository.findById(appointmentId)).thenReturn(Optional.of(existing(me, doctor)));

        assertForbidden(() -> service.update(appointmentId, request(otherPatient, doctor)));
        verify(repository, never()).save(any());
    }

    @Test
    void patientUpdate_changingDoctor_isForbidden() {
        loginAs(me, "PATIENT");
        when(repository.findById(appointmentId)).thenReturn(Optional.of(existing(me, doctor)));

        assertForbidden(() -> service.update(appointmentId, request(me, otherDoctor)));
        verify(repository, never()).save(any());
    }

    @Test
    void doctorUpdate_changingPatient_isForbidden() {
        loginAs(doctor, "DOCTOR");
        when(repository.findById(appointmentId)).thenReturn(Optional.of(existing(me, doctor)));

        assertForbidden(() -> service.update(appointmentId, request(otherPatient, doctor)));
        verify(repository, never()).save(any());
    }

    @Test
    void doctorUpdate_handingOverToOtherDoctor_isForbidden() {
        loginAs(doctor, "DOCTOR");
        when(repository.findById(appointmentId)).thenReturn(Optional.of(existing(me, doctor)));

        assertForbidden(() -> service.update(appointmentId, request(me, otherDoctor)));
        verify(repository, never()).save(any());
    }

    @Test
    void patientUpdate_onlyTimes_isAllowed() {
        loginAs(me, "PATIENT");
        when(repository.findById(appointmentId)).thenReturn(Optional.of(existing(me, doctor)));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(appointmentId, request(null, null));

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(me);
        assertThat(saved.getValue().getDoctorId()).isEqualTo(doctor);
    }

    @Test
    void adminUpdate_canReassignPatientAndDoctor() {
        loginAs(UUID.randomUUID(), "ADMIN");
        when(repository.findById(appointmentId)).thenReturn(Optional.of(existing(me, doctor)));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(appointmentId, request(otherPatient, otherDoctor));

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(otherPatient);
        assertThat(saved.getValue().getDoctorId()).isEqualTo(otherDoctor);
    }

    // ---------- helpers ----------

    private void loginAs(UUID id, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                id + "@test", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(new UserContext(id, id + "@test", role));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private CreateAppointmentRequestDTO request(UUID patientId, UUID doctorId) {
        CreateAppointmentRequestDTO r = new CreateAppointmentRequestDTO();
        r.setPatientId(patientId);
        r.setDoctorId(doctorId);
        r.setStartTime(OffsetDateTime.now().plusDays(2));
        r.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(1));
        return r;
    }

    private Appointment existing(UUID patientId, UUID doctorId) {
        Appointment a = new Appointment();
        a.setId(appointmentId);
        a.setPatientId(patientId);
        a.setDoctorId(doctorId);
        a.setStartTime(OffsetDateTime.now().plusDays(1));
        a.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        a.setStatus(AppointmentStatus.PLANNED);
        return a;
    }

    private void stubSave() {
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(appointmentId);
            return a;
        });
        lenient().when(kafkaTopics.appointmentCreated()).thenReturn("appointment.created");
        CompletableFuture<SendResult<String, AppointmentCreatedEvent>> future = new CompletableFuture<>();
        lenient().when(kafkaTemplate.send(eq("appointment.created"), anyString(), any())).thenReturn(future);
    }

    private void assertForbidden(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(403));
    }
}
