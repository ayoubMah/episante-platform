package episante.aai.appointmentservice;

import lombok.Data;

import java.util.UUID;

@Data
public class DoctorResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String specialization;
    private String email;
}
