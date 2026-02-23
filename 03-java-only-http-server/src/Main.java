import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
	public static void main(String[] args) {
		int port = 8080;
		String body = "Hello world";
		byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

		String headers =
						"HTTP/1.1 200 OK\r\n" +
						"Content-Type: text/plain; charset=UTF-8\r\n" +
						"Content-Length: " + bodyBytes.length + "\r\n" +
						"Connection: close\r\n" + 
						"\r\n";
		byte[] headerBytes = headers.getBytes(StandardCharsets.UTF_8);

		try (ServerSocket server = new ServerSocket(port)) {
			System.out.println("Server listening on port" + port);

			while(true) {
				try (Socket client = server.accept()) {
					System.out.println("Accepted: " + client.getRemoteSocketAddress());

					BufferedReader reader = new BufferedReader(
						new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
					
						String requestLine = reader.readLine(); // 例: GET / HTTP/1.1

					String line;
					while ((line = reader.readLine()) != null && !line.isEmpty()) {
						// Step4ではヘッダーを読み飛ばすだけ
					}

					String[] parts = requestLine.split(" ");
					String method = parts.length > 0 ? parts[0] : "";
					String path = parts.length > 1 ? parts[1] : "";
					System.out.println("method= " + method + ", path=" + path);

					OutputStream out = client.getOutputStream();
					out.write(headerBytes);
					out.write(bodyBytes);
					out.flush();
				} catch (IOException e) {
					System.err.println("Failed to handle client: " + e.getMessage());
				}
			}
		} catch (IOException e) {
			System.err.println("Failed to start server: " + e.getMessage());
		}
	}
}