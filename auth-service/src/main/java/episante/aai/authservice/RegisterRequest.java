package episante.aai.authservice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.upec.episantecommon.enums.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private Role role;

    // Only PATIENT and DOCTOR may self-register; ADMIN accounts are provisioned out of band
    @AssertTrue(message = "role must be PATIENT or DOCTOR")
    @JsonIgnore
    public boolean isSelfRegistrableRole() {
        return role == null || role == Role.PATIENT || role == Role.DOCTOR;
    }

    // Patient-specific fields
    private String firstName;
    private String lastName;
    private String dob;          // ISO string "2003-12-01"
    private String phone;

    // Doctor-specific fields
    private String specialty;
    private String rppsNumber;
    private String address;
}
