import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		int port = 8083;

		try (ServerSocket server = new ServerSocket(port)) {
			System.out.println("Server listening on port: " + port);

			while(true) {
				try (Socket client = server.accept()) {
					BufferedReader reader = new BufferedReader(
						new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
					);

					String requestLine = reader.readLine();
					if (requestLine == null || requestLine.isEmpty()) {
						continue;
					}

					String line;
					while ((line = reader.readLine()) != null && !line.isEmpty()) {
					}
					
					String[] parts = requestLine.split(" ");
					String method = parts.length > 0 ? parts[0] : "";
					String path = parts.length > 1 ? parts[1] : "";

					String status;
					String contentType;
					String responseBody;

					if ("GET".equals(method) && "/hello".equals(path)) {
						status = "200 OK";
						contentType = "text/plain; charset=UTF-8";
						responseBody = "Hello World";
					} else {
						status = "404 Not Found";
						contentType = "text/plain; charset=UTF-8";
						responseBody = "Not Found";
					}

					byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
					String headers = 
						"HTTP/1.1 " + status + "\r\n" +
						"Content-Type: " + contentType + "\r\n" +
						"Content-Length: " + bodyBytes.length + "\r\n" +
						"Connection: close\r\n" +
						"\r\n";

					OutputStream output = client.getOutputStream();
					output.write(headers.getBytes(StandardCharsets.UTF_8));
					output.write(bodyBytes);
					output.flush();
				} catch(IOException e) {
					System.err.println("Failed to handle client: " + e.getMessage());
				}
			}
		} catch(IOException e) {
			System.err.println("Failed to start server: " + e.getMessage());
		}
	}
}