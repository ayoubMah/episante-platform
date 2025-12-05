package episante.aai.authservice;

import com.upec.episantecommon.enums.Role;

public class RegisterRequest {

    // Common fields
    private String email;
    private String password;
    private Role role;

    // Patient-specific fields
    private String firstName;
    private String lastName;
    private String dob;          // ISO string "2003-12-01"
    private String phone;

    // Doctor-specific fields
    private String specialty;
    private String rppsNumber;
    private String address;


    // getters and setters :)

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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
