package com.gps.telemetry;

import com.gps.telemetry.model.Telemetry;
import com.gps.telemetry.repository.TelemetryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TelemetryRepositoryTest {

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Test
    void telemetryCanBeSavedAndRetrieved() {
        String vehicleId = "DB_TEST_" + UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-10-20T10:00:00Z");

        Telemetry telemetry = new Telemetry(
                vehicleId,
                timestamp,
                19.033,
                73.0297,
                40.0,
                false,
                false,
                true,
                12.4,
                false
        );

        Telemetry saved = telemetryRepository.saveAndFlush(telemetry);

        assertNotNull(saved.getId(), "Database should generate an ID");

        Telemetry retrieved = telemetryRepository.findById(saved.getId())
                .orElseThrow(() ->
                        new AssertionError("Saved telemetry was not found"));

        assertEquals(vehicleId, retrieved.getVehicleId());
        assertEquals(timestamp, retrieved.getTimestamp());
        assertEquals(19.033, retrieved.getLatitude(), 0.000001);
        assertEquals(73.0297, retrieved.getLongitude(), 0.000001);
        assertEquals(40.0, retrieved.getSpeed(), 0.000001);
        assertEquals(12.4, retrieved.getBatteryVoltage(), 0.000001);
        assertTrue(retrieved.isIgnition());
    }
}
