import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// public class Main {
//     public static void main(String[] args) {
//         int port = 8080;

//         try (ServerSocket server = new ServerSocket(port)) {
//             System.out.println("Server listening on port " + port);
//             while (true) {
//                 try (Socket client = server.accept()) {
//                     System.out.println("Accepted: " + client.getRemoteSocketAddress());

//                     BufferedReader reader = new BufferedReader(
//                         new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
//                     );

//                     // Block here until the client sends at least the request line.
//                     String requestLine = reader.readLine();
//                     if (requestLine == null || requestLine.isEmpty()) {
//                         continue;
//                     }

//                     String line;
//                     while ((line = reader.readLine()) != null && !line.isEmpty()) {
//                         // Read headers until CRLF CRLF.
//                     }

//                     String[] parts = requestLine.split(" ");
//                     String method = parts.length > 0 ? parts[0] : "";
//                     String path = parts.length > 1 ? parts[1] : "";
//                     System.out.println("method=" + method + ", path=" + path);

//                     String status;
//                     String responseBody;
//                     if ("GET".equals(method) && "/hello".equals(path)) {
//                         status = "200 OK";
//                         responseBody = "Hello World";
//                     } else {
//                         status = "404 Not Found";
//                         responseBody = "Not Found";
//                     }

//                     byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
//                     String headers =
//                         "HTTP/1.1 " + status + "\r\n" +
//                         "Content-Type: text/plain; charset=UTF-8\r\n" +
//                         "Content-Length: " + bodyBytes.length + "\r\n" +
//                         "Connection: close\r\n" +
//                         "\r\n";

//                     OutputStream out = client.getOutputStream();
//                     out.write(headers.getBytes(StandardCharsets.UTF_8));
//                     out.write(bodyBytes);
//                     out.flush();
//                 } catch (IOException e) {
//                     System.err.println("Failed to handle client: " + e.getMessage());
//                 }
//             }
//         } catch (IOException e) {
//             System.err.println("Failed to start server: " + e.getMessage());
//         }
//     }
// }

public class Main {
    public static void main(String[] args) {
        int port = 8080;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server listening on port" + port);

            while (true) {
                try (Socket client = server.accept()) {
                    System.out.println("Accepted: " + client.getRemoteSocketAdress());

                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
                    );
                    
                    String requestLine = reader.readLine();
                    if (requestLine == null || requestLine == isEmpty) continue

                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        READ headers until CRLF CRLF.
                    }

                    String[] parts = requestLine.split(" ");
                    String method = parts.length > 0 ? parts[0] : "";
                    String path = parts.length > 1 ? parts[1] : "";
                    System.out.println("method" + method + ", path=" + path);

                    //                     String status;
//                     String responseBody;
//                     if ("GET".equals(method) && "/hello".equals(path)) {
//                         status = "200 OK";
//                         responseBody = "Hello World";
//                     } else {
//                         status = "404 Not Found";
//                         responseBody = "Not Found";
//                     }

//                     byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
//                     String headers =
//                         "HTTP/1.1 " + status + "\r\n" +
//                         "Content-Type: text/plain; charset=UTF-8\r\n" +
//                         "Content-Length: " + bodyBytes.length + "\r\n" +
//                         "Connection: close\r\n" +
//                         "\r\n";

//                     OutputStream out = client.getOutputStream();
//                     out.write(headers.getBytes(StandardCharsets.UTF_8));
//                     out.write(bodyBytes);
//                     out.flush();
                    String status;
                    String responseBody;

                    if ("GET".equals(method) && "/hello".equals(path)) {
                        status = "200 OK";
                        responseBody = "Hello World";
                    } else {
                        status = "404 Not Found";
                        responseBody = "404 Not Found";
                    }

                    byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                    String headers =
                        "HTTP/1.1 " + status + "\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n" +
                        "Connection: close\r\n" + "\r\n";

                    OutputStream out = getOutputStream();
                    out.write(headers.getBytes(StandardCharsets.UTF_8));
                    out.write(bodyBytes);
                    out.flush();
                } catch(IOException e) {
                    System.err.println("Failed to start server :" + e.getMessage());
                }
            }
        } catch(IOException e) {
            System.err.println("Failed to start server:" + e.getMessage());
        }
    }
}