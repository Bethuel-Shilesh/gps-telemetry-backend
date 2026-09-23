import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class InvalidLengthSimulator {

    public static void main(String[] args) {

        String host = "localhost";
        int port = 5000;

        byte[] packet = createInvalidLengthPacket();

        try (Socket socket = new Socket(host, port)) {

            System.out.println("Connected to GPS backend");

            OutputStream output = socket.getOutputStream();

            output.write(packet);
            output.flush();

            System.out.println("Invalid length test packet sent");

            Thread.sleep(3000);

        } catch (Exception e) {

            System.err.println("Simulator error: " + e.getMessage());
        }
    }

    private static byte[] createInvalidLengthPacket() {

        ByteArrayOutputStream packet = new ByteArrayOutputStream();

        // Valid GT06 header
        packet.write(0x78);
        packet.write(0x78);

        // Deliberately WRONG length
        // Actual packet structure is based on our valid test packet,
        // but the declared length is intentionally incorrect.
        packet.write(0x20);

        // Protocol
        packet.write(0x01);

        // Test data
        packet.write(0x23);
        packet.write(0x45);
        packet.write(0x67);
        packet.write(0x89);
        packet.write(0x01);
        packet.write(0x23);
        packet.write(0x45);
        packet.write(0x67);

        packet.write(0x00);
        packet.write(0x01);

        // CRC
        byte[] data = packet.toByteArray();

        int crc = crc16X25(data, 3, data.length);

        packet.write((crc >> 8) & 0xFF);
        packet.write(crc & 0xFF);

        // Stop bits
        packet.write(0x0D);
        packet.write(0x0A);

        return packet.toByteArray();
    }

    private static int crc16X25(byte[] data, int start, int end) {

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
}
