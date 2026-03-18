# 1接続で2リクエストはどう実現する？ Javaで学ぶKeep-Alive入門

## はじめに

`ServerSocket` ベースの最小HTTPサーバーを題材に、HTTP/1.1 の keep-alive を実装してみました。  
この記事では「動いた」で終わらず、次の4点を中心に整理します。

- keep-alive は何をしているのか
- 切断主導はクライアント/サーバーどちら側でも実装として選べること
- 運用でハマりやすい注意点
- 実装時に気になりやすいポイント

---

## keep-alive は何をしているのか

一言でいうと、**1本のTCP接続を使い回して複数のHTTPリクエストを処理する仕組み**です。

keep-alive なし（毎回切断）:

1. 接続確立
2. 1リクエスト/1レスポンス
3. 切断
4. 次のリクエストでまた接続確立

keep-alive あり:

1. 接続確立
2. リクエスト1/レスポンス1
3. 同じ接続でリクエスト2/レスポンス2
4. 必要なタイミングで切断

この差によって、接続確立コストを減らし、体感速度や効率を改善できます。

---

## keep-alive のメリットとデメリット

### メリット

- 接続確立の回数が減り、レスポンス体感が改善しやすい
- 同じクライアントとの連続通信でCPU/ネットワーク効率が上がりやすい
- HTTPS環境ではハンドシェイク回数削減の効果も期待できる

### デメリット

- 接続を長く保持する分、サーバー側の接続リソースを消費しやすい
- timeout設計が悪いと、前段との切断競合やエラー原因になる
- 本文読み残しなどの実装ミスが、次リクエストへ波及しやすい

「速くなる可能性」と「保持コスト」のトレードオフなので、  
timeout・上限・監視をセットで設計するのが実務では重要です。

---

## 今回の実装方針（最小）

今回のコアは、接続ごとに内側ループを持つことです。

```java
while (true) { // acceptループ
    try (Socket client = server.accept()) {
        handleConnection(client);
    }
}
```

```java
private static void handleConnection(Socket client) throws IOException {
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
    );
    OutputStream out = client.getOutputStream();

    while (true) { // 同一接続で複数リクエスト
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) break;

        // method/path 解析
        // ヘッダー解析
        // レスポンス返却
    }
}
```

`curl -v http://localhost:8080/ http://localhost:8080/hello` で  
`Re-using existing connection ...` が出れば、接続再利用が確認できます。

---

## 実装全体（Main.java）

そのまま試せるように、今回の `Main.java` 全体を載せます。

この実装の前提:

- 学習用の最小実装として、`Content-Length` の本文読み切りは未対応
- 実運用向けには本文読み切り処理を追加する

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
    // 接続がアイドル状態でこの時間を超えたら読み取りをタイムアウトさせる
    private static final int IDLE_TIMEOUT_MS = 5000;
    // 1つのTCP接続で受け付ける最大リクエスト数
    private static final int MAX_REQUESTS_PER_CONNECTION = 5;

    public static void main(String[] args) {
        // サーバーソケットを作成して待ち受け開始
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);
            while (true) {
                // クライアント接続を1つ受け付ける
                try (Socket client = server.accept()) {
                    System.out.println("Accepted: " + client.getRemoteSocketAddress());
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
        // 同一接続が長く遊ばないようにタイムアウト設定
        client.setSoTimeout(IDLE_TIMEOUT_MS);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
        );
        OutputStream out = client.getOutputStream();
        int requestCount = 0;

        // keep-alive: 同じTCP接続で複数リクエストを順番に処理
        while (requestCount < MAX_REQUESTS_PER_CONNECTION) {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                // 接続終了 or 不正/空リクエスト
                break;
            }
            requestCount++;

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "";

            // ヘッダーを空行まで読み取ってMapに格納する
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

            // クライアントが close を要求したらこのレスポンス後に切断する
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

            // HTTPレスポンスを「ヘッダー -> 本文」の順に返す
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

実装時の補足:

- `Map<String, String> headers` を作るループは、`Host: ...` や `Connection: ...` を空行まで読んで `key/value` として保存する処理です。  
  この値を `headers.get("connection")` で参照して、接続を維持するか切るかを判定します。

---

## 実行して確認する手順

```bash
cd 06-java-http-keep-alive
javac src/Main.java
java -cp src Main
```

別ターミナル:

```bash
curl -v http://localhost:8080/ http://localhost:8080/hello
```

確認ポイント:

- `Re-using existing connection ...` が出る
- `Connection #0 ... left intact` が出る

---

## 切断主導はどちら側でも実装できる

ここは「どちらが正しいか」ではなく、要件に応じて実装を選べるポイントです。
判断軸は「接続効率を優先するか」「安全側に早めに回収するか」です。

### クライアント主導

`Connection: close` を送ると「この応答後に切りたい」という意思表示になります。

```bash
curl -v -H 'Connection: close' http://localhost:8080/hello
```

### サーバー主導

サーバー側もポリシーで切断できます。例:

- `Socket#setSoTimeout(...)`（アイドルタイムアウト）
- `MAX_REQUESTS_PER_CONNECTION`（1接続あたり上限）
- 負荷状況に応じた早期クローズ

つまり実際は、**クライアント意思 + サーバー運用方針** の両方で接続寿命が決まります。


## 運用上の注意点

### 1. タイムアウトの整合

前段（LB/CDN）とアプリで timeout 設計がズレると、切断競合でエラーが増えることがあります。  
「どちらが先に切るか」を意識して設計するのが大事です。

### 2. 本文の読み残し

keep-alive では同じストリームを使い続けるため、`Content-Length` 分の読み切りが重要です。  
読み残すと次リクエスト境界が壊れます。

### 3. 上限管理が必要

接続を保持し続けるとリソースを圧迫します。  
`idle timeout` と `max requests` の2軸を最低限入れておくと安定しやすいです。

### 4. 観測ポイントを先に決める

`curl -v` の以下を見ると状況を追いやすいです。

- 再利用: `Re-using existing connection ...`
- 維持: `Connection #0 ... left intact`
- 切断: `Closing connection ...`

---

## まとめ

- keep-alive の本質は「同一TCP接続の再利用」
- 切断はクライアント/サーバーどちらからでも可能
- 実運用では timeout・上限・本文読み切りが重要
- まずは `curl -v` で再利用ログを読めるようになると理解が一気に進む
