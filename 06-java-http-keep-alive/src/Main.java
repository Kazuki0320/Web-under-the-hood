import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        int port = 8080;
        String status;
        String body;

        // 1. 接続受理ブロック（穴埋め）
        try(ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            while(true) {
                try(Socket client = server.accept()) {
                    System.out.println("client: " + client + "\r\n");
                    System.out.println("Accepted: " + client.getRemoteSocketAddress());

                    // 2. リクエスト解析ブロック
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
                    );

                    String requestLine = reader.readLine(); //例：GET /hello HTTP/1.1
                    if (requestLine == null || requestLine.isEmpty()) {
                        continue;
                    };

                    String[] parts = requestLine.split(" ");
                    String method = parts.length > 0 ? parts[0] : " ";
                    String path = parts.length > 1 ? parts[1] : " ";
                    String version = parts.length > 2 ? parts[2] : " ";

                    // 3. レスポンス返却ブロック
                    if ("GET".equals(method) && "/hello".equals(path)) {
                        status = "200 OK";
                        body = "Hello, World";
                    } else {
                        status = "404 Not Found";
                        body = "Not Found";
                    }

                    byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

                    String headers = 
                        "HTTP/1.1 " + status + "\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" + 
                        "Context-Length: " + bodyBytes.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";
                    
                    OutputStream output = client.getOutputStream();
                    output.write(headers.getBytes(StandardCharsets.UTF_8));
                    output.write(bodyBytes);
                    output.flush();
                } catch(IOException e) {
                    System.err.println("Failed to start 何とか" + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start 何とか: " + e.getMessage());
        }
        //         } catch (IOException e) {
        //             System.err.println("Failed to handle client: " + e.getMessage());
        //         }
        //     }
        // } catch (IOException e) {
        //     System.err.println("Failed to start server: " + e.getMessage());
        // }
    }
}
