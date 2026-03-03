# FLOW_NC_TO_CLOSE

## テーマ
`nc 127.0.0.1 8080` を叩いた瞬間から接続終了までを、ネットワーク視点で分解する。

## 前提
- サーバー: `04-wireshark-local-http/src/Main.java` が `:8080` で待ち受け
- クライアント: `nc 127.0.0.1 8080`
- 観測: Wireshark（`lo0`, `tcp.port == 8080`）

## 全体フロー
1. `nc` が `connect()` を発行し、`127.0.0.1:8080` へ接続開始
2. 宛先が `127.0.0.1` のため、OSは loopback（`lo0`）へルーティング
3. TCP 3-way handshake（SYN -> SYN/ACK -> ACK）
4. サーバー側 `accept()` が戻り、通信専用 `Socket` が作られる
5. 接続のみの状態（ESTABLISHED）で待機
6. ユーザーがHTTPメッセージを入力
7. `PSH, ACK` でHTTPリクエストが送信される
8. サーバーがリクエストを読み、`GET /hello` なら `200`、それ以外は `404`
9. サーバーがHTTPレスポンスを返す（通常 `PSH, ACK`）
10. `Connection: close` により TCP切断（FIN/ACK 往復）

## Wiresharkでの見え方（正常系）
1. `SYN`
2. `SYN, ACK`
3. `ACK`
4. `PSH, ACK`（クライアント -> サーバー、HTTPリクエスト）
5. `HTTP/1.1 200 OK` または `HTTP/1.1 404 Not Found`
6. `FIN, ACK` 往復でクローズ

## サーバー停止時（異常系）
1. クライアントが `SYN`
2. 直後に `RST, ACK` が返る

意味:
- ポート `8080` に待ち受けプロセスがいないため接続拒否
- 3-way handshakeは成立しない
