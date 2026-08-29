package com.gps.telemetry.tcp;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

public class Gt06PacketFramer {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final Consumer<byte[]> packetConsumer;

    public Gt06PacketFramer(Consumer<byte[]> packetConsumer) {
        this.packetConsumer = packetConsumer;
    }

    public void addBytes(byte[] data) {

        buffer.write(data, 0, data.length);

        processBuffer();
    }

    private void processBuffer() {

        byte[] bytes = buffer.toByteArray();

        if (bytes.length < 3) {
            return;
        }

        int start = findHeader(bytes);

        if (start == -1) {
            buffer.reset();
            return;
        }

        if (start > 0) {

            byte[] remaining = new byte[bytes.length - start];

            System.arraycopy(
                    bytes,
                    start,
                    remaining,
                    0,
                    remaining.length
            );

            buffer.reset();
            buffer.write(remaining, 0, remaining.length);

            bytes = remaining;
        }

        if (bytes.length < 3) {
            return;
        }

        int length = bytes[2] & 0xFF;

        int packetLength = length + 5;

        if (bytes.length < packetLength) {
            return;
        }

        byte[] packet = new byte[packetLength];

        System.arraycopy(
                bytes,
                0,
                packet,
                0,
                packetLength
        );

        System.out.println("Complete GT06 packet detected");

        printHex(packet);

        packetConsumer.accept(packet);

        buffer.reset();

        if (bytes.length > packetLength) {

            byte[] remaining =
                    new byte[bytes.length - packetLength];

            System.arraycopy(
                    bytes,
                    packetLength,
                    remaining,
                    0,
                    remaining.length
            );

            buffer.write(
                    remaining,
                    0,
                    remaining.length
            );

            processBuffer();
        }
    }

    private int findHeader(byte[] bytes) {

        for (int i = 0; i < bytes.length - 1; i++) {

            if ((bytes[i] & 0xFF) == 0x78 &&
                (bytes[i + 1] & 0xFF) == 0x78) {

                return i;
            }
        }

        return -1;
    }

    private void printHex(byte[] bytes) {

        StringBuilder result = new StringBuilder();

        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }

        System.out.println(result.toString().trim());
    }
}