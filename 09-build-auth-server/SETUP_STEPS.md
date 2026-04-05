# 自作認証サーバー 手順書（09）

このファイルは、`09-build-auth-server` を順番に進めるための実行手順です。  
各ステップで「やること」と「完了条件」を分けています。

## Step 0: 事前確認

やること:
- Java実行環境と`curl`コマンドが使えることを確認する
- 作業ディレクトリを`09-build-auth-server`に合わせる

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/09-build-auth-server
java -version
javac -version
which curl
```

完了条件:
- `java` / `javac` / `curl` が利用可能
- `09-build-auth-server` 配下で作業開始できる

---

## Step 1: 最小構成のファイルを準備する

やること:
- `src` ディレクトリを作成する
- 実装ファイル `src/Main.java` を用意する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/09-build-auth-server
mkdir -p src
touch src/Main.java
```

完了条件:
- `src/Main.java` が作成されている

---

## Step 2: 認証サーバーを実装する

やること:
- `POST /login` でID/PWを受け取り、トークンを発行する
- `GET /me` でBearerトークンを検証する
- 未認証・不正トークン時は `401 Unauthorized` を返す

補足:
- フレームワークは使わない
- トークン管理はメモリ内（学習用）
- ポートは `8081` を使用（既存APIサーバーとの競合を避けるため）

実装手順（詳細）:
1. `ServerSocket` で待受ループを作る
2. `readLine()` でリクエスト行・ヘッダー・本文を読む
3. `POST /login` でID/PWを検証し、トークンを生成する
4. `GET /me` で `Authorization: Bearer ...` を検証する
5. 共通関数 `sendJson` でJSONレスポンスを返す

`src/Main.java`（最小実装例）:
```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final int PORT = 8081;
    private static final String DEMO_ID = "demo";
    private static final String DEMO_PASSWORD = "password123";
    private static final long TOKEN_TTL_SECONDS = 3600;

    private static final Map<String, Session> SESSIONS = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Auth server listening on http://127.0.0.1:" + PORT);

            while (true) {
                try (Socket client = server.accept()) {
                    handleClient(client);
                } catch (Exception e) {
                    System.err.println("Failed to handle client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private static void handleClient(Socket client) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
            );

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

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

            int contentLength = parseIntOrZero(headers.get("content-length"));
            String requestBody = readBody(reader, contentLength);

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "";

            if ("POST".equals(method) && "/login".equals(path)) {
                handleLogin(client, headers, requestBody);
                return;
            }

            if ("GET".equals(method) && "/me".equals(path)) {
                handleMe(client, headers);
                return;
            }

            sendJson(client, "404 Not Found", "{\"error\":\"not found\"}");
        } catch (Exception e) {
            sendJson(client, "500 Internal Server Error", "{\"error\":\"internal server error\"}");
            System.err.println("Request handling error: " + e.getMessage());
        }
    }

    private static void handleLogin(Socket client, Map<String, String> headers, String requestBody) throws IOException {
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

        if (!DEMO_ID.equals(id) || !DEMO_PASSWORD.equals(password)) {
            sendJson(client, "401 Unauthorized", "{\"error\":\"invalid credentials\"}");
            return;
        }

        String token = UUID.randomUUID().toString();
        long expiresAt = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        SESSIONS.put(token, new Session(id, expiresAt));

        String body = "{\"accessToken\":\"" + token + "\",\"tokenType\":\"Bearer\",\"expiresIn\":" + TOKEN_TTL_SECONDS + "}";
        sendJson(client, "200 OK", body);
    }

    private static void handleMe(Socket client, Map<String, String> headers) throws IOException {
        String auth = headers.getOrDefault("authorization", "");
        if (!auth.startsWith("Bearer ")) {
            sendJson(client, "401 Unauthorized", "{\"error\":\"missing bearer token\"}");
            return;
        }

        String token = auth.substring("Bearer ".length()).trim();
        Session session = SESSIONS.get(token);
        if (session == null) {
            sendJson(client, "401 Unauthorized", "{\"error\":\"invalid token\"}");
            return;
        }

        long now = Instant.now().getEpochSecond();
        if (now >= session.expiresAt) {
            SESSIONS.remove(token);
            sendJson(client, "401 Unauthorized", "{\"error\":\"token expired\"}");
            return;
        }

        String body = "{\"id\":\"" + session.userId + "\",\"name\":\"Demo User\"}";
        sendJson(client, "200 OK", body);
    }

    private static String readBody(BufferedReader reader, int contentLength) throws IOException {
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

    private static int parseIntOrZero(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher m = p.matcher(json == null ? "" : json);
        return m.find() ? m.group(1) : null;
    }

    private static void sendJson(Socket client, String status, String body) throws IOException {
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

    private static class Session {
        private final String userId;
        private final long expiresAt;

        private Session(String userId, long expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }
}
```

完了条件:
- `POST /login` と `GET /me` の最小実装が入っている
- `401 Unauthorized` の失敗系を返せる

---

## Step 3: コンパイルしてサーバーを起動する

やること:
- Javaコードをコンパイルして起動する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/09-build-auth-server
javac src/Main.java
java -cp src Main
```

完了条件:
- サーバーが待受状態で起動している
- `127.0.0.1:8081` で接続を受けられる

---

## Step 4: 正常系を確認する

やること:
- `POST /login` でトークンを発行する
- 発行したトークンで `GET /me` を呼ぶ

コマンド例:
```bash
# ログイン（成功）
curl -i -X POST http://127.0.0.1:8081/login \
  -H "Content-Type: application/json" \
  -d '{"id":"demo","password":"password123"}'

# 返ってきたaccessTokenを貼り付け
curl -i http://127.0.0.1:8081/me \
  -H "Authorization: Bearer <accessToken>"
```

完了条件:
- `/login` で `200 OK` と `accessToken` を取得できる
- `/me` で `200 OK` とユーザー情報を取得できる

---

## Step 5: 失敗系を確認する

やること:
- 認証失敗パターンを確認する

コマンド例:
```bash
# パスワード不一致
curl -i -X POST http://127.0.0.1:8081/login \
  -H "Content-Type: application/json" \
  -d '{"id":"demo","password":"wrong"}'

# トークンなし
curl -i http://127.0.0.1:8081/me

# 不正トークン
curl -i http://127.0.0.1:8081/me \
  -H "Authorization: Bearer invalid-token"
```

完了条件:
- 失敗時に `401 Unauthorized` を確認できる
- どの失敗で何が返るか説明できる

---

## Step 6: 検証ログを残す

やること:
- 成功系と失敗系の結果を1ファイルにまとめる

コマンド例:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/09-build-auth-server
{
  echo "# 09 Auth Server Test Log";
  date;
  echo;
  echo "## login success";
  curl -i -X POST http://127.0.0.1:8081/login \
    -H "Content-Type: application/json" \
    -d '{"id":"demo","password":"password123"}';
  echo;
  echo "## login failure";
  curl -i -X POST http://127.0.0.1:8081/login \
    -H "Content-Type: application/json" \
    -d '{"id":"demo","password":"wrong"}';
  echo;
  echo "## /me without token";
  curl -i http://127.0.0.1:8081/me;
} > AUTH_SERVER_TEST_LOG.md
```

完了条件:
- `AUTH_SERVER_TEST_LOG.md` が作成されている
- 成功系と失敗系の両方が記録されている

---

## 最終チェック

- `POST /login` でトークンを発行できる
- `GET /me` でトークン検証できる
- 未認証/不正トークン時に `401` を返せる
- 検証ログを残して再現可能な状態になっている
