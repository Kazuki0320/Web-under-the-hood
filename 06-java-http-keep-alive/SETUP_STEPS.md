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

完了条件:
- 同一接続で連続リクエストを処理できる実装になっている

---

## Step 4: 安全な終了条件を追加する

やること:
- `Socket#setSoTimeout(...)` でアイドルタイムアウトを設定
- `MAX_REQUESTS_PER_CONNECTION` を設けて上限管理
- 本文付きリクエストの読み残しを防ぐ（`Content-Length` 分を消費）

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
curl -v --http1.0 http://localhost:8080/ http://localhost:8080/hello
curl -v --http1.0 -H 'Connection: Keep-Alive' http://localhost:8080/ http://localhost:8080/hello
curl -v -H 'Connection: close' http://localhost:8080/ http://localhost:8080/hello
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

---

## Step 6: 結果を記録する

やること:
- 検証パターンごとに「接続再利用されたか」を記録する

記録テンプレート:

| パターン | 期待 | 実結果 | メモ |
|---|---|---|---|
| HTTP/1.1（デフォルト） | 再利用されやすい |  |  |
| HTTP/1.0 | 切断されやすい |  |  |
| HTTP/1.0 + Keep-Alive | 再利用される場合あり |  |  |
| Connection: close 指定 | 切断される |  |  |
| idle timeout到達 | 切断される |  |  |
| max requests到達 | 切断される |  |  |

完了条件:
- 再利用/切断の理由を、ログ付きで説明できる
