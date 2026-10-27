
package com.gps.telemetry.controller;

import com.gps.telemetry.model.Telemetry;
import com.gps.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin(origins = "http://localhost:5173")
public class TelemetryController {

    private static final Logger logger =
            LoggerFactory.getLogger(TelemetryController.class);

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    public Telemetry receiveTelemetry(
            @Valid @RequestBody Telemetry telemetry) {

        logger.info(
                "Received telemetry from: {}",
                telemetry.getVehicleId()
        );

        return telemetryService.saveTelemetry(telemetry);
    }

    @GetMapping
    public Page<Telemetry> getTelemetry(
            @RequestParam(required = false) String vehicleId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,
            @PageableDefault(
                    size = 20,
                    sort = "timestamp",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        if (vehicleId != null || from != null || to != null) {
            return telemetryService.searchTelemetry(
                    vehicleId, from, to, pageable
            );
        }

        return telemetryService.getTelemetry(pageable);
    }
}
