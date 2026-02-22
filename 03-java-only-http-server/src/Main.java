import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
	public static void main(String[] args) {
		int port = 8080;

		try (ServerSocket server = new ServerSocket(port)) {
			System.out.println("Server listening on port" + port);

			while (true) {
				try (Socket client = server.accept()) {
					System.out.println("Accept: " + client.getRemoteSocketAddress()); 
				} catch (IOException e) {
					System.err.println("Failed to handle client: " + e.getMessage());
				}
			}
		} catch (IOException e) {
			System.out.println("Failed to start server: " + e.getMessage());
		}
	}
}