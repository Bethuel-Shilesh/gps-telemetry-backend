import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class InvalidCrcSimulator {

    public static void main(String[] args) {

        String host = "localhost";
        int port = 5000;

        byte[] packet = createInvalidPacket();

        try (Socket socket = new Socket(host, port)) {

            System.out.println("Connected to GPS backend");
            OutputStream output = socket.getOutputStream();

            output.write(packet);
            output.flush();

            System.out.println("Invalid CRC test packet sent");

            Thread.sleep(3000);

        } catch (Exception e) {
            System.err.println("Simulator error: " + e.getMessage());
        }
    }

    private static byte[] createInvalidPacket() {

        ByteArrayOutputStream packet = new ByteArrayOutputStream();

        packet.write(0x78);
        packet.write(0x78);
        packet.write(0x0D);
        packet.write(0x01);
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

        // Deliberately WRONG CRC
        packet.write(0x00);
        packet.write(0x00);

        packet.write(0x0D);
        packet.write(0x0A);

        return packet.toByteArray();
    }
}
