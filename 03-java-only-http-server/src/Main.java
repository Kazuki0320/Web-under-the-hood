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

				// サーバーソケットのインスタンスを作成
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

						// アクセスをリッスンする状態を作る
            while (true) {
								// クライアントからのリクエストを常に待っている状態
                try (Socket client = server.accept()) {
                    String status;
                    String contentType;
                    String responseBody;

                    System.out.println("Accepted: " + client.getRemoteSocketAddress());

										// リクエストされてきた文字列の内容を解析
										// 正しい理解: ここではソケット入力を「1行ずつ読み取れる形」に変換している（解析そのものはこの後）。
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));

										// リクエストメソッドを判定
										// 正しい理解: この行で読んでいるのは「リクエスト行」全体（例: GET /hello HTTP/1.1）。method判定はsplit後に行う。
                    String requestLine = reader.readLine(); // 例: GET / HTTP/1.1
                    if (requestLine == null || requestLine.isEmpty()) {
                        continue;
                    }

                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        // Step 4ではヘッダーを読み飛ばすだけ
                    }

                    String[] parts = requestLine.split(" ");
                    String method = parts.length > 0 ? parts[0] : "";
                    String path = parts.length > 1 ? parts[1] : "";
                    System.out.println("method=" + method + ", path=" + path);

										// 各リクエストごとに返す値を変える
                    if ("GET".equals(method) && "/".equals(path)) {
                        status = "200 OK";
                        contentType = "text/html; charset=UTF-8";
                        responseBody = "<h1>Welcome</h1><p>Top page</p>";
                    } else if ("GET".equals(method) && "/hello".equals(path)) {
                        status = "200 OK";
                        contentType = "text/plain; charset=UTF-8";
                        responseBody = "Hello World";
                    } else if ("GET".equals(method) && "/api/hello".equals(path)) {
                        status = "200 OK";
                        contentType = "application/json; charset=UTF-8";
                        responseBody = "{\"message\":\"Hello API\"}";
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

                    OutputStream out = client.getOutputStream();
                    out.write(headers.getBytes(StandardCharsets.UTF_8));
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