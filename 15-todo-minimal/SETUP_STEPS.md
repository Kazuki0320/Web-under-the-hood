# SETUP_STEPS

## 前提
- ここでは実装コードは書かず、最短で進める手順だけ定義する。
- バックエンドは Java、フロントは HTML/CSS/JavaScript 前提。

## 手順
1. バックエンドの最小API（`GET /hello`）を作る
2. サーバーを `localhost:8080` で起動する
3. `curl http://localhost:8080/hello` でJSONが返ることを確認する
4. フロントの `index.html` を作る
5. ボタン押下で `fetch("http://localhost:8080/hello")` を呼ぶ
6. 取得結果を画面に表示する
7. 別オリジン構成ならCORSを追加して再確認する

## 確認ポイント
- API呼び出しURLは正しいか
- ブラウザDevToolsでNetworkが200になっているか
- 失敗時にCORSエラーか、接続エラーかを区別できるか
