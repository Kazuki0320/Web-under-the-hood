# QA_MEMO_TCP_HTTP

このメモは、実装中に出た疑問をテーマ別に整理したもの。
各項目は「疑問点」と「こういう意味らしい」で記録する。

## 1. ソケットと接続の役割

### 1-1. `try (ServerSocket ...)` と `try (Socket client = server.accept())` の違い
疑問点:
- 「最初の`try`で8080を開くのは分かるが、アクセス待ちは2つ目の`try`で合っているか？」

こういう意味らしい:
- 最初の`try (ServerSocket ...)`は待受ソケットを作る処理。
- 2つ目の`try (Socket client = server.accept())`は接続要求を待ち、接続ごとの通信用ソケットを受け取る処理。
- Java構文としては`try ... catch`であり、`try ... else`は使えない。

### 1-2. `accept()` は何をしているか
疑問点:
- 「`accept`でやっていることは何？」

こういう意味らしい:
- 接続要求が来るまで待機（ブロック）する。
- 接続が成立したら、そのクライアント専用の`Socket`を返す。

### 1-3. `accept()` が返すソケットはクライアント側のものか
疑問点:
- 「このソケットはサーバー側ではなくクライアント側の話か？」

こういう意味らしい:
- サーバー側で保持するオブジェクト。
- ただし用途は「そのクライアントとの1対1通信用」。
- `ServerSocket`（待受用）とは別物。

## 2. ストリームとHTTP読み取り

### 2-1. `BufferedReader(new InputStreamReader(client.getInputStream(), UTF_8))` の意味
疑問点:
- 「この処理は何をしているのか？」
- 「Streamはどういう役割か？」

こういう意味らしい:
- `client.getInputStream()`でソケットから生バイトを受信する。
- `InputStreamReader(..., UTF_8)`でバイトを文字に変換する。
- `BufferedReader`で`readLine()`を使えるようにする。
- Streamはデータの流れを表す。
  - `InputStream`: 受信
  - `OutputStream`: 送信

### 2-2. `while ((line = reader.readLine()) != null && !line.isEmpty())` の意味
疑問点:
- 「この処理は何をしているのか？」

こういう意味らしい:
- HTTPヘッダー終端（空行）まで読み進める処理。
- つまり`CRLF CRLF`に到達するまでヘッダー行を読み飛ばしている。

### 2-3. `requestLine.split(" ")` / `method` / `path` 取り出しの意味
疑問点:
- 「この4行はそれぞれ何をしているのか？」
  - `String[] parts = requestLine.split(" ");`
  - `String method = parts.length > 0 ? parts[0] : "";`
  - `String path = parts.length > 1 ? parts[1] : "";`
  - `System.out.println("method=" + method + ", path=" + path);`

こういう意味らしい:
- リクエスト行（例: `GET /hello HTTP/1.1`）を空白分割する。
- 1要素目を`method`、2要素目を`path`として安全に取り出す。
- 最後に`method/path`をログ出力して受信内容を確認する。

### 2-4. `out.flush()` の意味
疑問点:
- 「`out.flush()`って何？」

こういう意味らしい:
- `OutputStream`の送信バッファを即時に流す処理。
- `write()`したヘッダーと本文を確実に相手へ送るために使う。

## 3. WiresharkフィルタとTCPフラグ

### 3-1. `tcp.stream == 8080` は使えるか
疑問点:
- 「`tcp.stream == 8080`はダメか？」

こういう意味らしい:
- ダメ。
- `tcp.stream`は接続ID（0,1,2...）で、ポート番号ではない。
- ポート指定は`tcp.port == 8080`を使う。

### 3-2. `RST` の意味
疑問点:
- 「`RST`って何？」

こういう意味らしい:
- TCP Reset（接続の即時中断/拒否）。
- 典型例は待受なしポートへの接続時で、`SYN`の後に`RST,ACK`が返る。
