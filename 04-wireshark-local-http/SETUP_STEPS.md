# Wireshark ローカルHTTP観測 手順書（04）

このファイルは、`04-wireshark-local-http` を一つずつ確認しながら進めるための実行手順です。  
04配下の `src/Main.java` を使い、`nc`で生のHTTP/1.1リクエストを送って観測します。

## Step 0: Wiresharkをインストールする

やること:
- HomebrewでWiresharkをインストールする
- アプリ起動後、必要に応じてパケットキャプチャ権限を許可する

コマンド:
```bash
brew install --cask wireshark
```

完了条件:
- Wiresharkが起動できる
- インターフェース一覧（`lo0` など）が表示される

---

## Step 1: 04用のMain.javaを用意する

やること:
- `04-wireshark-local-http/src/Main.java` を作成する
- 接続直後には返さず、リクエスト行/ヘッダーを読んでから返す実装にする
- `GET /hello` は `200`、それ以外は `404` を返す

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/04-wireshark-local-http
mkdir -p src
```

`src/Main.java`:
```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        int port = 8080;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            while (true) {
                try (Socket client = server.accept()) {
                    System.out.println("Accepted: " + client.getRemoteSocketAddress());

                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
                    );

                    String requestLine = reader.readLine();
                    if (requestLine == null || requestLine.isEmpty()) {
                        continue;
                    }

                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    }

                    String[] parts = requestLine.split(" ");
                    String method = parts.length > 0 ? parts[0] : "";
                    String path = parts.length > 1 ? parts[1] : "";
                    System.out.println("method=" + method + ", path=" + path);

                    String status;
                    String responseBody;
                    if ("GET".equals(method) && "/hello".equals(path)) {
                        status = "200 OK";
                        responseBody = "Hello World";
                    } else {
                        status = "404 Not Found";
                        responseBody = "Not Found";
                    }

                    byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                    String headers =
                        "HTTP/1.1 " + status + "\\r\\n" +
                        "Content-Type: text/plain; charset=UTF-8\\r\\n" +
                        "Content-Length: " + bodyBytes.length + "\\r\\n" +
                        "Connection: close\\r\\n" +
                        "\\r\\n";

                    OutputStream out = client.getOutputStream();
                    out.write(headers.getBytes(StandardCharsets.UTF_8));
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

実装後の確認コマンド:
```bash
javac src/Main.java
```

完了条件:
- `javac src/Main.java` が通る
- `nc`で未入力時は即レスポンスせず待機する

---

## Step 2: 04のHTTPサーバーを起動する

やること:
- `04-wireshark-local-http` の `Main` を起動する
- `:8080` 待ち受け状態を作る

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/04-wireshark-local-http
java -cp src Main
```

完了条件:
- サーバーログに待ち受け開始メッセージが出る
- `8080` で接続受付できる状態になっている

---

## Step 3: Wiresharkでキャプチャ準備をする

やること:
- Wiresharkを起動する
- インターフェースに `lo0`（loopback）を選ぶ
- 表示フィルタを `tcp.port == 8080` に設定する

補足:
- `localhost` / `127.0.0.1` 宛通信は loopback 上に流れる
- インターフェースを間違えると何も見えない

完了条件:
- `lo0` でキャプチャ開始できる
- `tcp.port == 8080` で対象通信だけ表示できる

---

## Step 4: `nc` でHTTP/1.1リクエストを手入力する

やること:
- 新しいターミナルで `nc 127.0.0.1 8080` に接続する
- 以下をそのまま入力して送信する（最後に空行が必要）

コマンド:
```bash
nc 127.0.0.1 8080
```

入力するHTTPメッセージ:
```http
GET /hello HTTP/1.1
Host: localhost:8080
Connection: close

```

完了条件:
- `nc`上でHTTPレスポンス（`HTTP/1.1 200 ...`）が返る
- 04サーバーログに `method` / `path` が出る

---

## Step 5: パケットを読み解く

やること:
- SYN -> SYN/ACK -> ACK の接続確立を確認する
- HTTPリクエスト送信パケットを特定する
- HTTPレスポンス（ステータス行・ヘッダー・本文）を確認する
- `Follow TCP Stream` で送受信テキスト全体を確認する

完了条件:
- 「どのパケットが接続確立/リクエスト/レスポンスか」を説明できる
- アプリログとパケット時系列を対応づけられる

---

## Step 6: 404系を観測する

やること:
- もう一度 `nc 127.0.0.1 8080` で接続し、存在しないパスを送る

入力するHTTPメッセージ:
```http
GET /notfound HTTP/1.1
Host: localhost:8080
Connection: close

```

完了条件:
- `HTTP/1.1 404 ...` を確認できる
- 200系との違いを、レスポンス行・本文・パケットで説明できる

---

## Step 7: 失敗系（接続拒否）を観測する

やること:
- 04サーバーを停止する（`Ctrl+C`）
- その状態で `nc 127.0.0.1 8080` を実行する

コマンド:
```bash
nc 127.0.0.1 8080
```

完了条件:
- 接続失敗が再現する
- Wiresharkで失敗時のパケット挙動（例: RST）を確認できる

---

## よくある詰まりポイント

- `nc`入力後に空行を入れておらず、リクエストが完了しない
- `Host` ヘッダーを書かず、HTTP/1.1の期待とずれて挙動が不安定になる
- `lo0` 以外をキャプチャしていて通信が見えない
- `tcp.port == 8080` フィルタを入れず、不要パケットで埋もれる

## 最終チェック

- 200系・404系・接続失敗系の3パターンを観測できた
- それぞれの結果を「アプリ実装」「HTTPテキスト」「パケット」の3視点で説明できる
