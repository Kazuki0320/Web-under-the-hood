# フレームワークなしで作る最小APIサーバー入門（Java）

## はじめに

Java標準ライブラリだけで、フレームワークなしの最小APIサーバーを実装します。  
`GET /health` と `GET /hello` を実装し、`curl` で動作確認するところまでを扱います。

この記事のポイント:
- `ServerSocket` ベースでAPIサーバーを立ち上げる流れ
- リクエスト行を読んでルーティングする最小実装
- JSONレスポンスとHTTPステータスを返し分ける基本

---

## 前提環境

- OS: macOS
- Java: OpenJDK 17.0.17

---

## 今回作るもの

- `GET /health` -> `200 OK` + `{"status":"ok"}`
- `GET /hello` -> `200 OK` + `{"message":"hello"}`
- それ以外 -> `404 Not Found` + `{"error":"not found"}`
- 例外時 -> `500 Internal Server Error`

---

## 実装全体（`src/Main.java`）

```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("API server listening on http://127.0.0.1:" + PORT);

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

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
            }

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "";

            String status;
            String body;

            if ("GET".equals(method) && "/health".equals(path)) {
                status = "200 OK";
                body = "{\"status\":\"ok\"}";
            } else if ("GET".equals(method) && "/hello".equals(path)) {
                status = "200 OK";
                body = "{\"message\":\"hello\"}";
            } else {
                status = "404 Not Found";
                body = "{\"error\":\"not found\"}";
            }

            sendJson(client, status, body);
        } catch (Exception e) {
            sendJson(client, "500 Internal Server Error", "{\"error\":\"internal server error\"}");
            System.err.println("Request handling error: " + e.getMessage());
        }
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
}
```

---

## 実装の個別解説（使用箇所 -> 説明）

### 1. `ServerSocket`

使用箇所:
```java
try (ServerSocket server = new ServerSocket(PORT)) {
```

説明:
- サーバー側でTCP接続を受け付ける「待受ソケット」を作るクラスです。
- `new ServerSocket(PORT)` を実行すると、OSに「このポートはこのプロセスが使う」と登録されます（バインド）。
- バインド後はそのポートで接続待ち状態に入り、`accept()` で到着した接続を1件ずつ受け取れます。

ポートをバインドするとは:
- 「このポート番号はこのプロセスが使う」とOSに登録することです。
- `new ServerSocket(PORT)` の時点で、`127.0.0.1:8080` 宛の接続を受け取れる状態になります。
- 同じIP/ポートを別プロセスが使うと競合して起動失敗します。

### 2. `server.accept()`

使用箇所:
```java
while (true) {
    try (Socket client = server.accept()) {
        handleClient(client);
    }
}
```

説明:
- クライアントから接続が来るまで待つメソッドです（ブロッキング）。
- 接続が来ると、その通信専用の `Socket` を返します。
- 返ってきた `Socket` を使って、リクエスト読み取りとレスポンス返却を行います。

### 3. `BufferedReader`

使用箇所:
```java
BufferedReader reader = new BufferedReader(
    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
);
```

説明:
- 文字列を効率よく読むためのラッパーです。
- `readLine()` が使えるので、HTTPの1行単位処理に向いています。
- バッファを持つため、細かい読み取りを効率化できます。

### 4. `InputStreamReader`

使用箇所:
```java
new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
```

説明:
- ソケットから来るバイト列を文字列として読める形に変換するクラスです。
- ここで `UTF-8` を明示して、文字コードのズレを防いでいます。
- `BufferedReader` と組み合わせて使います。

### 5. `readLine()`

使用箇所:
```java
String requestLine = reader.readLine();

String line;
while ((line = reader.readLine()) != null && !line.isEmpty()) {
}
```

説明:
- 改行までを1行として読み取ります。
- 1回目はリクエスト行（例: `GET /hello HTTP/1.1`）を取得します。
- 2回目以降はヘッダー行を読み、空行でヘッダー終端を判定します。

---

## 実行手順

```bash
cd 10-build-api-server
javac src/Main.java
java -cp src Main
```

別ターミナルで確認:

```bash
curl -i http://127.0.0.1:8080/health
curl -i http://127.0.0.1:8080/hello
curl -i http://127.0.0.1:8080/not-found
```

期待結果:

- `/health` は `200 OK`
- `/hello` は `200 OK`
- `/not-found` は `404 Not Found`

---

## 実行結果

```text
## /health
HTTP/1.1 200 OK
{"status":"ok"}

## /hello
HTTP/1.1 200 OK
{"message":"hello"}

## /not-found
HTTP/1.1 404 Not Found
{"error":"not found"}
```

---

## まとめ

- `ServerSocket` / `accept()` / `readLine()` が、どの順で動くかをコードとログで対応づけて理解できる
- APIらしさは、ルーティング・JSONレスポンス・ステータスコードの返し分けで作られる
- 次は `POST` と入力バリデーション、DB接続を足すと実務に近づく
