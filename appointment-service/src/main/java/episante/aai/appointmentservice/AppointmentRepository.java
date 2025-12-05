package episante.aai.appointmentservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    // -------------------------------------------------------
    // BASIC QUERIES FOR ROLE-BASED ACCESS
    // -------------------------------------------------------

    List<Appointment> findByDoctorId(UUID doctorId);

    List<Appointment> findByPatientId(UUID patientId);


    // -------------------------------------------------------
    // CREATE — check overlapping appointment
    // -------------------------------------------------------
    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Appointment a
        WHERE a.doctorId = :doctorId
          AND (a.startTime < :endTime AND a.endTime > :startTime)
        """)
    boolean existsOverlappingAppointment(
            UUID doctorId,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    );

    // -------------------------------------------------------
    // UPDATE — check overlapping but ignore current appointment
    // -------------------------------------------------------
    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Appointment a
        WHERE a.doctorId = :doctorId
          AND a.id <> :ignoreId
          AND (a.startTime < :endTime AND a.endTime > :startTime)
        """)
    boolean existsOverlappingAppointment(
            UUID doctorId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            UUID ignoreId
    );
}