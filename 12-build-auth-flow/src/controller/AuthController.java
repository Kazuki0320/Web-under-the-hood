package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import service.AuthService;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void handle(Socket client) throws IOException {
        try {
            // ソケット入力をUTF-8の文字として読み取る準備
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
            );

            // リクエスト行（例: GET /me HTTP/1.1）を読む
            String requestLine = reader.readLine();
            // 何も読めない場合は処理対象がないので終了
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            // HTTPヘッダーを key-value で保持する
            Map<String, String> headers = new HashMap<>();
            String line;
            // 空行までヘッダー行を1行ずつ読む（空行はヘッダー終端）
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // "key: value" の区切り位置を探す
                int idx = line.indexOf(':');
                if (idx > 0) {
                    // キーを小文字化して保存すると後で比較しやすい
                    String key = line.substring(0, idx).trim().toLowerCase();
                    String value = line.substring(idx + 1).trim();
                    headers.put(key, value);
                }
            }

            // Content-Length ヘッダーから本文サイズを取得
            int contentLength = parseIntOrZero(headers.get("content-length"));
            // 本文を必要な文字数だけ読み取る
            String requestBody = readBody(reader, contentLength);

            // リクエスト行を分解してメソッドとパスを取り出す
            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "";

            // POST /login はログイン処理へ
            if ("POST".equals(method) && "/login".equals(path)) {
                handleLogin(client, headers, requestBody);
                return;
            }

            // GET /me はユーザー情報取得処理へ
            if ("GET".equals(method) && "/me".equals(path)) {
                handleMe(client, headers, requestBody);
                return;
            }

            // 定義していないパスは 404 を返す
            sendJson(client, "404 Not Found", "{\"error\":\"not found\"}");
        } catch (Exception e) {
            sendJson(client, "500 Internal Server Error", "{\"error\":\"internal server error\"}");
        }
    }

    private void handleLogin(Socket client, Map<String, String> headers, String requestBody) throws IOException {
        String contentType = headers.getOrDefault("content-type", "");
        if (!contentType.startsWith("application/json")) {
            sendJson(client, "415 Unsupported Media Type", "{\"error\":\"content-type must be application/json\"}");
            return;
        }

        String id = extractJsonString(requestBody, "id");
        String password = extractJsonString(requestBody, "password");
        if (isBlank(id) || isBlank(password)) {
            sendJson(client, "400 Bad Request", "{\"error\":\"id and password are required\"}");
            return;
        }

        String token = authService.login(id, password);
        if (token == null) {
            sendJson(client, "401 Unauthorized", "{\"error\":\"invalid credentials\"}");
            return;
        }

        sendJson(
            client,
            "200 OK",
            "{\"accessToken\":\"" + token + "\",\"tokenType\":\"Bearer\",\"expiresIn\":3600}"
        );
    }

    private void handleMe(Socket client, Map<String, String> headers, String requestBody) throws IOException {
        String authorization = headers.get("authorization");
        String userId = authService.me(authorization);
        if (userId == null) {
            sendJson(client, "401 Unauthorized", "{\"error\":\"unauthorized\"}");
            return;
        }
        sendJson(client, "200 OK", "{\"id\":\"" + userId + "\",\"name\":\"Demo User\"}");
    }

    private String extractJsonString(String json, String key) {
        if (json == null) {
            return null;
        }
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int parseIntOrZero(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String readBody(BufferedReader reader, int contentLength) throws IOException {
        if (contentLength <= 0) {
            return "";
        }

        char[] chars = new char[contentLength];
        int read = 0;
        while (read < contentLength) {
            int n = reader.read(chars, read, contentLength - read);
            if (n < 0) {
                break;
            }
            read += n;
        }
        return new String(chars, 0, read);
    }

    private void sendJson(Socket client, String status, String body) throws IOException {
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
