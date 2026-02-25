# 03 Java Only HTTP Server: 実装テスト

目的:
- 手を動かして、HTTPサーバー実装の理解を定着させる。

進め方:
- 各問題を `src/Main.java` で実装して `javac` / `curl -v` で確認する。
- 問題ごとに「できた / できない / 要復習」を記録する。

---

## 事前コマンド

```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/03-java-only-http-server
javac src/Main.java
java -cp src Main
```

---

## 実装テスト（12問）

### T1: 8080待受を実装
要件:
- `ServerSocket(8080)` で待受開始
- `accept()` で接続ログを出す

確認:
```bash
curl -v http://localhost:8080/
```

- 結果:
- 要復習: [ ]

### T2: 固定200レスポンス
要件:
- `HTTP/1.1 200 OK`
- 本文 `Hello World`
- `Content-Type`, `Content-Length` を付与

確認:
```bash
curl -v http://localhost:8080/
```

- 結果:
- 要復習: [ ]

### T3: requestLineを読む
要件:
- `reader.readLine()` で1行目を取得
- ログに `method` と `path` を表示

確認:
```bash
curl -v http://localhost:8080/
curl -v http://localhost:8080/hello
```

- 結果:
- 要復習: [ ]

### T4: ヘッダーを空行まで読む
要件:
- `while ((line = reader.readLine()) != null && !line.isEmpty()) { ... }`
- 空行でヘッダー終了できること

- 結果:
- 要復習: [ ]

### T5: `/` のルーティング
要件:
- `GET /` -> `200`, `text/html`, HTML本文

確認:
```bash
curl -v http://localhost:8080/
```

- 結果:
- 要復習: [ ]

### T6: `/hello` のルーティング
要件:
- `GET /hello` -> `200`, `text/plain`, `Hello World`

確認:
```bash
curl -v http://localhost:8080/hello
```

- 結果:
- 要復習: [ ]

### T7: `/api/hello` のルーティング
要件:
- `GET /api/hello` -> `200`, `application/json`, `{"message":"Hello API"}`

確認:
```bash
curl -v http://localhost:8080/api/hello
```

- 結果:
- 要復習: [ ]

### T8: 404の実装
要件:
- 未定義パスは `404 Not Found`
- 本文 `Not Found`

確認:
```bash
curl -v http://localhost:8080/notfound
```

- 結果:
- 要復習: [ ]

### T9: typoバグ修正
課題:
- `"/helllo"` のようなtypoを混ぜて、`/hello` が404になる状態を再現
- その後正しいパスに修正

- 結果:
- 要復習: [ ]

### T10: ヘッダー整合バグ修正
課題:
- `Content-Length` をわざと間違えて挙動確認
- 正しい値に直して再確認

- 結果:
- 要復習: [ ]

### T11: `\r\n` バグ修正
課題:
- ヘッダー終端の空行を崩す (`\r\n\r\n` を壊す)
- 失敗を確認して元に戻す

- 結果:
- 要復習: [ ]

### T12: 最終確認（4エンドポイント一括）
要件:
- `/`, `/hello`, `/api/hello`, `/notfound` の結果を表にまとめる

確認:
```bash
curl -v http://localhost:8080/
curl -v http://localhost:8080/hello
curl -v http://localhost:8080/api/hello
curl -v http://localhost:8080/notfound
```

記録テンプレート:

| path | status | Content-Type | body(要点) |
|---|---|---|---|
| / |  |  |  |
| /hello |  |  |  |
| /api/hello |  |  |  |
| /notfound |  |  |  |

- 結果:
- 要復習: [ ]

---

## 自己評価

- 完了した問題数:
- 詰まった問題:
- 次に直すポイント:
