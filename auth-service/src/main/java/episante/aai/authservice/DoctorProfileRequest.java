package episante.aai.authservice;

import java.util.UUID;

public class DoctorProfileRequest {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String specialty;
    private String rppsNumber;
    private String address;

    public DoctorProfileRequest(UUID id, String firstName, String lastName, String email, String specialty, String rppsNumber, String address) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.specialty = specialty;
        this.rppsNumber = rppsNumber;
        this.address = address;
    }

    // getters & setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getRppsNumber() {
        return rppsNumber;
    }

    public void setRppsNumber(String rppsNumber) {
        this.rppsNumber = rppsNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
