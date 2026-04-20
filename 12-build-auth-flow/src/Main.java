import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import controller.AuthController;
import repository.UserRepository;
import service.AuthService;
import store.TokenStore;

public class Main {
    private static final int PORT = 8080;
    
    public static void main(String args[]) {
        AuthController controller = createAuthController();
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server listening on port" + PORT);

            while(true) {
                try (Socket client = server.accept()) {
                    System.out.println("Accepted:" + client.getRemoteSocketAddress());
                    handleClient(client, controller);
                } catch (IOException e) {
                    System.err.println("Failed");
                }
            }
        } catch (IOException e) {
            System.err.println("Failed" + e.getMessage());
        }
    }

    private static AuthController createAuthController() {
        UserRepository userRepository = new UserRepository();
        TokenStore tokenStore = new TokenStore();
        AuthService authService = new AuthService(userRepository, tokenStore);
        return new AuthController(authService);
    }

    private static void handleClient(Socket client, AuthController controller) throws IOException {
        controller.handle(client);
    }
}