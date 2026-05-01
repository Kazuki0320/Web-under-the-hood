package chapter13.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

        String readLine = reader.readLine();
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
}
