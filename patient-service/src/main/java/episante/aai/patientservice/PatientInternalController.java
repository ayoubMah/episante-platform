package episante.aai.patientservice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/patients")
public class PatientInternalController {

    private final PatientService service;

    public PatientInternalController(PatientService service) {
        this.service = service;
    }

    @PostMapping("/profile")
    public void createProfile(@RequestBody PatientProfileRequest req) {
        service.createProfile(req);
    }
}

