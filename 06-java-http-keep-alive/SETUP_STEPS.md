# Java HTTP Keep-Alive 手順書（06）

このファイルは、`06-java-http-keep-alive` を順番に進めるための実行手順です。  
各ステップで「やること」と「完了条件」を分けています。

## Step 1: 作業ファイルを準備する

やること:
- `src/Main.java` を学習用に編集できる状態にする
- TODOを見ながら、実装方針（ループ/終了条件）を先に決める

例:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/06-java-http-keep-alive
ls -la src
```

完了条件:
- `src/Main.java` が存在する
- TODOの実装対象を把握している

---

## Step 2: まずは最小応答（1接続1リクエスト）を通す

やること:
- `ServerSocket(8080)` で待受
- `accept()` 後に固定レスポンスを返して一旦 close
- `javac` と `curl -v` で基本疎通を確認

実装例（`src/Main.java`）:
```java
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        String body = "Hello World";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String headers =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/plain; charset=UTF-8\r\n" +
            "Content-Length: " + bodyBytes.length + "\r\n" +
            "Connection: close\r\n" +
            "\r\n";
        byte[] headerBytes = headers.getBytes(StandardCharsets.UTF_8);

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                try (Socket client = server.accept()) {
                    System.out.println("Accepted: " + client.getRemoteSocketAddress());

                    OutputStream out = client.getOutputStream();
                    out.write(headerBytes);
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
```

確認コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/06-java-http-keep-alive
javac src/Main.java
java -cp src Main
```

別ターミナル:
```bash
curl -v http://localhost:8080/
```

完了条件:
- `HTTP/1.1 200 OK` が返る
- サーバーが接続を受けて応答できる

---

## Step 3: keep-alive用の接続ループを追加する

やること:
- 1接続内で複数回リクエストを読むループを作る
- `Connection: close` が来たらループ終了にする
- レスポンスの `Connection` ヘッダーを `keep-alive/close` で切り替える

実装ポイント:
- `handleConnection(...)` 内に「接続単位のwhileループ」を置く
- 終了条件を boolean で明示して分かりやすく管理する

実装例（Step 3 最小keep-alive版）:
```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                try (Socket client = server.accept()) {
                    handleConnection(client);
                } catch (IOException e) {
                    System.err.println("Failed to handle client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private static void handleConnection(Socket client) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
        );
        OutputStream out = client.getOutputStream();

        while (true) {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                break;
            }

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "";

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

            boolean closeConnection = "close".equalsIgnoreCase(headers.getOrDefault("connection", ""));
            String status;
            String body;
            if ("GET".equals(method) && "/hello".equals(path)) {
                status = "200 OK";
                body = "Hello, World";
            } else {
                status = "404 Not Found";
                body = "Not Found";
            }

            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            String responseHeaders =
                "HTTP/1.1 " + status + "\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: " + (closeConnection ? "close" : "keep-alive") + "\r\n" +
                "\r\n";

            out.write(responseHeaders.getBytes(StandardCharsets.UTF_8));
            out.write(bodyBytes);
            out.flush();

            if (closeConnection) {
                break;
            }
        }
    }
}
```

完了条件:
- 同一接続で連続リクエストを処理できる実装になっている

直後の動作確認コマンド（今回実施分）:


`curl`コマンドでの確認:
```bash
# keep-alive再利用確認（2リクエスト）
curl -v http://localhost:8080/ http://localhost:8080/hello

# close指定
curl -v -H 'Connection: close' http://localhost:8080/hello
```

別ターミナル（`nc`）:
```bash
# 手動入力
nc 127.0.0.1 8080
```

`nc` 入力内容（最後に空行）:
```http
GET /hello HTTP/1.1
Host: localhost:8080
Connection: close

```

`nc` を1コマンドで送る場合:
```bash
printf 'GET /hello HTTP/1.1\r\nHost: localhost:8080\r\nConnection: close\r\n\r\n' | nc 127.0.0.1 8080
```

ログの読み解き（`curl -v http://localhost:8080/ http://localhost:8080/hello`）:

- `* Connected to localhost ...`
  - TCP接続が確立された（この時点ではHTTP本文はまだ未送信）。
- `> GET / HTTP/1.1`
  - 1回目のHTTPリクエスト送信（パスは `/`）。
- `< HTTP/1.1 404 Not Found`
  - 1回目のHTTPレスポンス（`/` は未実装なので404）。
- `< Connection: keep-alive`
  - サーバーが「接続を維持する」方針を返している。
- `* Connection #0 to host localhost left intact`
  - 接続#0を閉じずに保持した（次リクエストに再利用可能）。
- `* Re-using existing connection with host localhost`
  - 新規接続せず、同じ接続#0を再利用した（keep-alive成立の重要サイン）。
- `> GET /hello HTTP/1.1`
  - 2回目のHTTPリクエスト送信（同一TCP接続上）。
- `< HTTP/1.1 200 OK`
  - 2回目のHTTPレスポンス（`/hello` は成功）。
- `* Connection #0 to host localhost left intact`
  - 2回目応答後も接続#0を保持して終了した。

今回の区切り:
- TCP接続は1本（Connection #0）
- HTTPリクエスト/レスポンスは2往復（`/` と `/hello`）

---

## Step 4: 安全な終了条件を追加する

やること:
- `Socket#setSoTimeout(...)` でアイドルタイムアウトを設定
- `MAX_REQUESTS_PER_CONNECTION` を設けて上限管理
- 本文付きリクエストの読み残しを防ぐ（`Content-Length` 分を消費）

今回の「部分追記」（先にここまで）:
- `IDLE_TIMEOUT_MS` を定数追加し、`client.setSoTimeout(...)` を設定
- `MAX_REQUESTS_PER_CONNECTION` を定数追加し、接続内ループの上限に使用

`Main.java` 追記例（Step 4 部分）:
```java
private static final int IDLE_TIMEOUT_MS = 5000;
private static final int MAX_REQUESTS_PER_CONNECTION = 5;

private static void handleConnection(Socket client) throws IOException {
    client.setSoTimeout(IDLE_TIMEOUT_MS);
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
    );
    OutputStream out = client.getOutputStream();
    int requestCount = 0;

    while (requestCount < MAX_REQUESTS_PER_CONNECTION) {
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            break;
        }
        requestCount++;

        // 既存の method/path 解析
        // 既存のレスポンス返却
    }
}
```

完了条件:
- timeout・最大件数・明示closeの3条件で終了できる
- 次リクエストの境界崩れが起きにくい実装になっている

---

## Step 5: 再利用できているかを観測する

やること:
- `curl -v` で同一接続再利用のログを確認する
- `--http1.1` / `--http1.0` / `Connection: Keep-Alive` で挙動を比較する

確認コマンド例:
```bash
curl -v http://localhost:8080/ http://localhost:8080/hello
```

見るポイント:
- 再利用: `Re-using existing connection!`
- 切断: `Closing connection ...`

完了条件:
- どの条件で再利用/切断されるか説明できる

補足（切断主導の確認）:
- クライアント主導で切る確認:
  - `curl -v -H 'Connection: close' http://localhost:8080/`
- サーバー主導で切る確認:
  - `IDLE_TIMEOUT_MS` を短めにして、1回目の後に待ってから2回目を送る
  - 例: 「1回目送信 -> timeout以上待つ -> 2回目送信」で新規接続になるか確認

アイドルタイムアウト確認:

リクエスト内容（同一接続で2回送る。間に6秒待つ）:
```http
GET /hello HTTP/1.1
Host: localhost:8080

[6秒待つ]
GET /hello HTTP/1.1
Host: localhost:8080
Connection: close

```

実行コマンド:
```bash
{
  printf 'GET /hello HTTP/1.1\r\nHost: localhost:8080\r\n\r\n'
  sleep 6
  printf 'GET /hello HTTP/1.1\r\nHost: localhost:8080\r\nConnection: close\r\n\r\n'
} | nc 127.0.0.1 8080
```

コマンドの意味（初見向け）:
- `{ ... }` は「オブジェクト」ではなく、シェルの**コマンドグループ**。
- グループ内のコマンドを上から順に実行し、出力をまとめて右側へ渡す。
- 1つ目の `printf` で1回目リクエスト送信。
- `sleep 6` で6秒待機。
- 2つ目の `printf` で2回目リクエスト送信。
- `| nc 127.0.0.1 8080` で、まとめた出力を同一 `nc` 接続に流し込む。
- 目的は「同じTCP接続で、時間を空けた2回のHTTPリクエスト」を再現すること。

返り値例:
```http
HTTP/1.1 200 OK
Content-Type: text/plain; charset=UTF-8
Content-Length: 12
Connection: keep-alive

Hello, WorldHTTP/1.1 200 OK
Content-Type: text/plain; charset=UTF-8
Content-Length: 12
Connection: close

Hello, World
```

結果の見方:
- 期待（IDLE_TIMEOUT_MS=5000 の場合）:
  - 1回目の `HTTP/1.1 200 OK` は返る
  - 待機6秒でサーバー側が接続を回収し、同一接続の2回目は通らない（または再接続扱いになる）
- サーバーログ側:
  - timeout 由来の切断（例: read timed out）が確認できればOK
- 補足:
  - サーバーのtimeout設定が長い場合は、返り値例のように2回目も成功する
- これが確認できれば「idle timeout到達で切断される」を説明できるので Step 5 完了
