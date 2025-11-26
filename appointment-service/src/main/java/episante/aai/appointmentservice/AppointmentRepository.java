package episante.aai.appointmentservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("""
    SELECT COUNT(a) > 0 FROM Appointment a
    WHERE a.doctorId = :doctorId
    AND a.status != 'CANCELED'
    AND (
        (a.startTime < :newEndTime AND a.endTime > :newStartTime)
    )
""")
    boolean existsOverlappingAppointment(UUID doctorId, OffsetDateTime newStartTime, OffsetDateTime newEndTime);
}
