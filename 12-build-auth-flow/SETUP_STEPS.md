# 認証フロー実装 手順書（12）

このファイルは、`12-build-auth-flow` を「設計先行」で進めるためのセットアップ手順です。  
今回は実装そのものよりも、認証フロー設計と学習ポイント整理を先に固めます。

## Step 0: 事前確認

やること:
- Java実行環境と`curl`が使えることを確認する
- 作業ディレクトリを `12-build-auth-flow` に合わせる

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/12-build-auth-flow
java -version
javac -version
which curl
```

完了条件:
- `java` / `javac` / `curl` が利用可能
- `12-build-auth-flow` 配下で作業開始できる

---

## Step 1: 今回の学習ゴールを固定する

やること:
- 今回の「到達目標」と「対象外」を先に決める
- 設計判断の優先順位を決める（動くこと > 説明できること > 拡張性）

この章の到達目標:
1. `POST /login` で認証成功時にトークン発行できる
2. `GET /me` でBearerトークン検証ができる
3. `401` の失敗理由（未送信/不正/期限切れ）を区別して説明できる
4. `Controller -> Service -> Repository` の責務分離を説明できる

今回の対象外（割り切る）:
- OAuth/OIDC準拠の完全実装
- 本番向けセキュリティ（署名付きJWT、鍵管理、リフレッシュトークン）
- 分散環境向けセッション共有

完了条件:
- 「何を学ぶか / 何をやらないか」を言語化できる

---

## Step 2: 設計を先に確定する（責務分離）

やること:
- レイヤ責務を明文化する
- 各レイヤ間の依存方向を固定する

推奨レイヤ構成:
- `Main`: サーバー起動と依存組み立て
- `Controller`: HTTP入出力（ヘッダー、JSON、ステータス）
- `Service`: 認証ロジック（資格情報確認、トークン発行/検証）
- `Repository`: データアクセス（ユーザー参照、トークン保存/参照）
- `Model`: `User` / `Session(Token)` などのドメイン表現

依存ルール:
- `Controller -> Service -> Repository`
- 下位レイヤは上位レイヤを知らない

完了条件:
- 各クラスの責務を1行で説明できる
- 依存方向を逆転させない方針が決まっている

---

## Step 3: API契約（I/F）を固定する

やること:
- 先にリクエスト/レスポンス形式を決める
- 正常系と異常系のHTTPステータスを固定する

API契約（最小）:
1. `POST /login`
   - 入力: `{"id":"...","password":"..."}`
   - 成功: `200` + `{"accessToken":"...","tokenType":"Bearer","expiresIn":...}`
   - 失敗: `400`（入力不備）/ `401`（認証失敗）/ `415`（Content-Type不正）
2. `GET /me`
   - 入力: `Authorization: Bearer <token>`
   - 成功: `200` + `{"id":"...","name":"..."}`
   - 失敗: `401`（未送信/不正/期限切れ）

完了条件:
- 各エンドポイントで「何を受けて何を返すか」が確定している

---

## Step 4: トークンライフサイクルを設計する

やること:
- トークンの状態遷移を決める
- 有効期限判定のタイミングを決める

最小ルール:
1. `login` 成功時にランダムトークン発行
2. `token -> userId + expiresAt` を保存
3. `GET /me` で `存在確認 -> 期限確認` の順で検証
4. 期限切れは `401` を返し、必要なら保存領域から削除

完了条件:
- トークンが「生成 -> 保存 -> 検証 -> 失効」する流れを説明できる

---

## Step 5: 実装前の最小ファイルだけ準備する

やること:
- 実装の土台だけ作る（中身はこれから自分で書く）
- フォルダ作成時に、質問メモ（QAメモ）も同時に作る

コマンド例:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/12-build-auth-flow
mkdir -p src/controller src/service src/repository src/model src/store
touch QA_MEMO_AUTH_FLOW.md
touch src/Main.java
touch src/controller/AuthController.java
touch src/service/AuthService.java
touch src/repository/UserRepository.java
touch src/store/TokenStore.java
touch src/model/User.java
touch src/model/Session.java
```

完了条件:
- レイヤごとの最小ファイルが揃っている
- `QA_MEMO_AUTH_FLOW.md` が作成されている
- 実装開始前に迷わない骨組みができている

---

## Step 6: まず `Main` に必要な実装を入れる

やること:
- `Main` を「サーバー起動専任」にする（ビジネスロジックは書かない）
- `ServerSocket` 待受ループと例外ハンドリングだけ先に作る
- `AuthController` 呼び出しの入口を作る（中身実装は後でOK）

`Main` で最低限必要なimport:
- `java.io.IOException`
- `java.net.ServerSocket`
- `java.net.Socket`

`Main` の実装チェックリスト:
1. `private static final int PORT = 8080;` を定数化する
2. `try (ServerSocket server = new ServerSocket(PORT))` で待受開始する
3. 起動ログを出す（例: `Server listening on port 8080`）
4. `while (true)` で `accept()` ループを作る
5. 各接続を `handleClient(Socket client, AuthController controller)` に委譲する
6. 接続処理失敗時に `System.err` へログを出す
7. 起動失敗時に `Failed to start server: ...` を出す

推奨メソッド構成（`Main` 内）:
- `main(String[] args)`: 起動・依存組み立て・acceptループ
- `createAuthController()`: `Repository -> Service -> Controller` を組み立てる
- `handleClient(Socket client, AuthController controller)`: 1リクエスト処理（詳細はControllerへ委譲）

`src/Main.java`（見本実装）:
```java
import controller.AuthController;
import repository.UserRepository;
import service.AuthService;
import store.TokenStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
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
        TokenStore tokenStore = new TokenStore();
        AuthService authService = new AuthService(userRepository, tokenStore);
        return new AuthController(authService);
    }

    private static void handleClient(Socket client, AuthController controller) throws IOException {
        controller.handle(client);
    }
}
```

補足（この見本を動かすための最小シグネチャ）:
- `AuthController(AuthService authService)`
- `void handle(Socket client) throws IOException`
- `AuthService(UserRepository userRepository, TokenStore tokenStore)`

重要:
- Step 5 直後（クラス雛形だけ作った段階）は、`Main` の見本実装とクラス定義が不一致なためコンパイルが通らないのが正常。
- Step 6 で上記の最小シグネチャを追加してから、コンパイル確認へ進む。

完了条件:
- `Main` だけで「起動できる」「接続を受け取れる」「Controllerへ渡せる」状態になっている
- `Main` に認証ロジックを直接書いていない

---

## Step 7: `Main` が呼ぶ最小シグネチャを実装する

やること:
- `Main` から呼ばれているコンストラクタ/メソッドだけを先に実装する
- ここではロジック本体は書かず、まずコンパイルを通すことを目的にする

実装対象:
1. `AuthService(UserRepository, TokenStore)` コンストラクタ
2. `AuthController(AuthService)` コンストラクタ
3. `AuthController#handle(Socket)` メソッド

`src/service/AuthService.java`（見本実装）:
```java
package service;

import repository.UserRepository;
import store.TokenStore;

public class AuthService {
    private final UserRepository userRepository;
    private final TokenStore tokenStore;

    public AuthService(UserRepository userRepository, TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
    }
}
```

`src/controller/AuthController.java`（見本実装）:
```java
package controller;

import service.AuthService;

import java.io.IOException;
import java.net.Socket;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void handle(Socket client) throws IOException {
        // Step 8 で HTTP 解析とルーティングを実装する
    }
}
```

コンパイル確認コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/12-build-auth-flow
javac -cp src $(find src -name "*.java")
```

完了条件:
- `Main` / `AuthController` / `AuthService` の依存不整合エラーが解消している
- `javac` が成功する
- 実処理（/login, /me）はまだ未実装である

---

## Step 8: `AuthController` にHTTP解析と最小ルーティングを実装する

やること:
- `Socket` からHTTPリクエスト行/ヘッダー/本文を読む
- まずは `POST /login` と `GET /me` の入口だけ作る
- このStepではService呼び出しの枠だけ整え、詳細ロジックは次Stepへ回す

実装方針（最小）:
1. `requestLine` を読む（空なら return）
2. ヘッダーを `Map<String, String>` に取り込む
3. `Content-Length` を見て本文を読む
4. `method/path` でルーティングする
5. `/login` と `/me` は仮レスポンス、その他は `404`
6. 例外時は `500` を返す

`src/controller/AuthController.java`（見本実装）:
```java
package controller;

import service.AuthService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void handle(Socket client) throws IOException {
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
        }
    }

    private void handleLogin(Socket client, Map<String, String> headers, String requestBody) throws IOException {
        sendJson(client, "501 Not Implemented", "{\"error\":\"login not implemented\"}");
    }

    private void handleMe(Socket client, Map<String, String> headers) throws IOException {
        sendJson(client, "501 Not Implemented", "{\"error\":\"me not implemented\"}");
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
```

動作確認（このStepの期待）:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/12-build-auth-flow
javac -cp src $(find src -name "*.java")
java -cp src Main

# 別ターミナル
curl -i -X POST http://127.0.0.1:8080/login
curl -i http://127.0.0.1:8080/me
curl -i http://127.0.0.1:8080/not-found
```

期待結果:
- `/login`: `501 Not Implemented`
- `/me`: `501 Not Implemented`
- `/not-found`: `404 Not Found`

完了条件:
- `AuthController` 内でHTTP受信とルーティングができている
- ルートごとの最低限レスポンスを返せる
- Serviceの詳細実装を入れる準備が整っている

---

## Step 9: 認証本体（login/me）を実装する

やること:
- `AuthService` に認証とトークン検証の本体ロジックを追加する
- `TokenStore` に `save/find/remove` を実装する
- `AuthController` の `handleLogin` / `handleMe` で Service を呼び出す

実装順:
1. `TokenStore` を実装（メモリMap）
2. `AuthService` に `login` / `me` を実装
3. `AuthController` から Service 呼び出しへ差し替え

`src/store/TokenStore.java`（見本実装）:
```java
package store;

import model.Session;

import java.util.HashMap;
import java.util.Map;

public class TokenStore {
    private final Map<String, Session> sessions = new HashMap<>();

    public void save(String token, Session session) {
        sessions.put(token, session);
    }

    public Session find(String token) {
        return sessions.get(token);
    }

    public void remove(String token) {
        sessions.remove(token);
    }
}
```

`src/service/AuthService.java`（見本実装）:
```java
package service;

import model.Session;
import repository.UserRepository;
import store.TokenStore;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AuthService {
    private static final long TOKEN_TTL_SECONDS = 3600;
    private final UserRepository userRepository;
    private final TokenStore tokenStore;

    public AuthService(UserRepository userRepository, TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
    }

    public String login(String id, String password) {
        if (!"demo".equals(id) || !"password123".equals(password)) {
            return null;
        }
        String token = UUID.randomUUID().toString();
        long expiresAt = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        tokenStore.save(token, new Session(id, expiresAt));
        return token;
    }

    public String me(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        Session session = tokenStore.find(token);
        if (session == null) {
            return null;
        }
        long now = Instant.now().getEpochSecond();
        if (now >= session.getExpiresAt()) {
            tokenStore.remove(token);
            return null;
        }
        return session.getUserId();
    }
}
```

`src/controller/AuthController.java` の差し替えポイント:
- `handleLogin`: `id/password` 抽出 -> `authService.login(...)` -> 成功200/失敗401
- `handleMe`: `authorization` ヘッダー -> `authService.me(...)` -> 成功200/失敗401

コンパイル/動作確認:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/12-build-auth-flow
javac -cp src $(find src -name "*.java")
java -cp src Main

# 別ターミナル
curl -i -X POST http://127.0.0.1:8080/login \
  -H "Content-Type: application/json" \
  -d '{"id":"demo","password":"password123"}'

curl -i http://127.0.0.1:8080/me -H "Authorization: Bearer <TOKEN>"
```

完了条件:
- `/login` 正常時に `200` と `accessToken` を返せる
- `/me` で有効トークンは `200`、不正/期限切れは `401` になる


