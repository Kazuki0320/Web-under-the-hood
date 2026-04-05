# 10 Build API Server

フレームワークなしでAPIサーバーを実装し、HTTP処理の土台を理解するためのディレクトリ。

## 目的
- 標準ライブラリだけでAPIサーバーを構築する
- ルーティング、リクエスト解析、レスポンス返却の流れを理解する

## 最小ゴール
- `ServerSocket` で `localhost:8080` の待ち受けができる
- `GET /health` と `GET /hello` を実装できる
- `404 Not Found` と `500 Internal Server Error` を返せる
- `curl -v` で正常系/異常系を確認できる
