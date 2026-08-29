package com.gps.telemetry.tcp;
import com.gps.telemetry.model.Telemetry;
import com.gps.telemetry.service.TelemetryService;
import java.time.Instant;

public class Gt06PacketDecoder {

    private final TelemetryService telemetryService;

    public Gt06PacketDecoder(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    public void decode(byte[] data) {

        System.out.println("---- GT06 PACKET ----");

        if (data == null || data.length < 8) {
            System.out.println("INVALID: Packet too short");
            return;
        }

        System.out.println("Packet length: " + data.length + " bytes");
        System.out.println("Packet HEX: " + bytesToHex(data));

        if (!hasValidHeader(data)) {
            System.out.println("INVALID: Unknown packet header");
            return;
        }

        int lengthField = data[2] & 0xFF;
        int expectedLength = lengthField + 5;

        if (data.length != expectedLength) {
            System.out.println(
                    "INVALID: Packet length mismatch. " +
                    "Expected: " + expectedLength +
                    ", Received: " + data.length
            );
            return;
        }

        int protocolNumber = data[3] & 0xFF;

        System.out.println(
                String.format(
                        "Protocol number: 0x%02X",
                        protocolNumber
                )
        );

        if (!hasValidStopBits(data)) {
            System.out.println("INVALID: Stop bits");
            return;
        }

        if (!hasValidCrc(data)) {
            System.out.println("INVALID: CRC check failed");
            return;
        }

        System.out.println("GT06 PACKET VALID");

        Telemetry telemetry = createSimulatedTelemetry();

        Telemetry savedTelemetry =
                telemetryService.saveTelemetry(telemetry);

        System.out.println(
                "Telemetry saved with ID: " +
                savedTelemetry.getId()
        );

        System.out.println("---------------------");
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

        System.out.println(
                String.format(
                        "Received CRC: 0x%04X",
                        receivedCrc
                )
        );

        System.out.println(
                String.format(
                        "Calculated CRC: 0x%04X",
                        calculatedCrc
                )
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