# HTTP と WebSocket の違いを観察する手順書（16）

このファイルは、`16-http-vs-websocket` を一つずつ確認しながら進めるための実行手順です。  
HTTP の `request -> response` と、WebSocket の `Upgrade` 後の双方向通信を、実装とパケット観測の両方で確認します。

## Step 0: Wireshark を用意する

やること:
- Wireshark をインストールする
- パケットキャプチャ権限を許可する
- `lo0` などの loopback インターフェースを選べることを確認する

コマンド:
```bash
brew install --cask wireshark
```

確認ポイント:
- Wireshark が起動できる
- ローカル通信を観察するためのインターフェースが選べる

完了条件:
- Wireshark を起動できる
- `localhost` 宛の通信をキャプチャできる状態になる

---

## Step 1: HTTP の最小サーバーを用意する

やること:
- `GET /hello` を返す最小 HTTP サーバーを作る
- `200 OK` と JSON かテキストを返す
- `Content-Length` と `Connection` を正しく付ける

確認したいこと:
- HTTP は 1 リクエストごとに処理が完結する
- サーバーは毎回 request を受けて response を返す

完了条件:
- `curl http://localhost:8080/hello` で `200 OK` が返る
- レスポンス本文が確認できる

---

## Step 2: WebSocket の最小サーバーを用意する

やること:
- `Upgrade: websocket` を受ける最小実装を作る
- `101 Switching Protocols` を返す
- 握手後にテキストメッセージを受けて返せるようにする

確認したいこと:
- 最初は HTTP で始まる
- 握手後は HTTP ではなく WebSocket フレームで通信する

完了条件:
- WebSocket クライアントから接続できる
- テキスト送信に対して応答が返る

---

## Step 3: HTTP を Wireshark で観察する

やること:
- Wireshark で `lo0` をキャプチャする
- 表示フィルタを `tcp.port == 8080` にする
- `curl http://localhost:8080/hello` を実行する

見るポイント:
- TCP の 3-way handshake
- `GET /hello HTTP/1.1`
- `HTTP/1.1 200 OK`
- ヘッダーと本文が平文で見えること

補足:
- `localhost` の通信は loopback 上に流れる
- キャプチャ対象インターフェースを間違えると何も見えない

完了条件:
- HTTP のリクエストとレスポンスを Wireshark で読める
- `Follow TCP Stream` で全文を追える

---

## Step 4: WebSocket の握手を Wireshark で観察する

やること:
- WebSocket サーバーを起動する
- ブラウザの DevTools Console か簡単な HTML から接続する
- `ws://localhost:8080` に接続する

見るポイント:
- `GET` リクエストに `Upgrade: websocket` が付いていること
- `Connection: Upgrade`
- `Sec-WebSocket-Key` / `Sec-WebSocket-Version`
- サーバーの `101 Switching Protocols`

完了条件:
- WebSocket の握手が HTTP の拡張として始まることを説明できる
- `101 Switching Protocols` を確認できる

---

## Step 5: WebSocket フレームを観察する

やること:
- 接続後にテキストを送る
- サーバーからの応答を返す
- Wireshark で送受信フレームを追う

見るポイント:
- 握手後に HTTP ではなくフレーム単位の通信になること
- `Text` フレームや `Close` フレームが見えること
- `ping` / `pong` が使われる場合はその制御フレームも確認する

補足:
- Wireshark が WebSocket として自動解釈できない場合は `Follow TCP Stream` を使う
- WebSocket は 1 本の TCP 接続を維持して双方向に通信する

完了条件:
- 送信したメッセージと応答の流れを追える
- HTTP との違いをパケット単位で説明できる

---

## Step 6: `ws://` と `wss://` を比較する

やること:
- まずは暗号化なしの `ws://` で観察する
- 必要なら TLS を使う `wss://` でも試す

見るポイント:
- `ws://` は中身を読みやすい
- `wss://` は TLS で暗号化されるため、素のキャプチャでは内容が読めない
- 暗号化されると、見えるのは主に接続先や TLS の握手までになる

完了条件:
- どこまで見えて、どこから見えなくなるかを説明できる

---

## Step 7: HTTP と WebSocket の違いをまとめる

やること:
- HTTP と WebSocket を観察結果つきで比較する
- 用途ごとにどちらを使うべきか整理する

確認ポイント:
- HTTP は都度の取得・更新向き
- WebSocket はリアルタイム通知・チャット・双方向更新向き
- WebSocket は「HTTPで始まるが、ずっとHTTPではない」ことを説明できるか

完了条件:
- HTTP と WebSocket の違いを、自分の言葉で説明できる
- Wireshark の観察内容を使って説明できる
