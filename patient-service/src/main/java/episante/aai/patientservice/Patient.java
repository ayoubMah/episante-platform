package episante.aai.patientservice;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 30)
    private String phone;

    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public enum Gender {
        MALE, FEMALE
    }
}