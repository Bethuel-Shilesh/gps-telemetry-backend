package com.gps.telemetry.service;

import com.gps.telemetry.model.Telemetry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;

@Service
public class TelemetryService {

    private final JdbcTemplate jdbcTemplate;

    public TelemetryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveTelemetry(Telemetry telemetry) {

        String sql = """
                INSERT INTO telemetry (
                    vehicle_id,
                    timestamp,
                    latitude,
                    longitude,
                    speed,
                    harsh_brake,
                    harsh_acceleration,
                    ignition,
                    battery_voltage,
                    crash_event
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                telemetry.getVehicleId(),
                Timestamp.from(telemetry.getTimestamp()),
                telemetry.getLatitude(),
                telemetry.getLongitude(),
                telemetry.getSpeed(),
                telemetry.isHarshBrake(),
                telemetry.isHarshAcceleration(),
                telemetry.isIgnition(),
                telemetry.getBatteryVoltage(),
                telemetry.isCrashEvent()
        );
    }
}