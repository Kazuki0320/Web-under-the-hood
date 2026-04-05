import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("API server listening on http://127.0.0.1:" + PORT);

            while (true) {
                try (Socket client = server.accept()) {
                    handleClient(client);
                } catch (Exception e) {
                    System.err.println("Failed to handle client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private static void handleClient(Socket client) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
            );

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
            }

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "";

            String status;
            String body;

            if ("GET".equals(method) && "/health".equals(path)) {
                status = "200 OK";
                body = "{\"status\":\"ok\"}";
            } else if ("GET".equals(method) && "/hello".equals(path)) {
                status = "200 OK";
                body = "{\"message\":\"hello\"}";
            } else {
                status = "404 Not Found";
                body = "{\"error\":\"not found\"}";
            }

            sendJson(client, status, body);
        } catch (Exception e) {
            sendJson(client, "500 Internal Server Error", "{\"error\":\"internal server error\"}");
            System.err.println("Request handling error: " + e.getMessage());
        }
    }

    private static void sendJson(Socket client, String status, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String headers =
            "HTTP/1.1 " + status + "\r\n" +
            "Content-Type: application/json; charset=UTF-8\r\n" +
            "Content-Length: " + bodyBytes.length + "\r\n" +
            "Connection: close\r\n" +
            "\r\n";

        OutputStream out = client.getOutputStream();
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }
}
