import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class Main {
    private static final int PORT = 10053;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("DNS server listening on UDP " + PORT);

            while (true) {
                try {
                    byte[] buf = new byte[512];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    byte[] query = Arrays.copyOf(packet.getData(), packet.getLength());
                    byte[] response = buildResponse(query);
                    if (response == null) {
                        continue;
                    }

                    DatagramPacket reply = new DatagramPacket(
                        response,
                        response.length,
                        packet.getAddress(),
                        packet.getPort()
                    );
                    socket.send(reply);
                } catch (IOException e) {
                    System.err.println("Failed to handle packet: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to run DNS server: " + e.getMessage());
        }
    }

    private static byte[] buildResponse(byte[] query) {
        if (query.length < 12) {
            return null;
        }

        int qdCount = ((query[4] & 0xFF) << 8) | (query[5] & 0xFF);
        if (qdCount != 1) {
            return null;
        }

        int idx = 12;
        while (idx < query.length && query[idx] != 0) {
            idx += (query[idx] & 0xFF) + 1;
        }
        if (idx + 5 >= query.length) {
            return null;
        }

        int qnameEnd = idx + 1;
        int qtype = ((query[qnameEnd] & 0xFF) << 8) | (query[qnameEnd + 1] & 0xFF);
        int qclass = ((query[qnameEnd + 2] & 0xFF) << 8) | (query[qnameEnd + 3] & 0xFF);

        if (qtype != 1 || qclass != 1) {
            return null;
        }

        byte[] response = new byte[query.length + 16];
        System.arraycopy(query, 0, response, 0, query.length);

        response[2] = (byte) 0x81;
        response[3] = (byte) 0x80;
        response[6] = 0x00;
        response[7] = 0x01;

        int ans = query.length;
        response[ans] = (byte) 0xC0;
        response[ans + 1] = 0x0C;
        response[ans + 2] = 0x00;
        response[ans + 3] = 0x01;
        response[ans + 4] = 0x00;
        response[ans + 5] = 0x01;
        response[ans + 6] = 0x00;
        response[ans + 7] = 0x00;
        response[ans + 8] = 0x00;
        response[ans + 9] = 0x3C;
        response[ans + 10] = 0x00;
        response[ans + 11] = 0x04;
        response[ans + 12] = 127;
        response[ans + 13] = 0;
        response[ans + 14] = 0;
        response[ans + 15] = 1;

        return response;
    }
}
