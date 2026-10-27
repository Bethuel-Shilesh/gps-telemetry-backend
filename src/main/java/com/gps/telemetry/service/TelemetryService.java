
package com.gps.telemetry.service;

import com.gps.telemetry.model.Telemetry;
import com.gps.telemetry.repository.TelemetryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;

    public TelemetryService(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    public Telemetry saveTelemetry(Telemetry telemetry) {
        return telemetryRepository.save(telemetry);
    }

    public Page<Telemetry> getTelemetry(Pageable pageable) {
        return telemetryRepository.findAll(pageable);
    }
}
