package episante.aai.doctorservice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/doctors")
public class DoctorInternalController {

    private final DoctorService service;

    public DoctorInternalController(DoctorService service) {
        this.service = service;
    }

    @PostMapping("/profile")
    public void createProfile(@RequestBody DoctorProfileRequest req) {
        service.createProfile(req);
    }
}

