package com.upec.healthalertengine;

import com.upec.episantecommon.event.HealthAlertEvent;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private static final int MAX_ALERTS = 1000;
    private final Deque<HealthAlertEvent> alertBuffer = new ConcurrentLinkedDeque<>();

    public void bufferAlert(HealthAlertEvent alert) {
        alertBuffer.addFirst(alert);
        if (alertBuffer.size() > MAX_ALERTS) {
            alertBuffer.removeLast();
        }
    }

    @GetMapping("/doctor/{doctorId}")
    public List<HealthAlertEvent> getAlertsForDoctor(@PathVariable UUID doctorId) {
        return alertBuffer.stream()
                .filter(a -> doctorId.equals(a.doctorId()))
                .toList();
    }

    @GetMapping("/patient/{patientId}")
    public List<HealthAlertEvent> getAlertsForPatient(@PathVariable UUID patientId) {
        return alertBuffer.stream()
                .filter(a -> patientId.equals(a.patientId()))
                .toList();
    }

    @GetMapping
    public List<HealthAlertEvent> getAllAlerts() {
        return List.copyOf(alertBuffer);
    }
}
