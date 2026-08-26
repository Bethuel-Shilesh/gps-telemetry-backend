package com.gps.telemetry.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public class Telemetry {

    @NotBlank
    private String vehicleId;

    @NotNull
    private Instant timestamp;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private double longitude;

    @PositiveOrZero
    private double speed;

    private boolean harshBrake;

    private boolean harshAcceleration;

    private boolean ignition;

    @PositiveOrZero
    private double batteryVoltage;

    private boolean crashEvent;

    public Telemetry() {
    }

    public Telemetry(String vehicleId, Instant timestamp,
                     double latitude, double longitude, double speed,
                     boolean harshBrake, boolean harshAcceleration,
                     boolean ignition, double batteryVoltage,
                     boolean crashEvent) {

        this.vehicleId = vehicleId;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.harshBrake = harshBrake;
        this.harshAcceleration = harshAcceleration;
        this.ignition = ignition;
        this.batteryVoltage = batteryVoltage;
        this.crashEvent = crashEvent;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public boolean isHarshBrake() {
        return harshBrake;
    }

    public void setHarshBrake(boolean harshBrake) {
        this.harshBrake = harshBrake;
    }

    public boolean isHarshAcceleration() {
        return harshAcceleration;
    }

    public void setHarshAcceleration(boolean harshAcceleration) {
        this.harshAcceleration = harshAcceleration;
    }

    public boolean isIgnition() {
        return ignition;
    }

    public void setIgnition(boolean ignition) {
        this.ignition = ignition;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public boolean isCrashEvent() {
        return crashEvent;
    }

    public void setCrashEvent(boolean crashEvent) {
        this.crashEvent = crashEvent;
    }
}