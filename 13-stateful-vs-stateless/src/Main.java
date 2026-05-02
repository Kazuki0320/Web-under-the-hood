import chapter13.controller.AuthController;
import chapter13.repository.UserRepository;
import chapter13.service.CookieSessionAuthService;
import chapter13.store.SessionStore;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    // TODO: 8080番ポートでServerSocketを起動する。
    // TODO: 接続をループで受け付ける。
    // TODO: AuthControllerを組み立てて、各リクエスト処理を委譲する。
    // TODO: このクラスは起動処理と依存配線だけに責務を限定する。
    private static final int PORT = 8080;

    public static void main(String[] args) {
        AuthController controller = createAuthController();
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                try (Socket client = server.accept()) {
                    System.out.println("Accepted: " + client.getRemoteSocketAddress());
                    handleClient(client, controller);
                } catch (IOException e) {
                    System.err.println("Failed to handle client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private static AuthController createAuthController() {
        UserRepository userRepository = new UserRepository();
        SessionStore sessionStore = new SessionStore();
        CookieSessionAuthService cookieSessionAuthService = new CookieSessionAuthService(userRepository, sessionStore);
        return new AuthController(cookieSessionAuthService);
    }

    private static void handleClient(Socket client, AuthController controller) throws IOException {
        controller.handle(client);
    }
}
