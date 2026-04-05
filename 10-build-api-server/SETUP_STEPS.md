# 自作APIサーバー 手順書（10）

このファイルは、`10-build-api-server` を順番に進めるための実行手順です。  
各ステップで「やること」と「完了条件」を分けています。

## Step 0: 事前確認

やること:
- Java実行環境と`curl`コマンドが使えることを確認する
- 作業ディレクトリを`10-build-api-server`に合わせる

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/10-build-api-server
java -version
javac -version
which curl
```

完了条件:
- `java` / `javac` / `curl` が利用可能
- `10-build-api-server` 配下で作業開始できる

---

## Step 1: 最小構成のファイルを準備する

やること:
- `src` ディレクトリを作成する
- 実装ファイル `src/Main.java` を用意する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/10-build-api-server
mkdir -p src
touch src/Main.java
```

完了条件:
- `src/Main.java` が作成されている

---

## Step 2: APIサーバーの最小要件を実装する

やること:
- `ServerSocket` で `localhost:8080` を待ち受ける
- リクエスト行（method/path）を読み取る
- `GET /health` と `GET /hello` を返す
- 未定義パスは `404 Not Found` を返す
- 例外発生時は `500 Internal Server Error` を返す

補足:
- ここではフレームワークは使わない
- Controller層のみの固定実装で問題ない（次段階で分離）

実装手順（詳細）:
1. `import` を追加して、`ServerSocket` / `Socket` / `BufferedReader` / `OutputStream` を使えるようにする
2. `PORT = 8080` を定義し、`main` で待受ループを作る
3. `handleClient` でリクエスト行とヘッダー終端までを読み取る
4. `method` と `path` を取り出してルーティングする
5. `sendJson` でステータス行・ヘッダー・JSON本文を返す
6. `handleClient` 内で例外が起きた場合は `500` を返す

`src/Main.java`（実装例）:
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

完了条件:
- 上記のルート/エラーを返せる実装になっている

---

## Step 3: コンパイルしてサーバーを起動する

やること:
- Javaコードをコンパイルして起動する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/10-build-api-server
javac src/Main.java
java -cp src Main
```

完了条件:
- サーバーが待受状態で起動している
- ポート`8080`で接続を受けられる

---

## Step 4: curlで正常系・異常系を確認する

やること:
- 実装したエンドポイントと404を検証する

コマンド例:
```bash
curl -i http://127.0.0.1:8080/health
curl -i http://127.0.0.1:8080/hello
curl -i http://127.0.0.1:8080/not-found
```

期待結果:
- `/health`: `200 OK`
- `/hello`: `200 OK`
- `/not-found`: `404 Not Found`

完了条件:
- 3ケースのHTTPステータスを確認できる

---

## Step 5: 検証ログを残す

やること:
- 実行結果を1ファイルにまとめる

コマンド例:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/10-build-api-server
{
  echo "# 10 API Server Test Log";
  date;
  echo;
  echo "## /health";
  curl -i http://127.0.0.1:8080/health;
  echo;
  echo "## /hello";
  curl -i http://127.0.0.1:8080/hello;
  echo;
  echo "## /not-found";
  curl -i http://127.0.0.1:8080/not-found;
} > API_SERVER_TEST_LOG.md
```

完了条件:
- `API_SERVER_TEST_LOG.md` が作成されている
- 正常系/異常系の結果が残っている

---

## 最終チェック

- `ServerSocket`ベースでAPIサーバーを起動できる
- `GET /health` / `GET /hello` / `404` を説明できる
- 実行結果ログを残して再現可能な状態になっている
