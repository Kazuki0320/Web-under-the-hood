package chapter13.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import chapter13.model.User;
import chapter13.service.CookieSessionAuthService;
import chapter13.service.JwtAuthService;

public class AuthController {
    // TODO: HTTPリクエスト行・ヘッダー・本文を解析する。
    // TODO: パスに応じてCookie方式とJWT方式へルーティングする。
    // TODO: ステータス・ヘッダー・本文を含むHTTPレスポンスを組み立てる。
    // TODO: 未対応パスや不正リクエストを適切にハンドリングする。
    private final CookieSessionAuthService cookieSessionAuthService;
    private final JwtAuthService jwtAuthService;

    public AuthController(CookieSessionAuthService cookieSessionAuthService, JwtAuthService jwtAuthService) {
        this.cookieSessionAuthService = cookieSessionAuthService;
        this.jwtAuthService = jwtAuthService;
    }

    public void handle(Socket client) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
        );

        // 1) リクエスト行を読む（例: POST /cookie/login HTTP/1.1）
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            return;
        }

        // 2) method / path を取り出す
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            return;
        }
        String method = parts[0]; // "POST"
        String path = parts[1]; // "/cookie/login"

        // 3) ここから先でヘッダーを1行ずつ読んで Content-Length を取得する
        //    - 空行（""）が来るまでヘッダー
        //    - Content-Length があれば本文の長さとして保持
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

        // 4) Content-Length 分だけ本文を読む（JSON文字列）
        //    例: {"id":"demo","password":"password123"}
        int contentLength = parseIntOrZero(headers.get("content-length"));
        String requestBody = readBody(reader, contentLength);

        // 5) /cookie/login のとき本文から id/password を取り出す
        //    取り出した値を cookieSessionAuthService.login(id, password) に渡す
        // 7) それ以外のルートは今まで通り route 判定で 404/200 を返す
        String route = resolveRoute(method, path);
        if (route == null) {
            writeJsonResponse(client, 404, "{\"error\":\"not found\"}");
            return;
        }

        if ("cookie-login".equals(route)) {
            String id = extractJsonValue(requestBody, "id");
            String password = extractJsonValue(requestBody, "password");
            String sid = cookieSessionAuthService.login(id, password);
            if (sid == null) {
                writeJsonResponse(client, 401, "{\"error\":\"unauthorized\"}");
                return;
            }
            writeJsonResponseWithCookie(
                client,
                200,
                "{\"message\":\"login ok\"}",
                "sid=" + sid + "; Path=/; HttpOnly"
            );
            return;
        }

        if ("cookie-me".equals(route)) {
            String cookieHeader = headers.get("cookie");
            String sid = extractCookieValue(cookieHeader, "sid");
            User user = cookieSessionAuthService.me(sid);
            if (user == null) {
                writeJsonResponse(client, 401, "{\"error\":\"unauthorized\"}");
                return;
            }
            writeJsonResponse(
                client,
                200,
                "{\"id\":\"" + user.getId() + "\",\"name\":\"" + user.getName() + "\"}"
            );
            return;
        }

        if ("jwt-login".equals(route)) {
            String id = extractJsonValue(requestBody, "id");
            String password = extractJsonValue(requestBody, "password");
            String token = jwtAuthService.login(id, password);
            if (token == null) {
                writeJsonResponse(client, 401, "{\"error\":\"unauthorized\"}");
                return;
            }
            writeJsonResponse(client, 200, "{\"token\":\"" + token + "\"}");
            return;
        }

        if ("jwt-me".equals(route)) {
            String authorization = headers.get("authorization");
            String token = extractBearerToken(authorization);
            User user = jwtAuthService.me(token);
            if (user == null) {
                writeJsonResponse(client, 401, "{\"error\":\"unauthorized\"}");
                return;
            }
            writeJsonResponse(
                client,
                200,
                "{\"id\":\"" + user.getId() + "\",\"name\":\"" + user.getName() + "\"}"
            );
            return;
        }

        writeJsonResponse(client, 200, "{\"route\":\"" + route + "\"}");
    }

    public String resolveRoute(String method, String path) {
        if ("POST".equals(method) && "/cookie/login".equals(path)) {
            
            return "cookie-login";
        }
        if ("GET".equals(method) && "/cookie/me".equals(path)) {
            return "cookie-me";
        }
        if ("POST".equals(method) && "/jwt/login".equals(path)) {
            return "jwt-login";
        }
        if ("GET".equals(method) && "/jwt/me".equals(path)) {
            return "jwt-me";
        }
        return null;
    }

    private void writeJsonResponse(Socket client, int statusCode, String body) throws IOException {
        String reason = reasonPhrase(statusCode);
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String headers =
            "HTTP/1.1 " + statusCode + " " + reason + "\r\n" +
            "Content-Type: application/json; charset=utf-8\r\n" +
            "Content-Length: " + bodyBytes.length + "\r\n" +
            "Connection: close\r\n" +
            "\r\n";

        OutputStream out = client.getOutputStream();
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    private void writeJsonResponseWithCookie(Socket client, int statusCode, String body, String setCookie)
        throws IOException {
        String reason = reasonPhrase(statusCode);
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String headers =
            "HTTP/1.1 " + statusCode + " " + reason + "\r\n" +
            "Content-Type: application/json; charset=utf-8\r\n" +
            "Content-Length: " + bodyBytes.length + "\r\n" +
            "Set-Cookie: " + setCookie + "\r\n" +
            "Connection: close\r\n" +
            "\r\n";

        OutputStream out = client.getOutputStream();
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    private String extractJsonValue(String json, String key) {
    if (json == null || key == null) {
        return null;
    }

    String pattern = "\"" + key + "\"";
    int keyIndex = json.indexOf(pattern);
    if (keyIndex < 0) {
        return null;
    }

    int colonIndex = json.indexOf(':', keyIndex + pattern.length());
    if (colonIndex < 0) {
        return null;
    }

    int firstQuote = json.indexOf('"', colonIndex + 1);
    if (firstQuote < 0) {
        return null;
    }

    int secondQuote = json.indexOf('"', firstQuote + 1);
    if (secondQuote < 0) {
        return null;
    }

    return json.substring(firstQuote + 1, secondQuote);
}

    private String extractCookieValue(String cookieHeader, String key) {
        if (cookieHeader == null || key == null) {
            return null;
        }
        String[] pairs = cookieHeader.split(";");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            String prefix = key + "=";
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length());
            }
        }
        return null;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            return null;
        }
        return authorizationHeader.substring(prefix.length()).trim();
    }

    private String reasonPhrase(int statusCode) {
        if (statusCode == 200) {
            return "OK";
        }
        if (statusCode == 401) {
            return "Unauthorized";
        }
        if (statusCode == 404) {
            return "Not Found";
        }
        return "Internal Server Error";
    }

    private int parseIntOrZero(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String readBody(BufferedReader reader, int contentLength) throws IOException {
        if (contentLength <= 0) {
            return "";
        }
        char[] buffer = new char[contentLength];
        int total = 0;
        while (total < contentLength) {
            int read = reader.read(buffer, total, contentLength - total);
            if (read < 0) {
                break;
            }
            total += read;
        }
        return new String(buffer, 0, total);
    }

}
