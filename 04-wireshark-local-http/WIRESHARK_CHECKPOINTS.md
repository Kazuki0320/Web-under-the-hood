# WIRESHARK_CHECKPOINTS

## テーマ
04の実験で、Wireshark上で最低限確認すべき観点をまとめる。

## キャプチャ前チェック
- インターフェース: `Loopback: lo0`
- 表示フィルタ: `tcp.port == 8080`
- サーバー起動: `java -cp src Main`

## 正常系チェック
1. 接続確立
- `SYN -> SYN/ACK -> ACK`

2. リクエスト送信
- クライアント側 `PSH, ACK`
- `Follow TCP Stream` で `GET /... HTTP/1.1` を確認

3. レスポンス受信
- `HTTP/1.1 200 OK` または `HTTP/1.1 404 Not Found`
- `Content-Type`, `Content-Length`, `Connection` を確認

4. 接続終了
- `FIN, ACK` の往復でクローズ

## 異常系チェック
- サーバー停止で `SYN` の後に `RST, ACK`
- 意味: 接続拒否（待ち受けなし）

## 便利フィルタ
- `tcp.port == 8080`
- `http`
- `tcp.flags.reset == 1`
- `tcp.stream == N`（特定接続だけ追う）

## 詰まりポイント
- `lo0` 以外を見ている
- フィルタ式ミス（例: `===` や末尾余計文字）
- `nc` で空行を入れず、HTTPリクエストが未完了
