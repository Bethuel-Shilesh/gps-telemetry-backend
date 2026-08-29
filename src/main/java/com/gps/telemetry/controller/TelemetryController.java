package com.gps.telemetry.controller;

import com.gps.telemetry.model.Telemetry;
import com.gps.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    public Telemetry receiveTelemetry(
            @Valid @RequestBody Telemetry telemetry) {

        System.out.println(
                "Received telemetry from: " +
                telemetry.getVehicleId()
        );

        return telemetryService.saveTelemetry(telemetry);
    }
}