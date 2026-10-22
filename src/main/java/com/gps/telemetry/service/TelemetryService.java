
package com.gps.telemetry.service;

import com.gps.telemetry.model.Telemetry;
import com.gps.telemetry.repository.TelemetryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;

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

    public Page<Telemetry> searchTelemetry(
            String vehicleId,
            Instant fromTime,
            Instant toTime,
            Pageable pageable) {

        Specification<Telemetry> specification =
                (root, query, builder) -> builder.conjunction();

        if (vehicleId != null && !vehicleId.isBlank()) {
            String pattern = "%" + vehicleId.trim().toLowerCase() + "%";

            specification = specification.and(
                    (root, query, builder) ->
                            builder.like(
                                    builder.lower(root.get("vehicleId")),
                                    pattern
                            )
            );
        }

        if (fromTime != null) {
            specification = specification.and(
                    (root, query, builder) ->
                            builder.greaterThanOrEqualTo(
                                    root.get("timestamp"),
                                    fromTime
                            )
            );
        }

        if (toTime != null) {
            specification = specification.and(
                    (root, query, builder) ->
                            builder.lessThan(
                                    root.get("timestamp"),
                                    toTime
                            )
            );
        }

        return telemetryRepository.findAll(specification, pageable);
    }
}
