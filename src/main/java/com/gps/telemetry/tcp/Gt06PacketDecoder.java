package com.gps.telemetry.tcp;

import com.gps.telemetry.model.Telemetry;
import com.gps.telemetry.service.TelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class Gt06PacketDecoder {

    private static final Logger logger =
            LoggerFactory.getLogger(Gt06PacketDecoder.class);

    private final TelemetryService telemetryService;

    public Gt06PacketDecoder(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    public void decode(byte[] data) {

        logger.info("---- GT06 PACKET ----");

        if (data == null || data.length < 8) {
            logger.warn("INVALID: Packet too short");
            return;
        }

        logger.debug("Packet length: {} bytes", data.length);
        logger.debug("Packet HEX: {}", bytesToHex(data));

        if (!hasValidHeader(data)) {
            logger.warn("INVALID: Unknown packet header");
            return;
        }

        int lengthField = data[2] & 0xFF;
        int expectedLength = lengthField + 5;

        if (data.length != expectedLength) {
            logger.warn(
                    "INVALID: Packet length mismatch. Expected: {}, Received: {}",
                    expectedLength,
                    data.length
            );
            return;
        }

        int protocolNumber = data[3] & 0xFF;

        logger.debug(
                "Protocol number: 0x{}",
                String.format("%02X", protocolNumber)
        );

        if (!hasValidStopBits(data)) {
            logger.warn("INVALID: Stop bits");
            return;
        }

        if (!hasValidCrc(data)) {
            logger.warn("INVALID: CRC check failed");
            return;
        }

        logger.info("GT06 PACKET VALID");

        Telemetry telemetry = createSimulatedTelemetry();

        Telemetry savedTelemetry =
                telemetryService.saveTelemetry(telemetry);

        logger.info(
                "Telemetry saved with ID: {}",
                savedTelemetry.getId()
        );

        logger.info("---------------------");
    }

    private boolean hasValidHeader(byte[] data) {

        return (data[0] & 0xFF) == 0x78 &&
               (data[1] & 0xFF) == 0x78;
    }

    private boolean hasValidStopBits(byte[] data) {

        int last = data.length - 2;

        return (data[last] & 0xFF) == 0x0D &&
               (data[last + 1] & 0xFF) == 0x0A;
    }

    private boolean hasValidCrc(byte[] data) {

        int crcIndex = data.length - 4;

        int receivedCrc =
                ((data[crcIndex] & 0xFF) << 8)
                | (data[crcIndex + 1] & 0xFF);

        int calculatedCrc = crc16X25(
                data,
                3,
                crcIndex
        );

        logger.debug(
                "Received CRC: 0x{}",
                String.format("%04X", receivedCrc)
        );

        logger.debug(
                "Calculated CRC: 0x{}",
                String.format("%04X", calculatedCrc)
        );

        return receivedCrc == calculatedCrc;
    }

    private int crc16X25(
            byte[] data,
            int start,
            int end) {

        int crc = 0xFFFF;

        for (int i = start; i < end; i++) {

            crc ^= data[i] & 0xFF;

            for (int j = 0; j < 8; j++) {

                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0x8408;
                } else {
                    crc >>>= 1;
                }
            }
        }

        return (~crc) & 0xFFFF;
    }

    private String bytesToHex(byte[] bytes) {

        StringBuilder result = new StringBuilder();

        for (byte b : bytes) {
            result.append(
                    String.format("%02X ", b)
            );
        }

        return result.toString().trim();
    }

    private Telemetry createSimulatedTelemetry() {

        Telemetry telemetry = new Telemetry();

        telemetry.setVehicleId("SIMULATED_AW01");
        telemetry.setTimestamp(Instant.now());
        telemetry.setLatitude(19.0760);
        telemetry.setLongitude(72.8777);
        telemetry.setSpeed(45.5);
        telemetry.setHarshBrake(false);
        telemetry.setHarshAcceleration(false);
        telemetry.setIgnition(true);
        telemetry.setBatteryVoltage(12.6);
        telemetry.setCrashEvent(false);

        return telemetry;
    }
}