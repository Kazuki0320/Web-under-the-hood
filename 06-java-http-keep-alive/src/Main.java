import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final int PORT = 8080;
    // 接続がアイドル状態でこの時間を超えたら読み取りをタイムアウトさせる
    private static final int IDLE_TIMEOUT_MS = 5000;
    // 1つのTCP接続で受け付ける最大リクエスト数
    private static final int MAX_REQUESTS_PER_CONNECTION = 5;

    public static void main(String[] args) {
        // サーバーソケットを作成して待ち受け開始
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);
            while (true) {
                // クライアント接続を1つ受け付ける
                try (Socket client = server.accept()) {
                    System.out.println("Accepted: " + client.getRemoteSocketAddress());
                    handleConnection(client);
                } catch (IOException e) {
                    System.err.println("Failed to handle client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private static void handleConnection(Socket client) throws IOException {
        // 同一接続が長く遊ばないようにタイムアウト設定
        client.setSoTimeout(IDLE_TIMEOUT_MS);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
        );
        OutputStream out = client.getOutputStream();
        int requestCount = 0;

        // keep-alive: 同じTCP接続で複数リクエストを順番に処理
        while (requestCount < MAX_REQUESTS_PER_CONNECTION) {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                // 接続終了 or 不正/空リクエスト
                break;
            }
            requestCount++;

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "";

            // ヘッダーを空行まで読み取ってMapに格納する
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim().toLowerCase();
                    String value = line.substring(idx + 1).trim();
                    headers.put(key, value);
                }
            }

            // クライアントが close を要求したらこのレスポンス後に切断する
            boolean closeConnection = "close".equalsIgnoreCase(headers.getOrDefault("connection", ""));

            String status;
            String body;
            if ("GET".equals(method) && "/hello".equals(path)) {
                status = "200 OK";
                body = "Hello, World";
            } else {
                status = "404 Not Found";
                body = "Not Found";
            }

            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            String responseHeaders =
                "HTTP/1.1 " + status + "\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: " + (closeConnection ? "close" : "keep-alive") + "\r\n" +
                "\r\n";

            // HTTPレスポンスを「ヘッダー -> 本文」の順に返す
            out.write(responseHeaders.getBytes(StandardCharsets.UTF_8));
            out.write(bodyBytes);
            out.flush();

            if (closeConnection) {
                break;
            }
        }
    }
}
