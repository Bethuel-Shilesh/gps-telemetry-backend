package com.gps.telemetry.controller;

import com.gps.telemetry.model.Telemetry;
import com.gps.telemetry.service.DatabaseTestService;
import com.gps.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final DatabaseTestService databaseTestService;
    private final TelemetryService telemetryService;

    public TelemetryController(
            DatabaseTestService databaseTestService,
            TelemetryService telemetryService) {

        this.databaseTestService = databaseTestService;
        this.telemetryService = telemetryService;
    }

    @PostMapping
    public Telemetry receiveTelemetry(
            @Valid@ RequestBody Telemetry telemetry) {

        System.out.println(
                "Received telemetry from: " + telemetry.getVehicleId()
        );

        telemetryService.saveTelemetry(telemetry);

        return telemetry;
    }

    @GetMapping("/db-test")
    public String databaseTest() {
        return databaseTestService.testConnection();
    }
}