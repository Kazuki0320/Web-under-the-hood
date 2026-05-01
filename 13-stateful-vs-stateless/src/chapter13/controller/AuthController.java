package chapter13.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import chapter13.service.CookieSessionAuthService;

public class AuthController {
    // TODO: HTTPリクエスト行・ヘッダー・本文を解析する。
    // TODO: パスに応じてCookie方式とJWT方式へルーティングする。
    // TODO: ステータス・ヘッダー・本文を含むHTTPレスポンスを組み立てる。
    // TODO: 未対応パスや不正リクエストを適切にハンドリングする。
    private final CookieSessionAuthService cookieSessionAuthService;

    public AuthController(CookieSessionAuthService cookieSessionAuthService) {
        this.cookieSessionAuthService = cookieSessionAuthService;
    }

    public void handle(Socket client) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
        );

        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            return;
        }
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            return;
        }
        String method = parts[0]; // "POST"
        String path = parts[1]; // "/cookie/login"

        String route = resolveRoute(method, path);
        if (route == null) {
            writeJsonResponse(client, 404, "{\"error\":\"not found\"}");
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
        return null;
    }

    private void writeJsonResponse(Socket client, int statusCode, String body) throws IOException {
        String reason = (statusCode == 200) ? "OK" : "Not Found";
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
}
