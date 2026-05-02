# Stateful vs Stateless 実装 手順書（13）

このファイルは、`13-stateful-vs-stateless` を順番に進めるための実行手順です。  
まずは実装前に「今回の目的」と「ステートレス/ステートフルが使われる部分」を固定します。

## Step 0: 今回の学習目的を固定する

やること:
- 同一要件を `Cookie Session`（Stateful）と `JWT Bearer`（Stateless）で実装し、差分を説明できる状態を目指す
- 「認証情報の運搬方法」と「認証状態の保持方法」を分けて整理する
- 認証方式と状態管理方式を混同せずに説明できるようにし、方式選定理由を根拠付きで説明できるようにする
- 最後に「どの要件ならどちらを選ぶか」を根拠付きで言語化する

この章で学ぶ到達目標:
1. `POST /login` / `GET /me` / `POST /logout` を2方式で実装できる
2. Stateful/Stateless の違いをコードと実行結果で説明できる
3. 認証方式と状態管理方式を分離して説明し、選定理由を根拠付きで説明できる
4. セキュリティと運用のトレードオフを比較表で示せる

完了条件:
- 方式選択の比較軸（セキュリティ、運用、拡張性）が固定されている
- 実装結果を使って「なぜこの方式を選ぶか」を説明できる

---

## Step 1: ステートレスとステートフルが使われる部分を確認する

### 1-1. HTTPそのもの
- 基本はステートレス
- 各リクエストは独立して処理される（前回リクエストの状態を前提にしない）

### 1-2. Cookie Session 認証（Stateful）
- `POST /login` 成功時に `sid` を発行
- サーバー側 Store に `sid -> session` を保存
- `GET /me` は毎回 Store を参照して認証判定
- `POST /logout` は Store から session を削除して失効

要点:
- 認証判定にサーバー状態参照が必須なので Stateful

### 1-3. JWT Bearer 認証（Stateless）
- `POST /login` 成功時に JWT を発行
- `GET /me` は `Authorization: Bearer <token>` の署名・期限を検証
- 最小構成ではサーバーStore参照なしで判定可能

要点:
- 認証判定がトークン自己完結なので Stateless

### 1-4. JWTでもStatefulになる部分（発展）
- denylist（失効リスト）を持つ
- refresh token を保存して再発行管理する
- token version をユーザー単位で管理する

要点:
- 「Bearer方式」そのものではなく、運用要件次第で部分的にStateful化する

---

## Step 2: この章で使う比較軸を固定する

比較軸:
1. 認証判定時にサーバー状態参照が必要か
2. 失効（logout/revoke）のしやすさ
3. XSS観点での注意点
4. スケール時の構成難易度

完了条件:
- 上記4軸で、実装後に方式比較できる準備ができている

---

## Step 3: 今回の実装方針を確定する（この章の制約）

この章の実装方針:
1. フレームワークなし Java（`ServerSocket` ベース）で実装する
2. JWTはライブラリを使う（自前署名実装はしない）
3. CSRF対策は今回は扱わない（別章で実施）

実装方式（比較しやすさ優先）:
- 同一プロセス・同一ポートで2方式を並行実装する
- Cookie Session系:
  - `POST /cookie/login`
  - `GET /cookie/me`
  - `POST /cookie/logout`
- JWT Bearer系:
  - `POST /jwt/login`
  - `GET /jwt/me`
  - `POST /jwt/logout`

完了条件:
- 2方式のAPI境界が先に固定されている
- 今回はやらないこと（CSRF）が明文化されている

---

## Step 4: 最小ファイル構成を作る

やること:
- Cookie Session と JWT の責務を分離できる構成を先に用意する
- 実装と検証ログを残すための補助ファイルも同時に作る

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/13-stateful-vs-stateless
mkdir -p src/controller src/service src/repository src/model src/store src/security src/http lib

touch src/Main.java
touch src/controller/AuthController.java
touch src/service/CookieSessionAuthService.java
touch src/service/JwtAuthService.java
touch src/repository/UserRepository.java
touch src/store/SessionStore.java
touch src/store/JwtRevocationStore.java
touch src/security/JwtProvider.java
touch src/http/HttpRequest.java
touch src/http/HttpResponse.java
touch src/model/User.java
touch src/model/Session.java
```

完了条件:
- 主要クラスが揃っている

---

## Step 5: セッション認証を先に完成させる（Phase 1）

やること:
- まず Cookie Session 側だけで `login -> me -> logout` を成立させる
- この段階では JWT 実装は着手しない

Phase 1 で実装する対象:
- `src/chapter13/model/User.java`
- `src/chapter13/model/Session.java`
- `src/chapter13/repository/UserRepository.java`
- `src/chapter13/store/SessionStore.java`
- `src/chapter13/service/CookieSessionAuthService.java`
- `src/chapter13/controller/AuthController.java`（Cookieルートのみ）
- `src/Main.java`

Phase 1 実装ステップ（この順で着手）:
1. `User` と `Session` を完成させる
   - `User`: `id`, `name` の不変フィールド、コンストラクタ、getter
   - `Session`: `sid`, `userId`, `expiresAtEpochSecond`、getter、`isExpired(now)`
2. `UserRepository` を実装する
   - `findByCredentials(String id, String password): User | null`
   - 固定ユーザー（例: `demo/password123`）で最小実装
3. `SessionStore` を実装する
   - `save(String sid, Session session)`
   - `find(String sid): Session | null`
   - `delete(String sid)`
4. `CookieSessionAuthService` を実装する
   - `login(id, password)`:
     - `UserRepository` で認証
     - 成功時 `sid` 生成 + `expiresAt` 設定 + `SessionStore.save(...)`
   - `me(sid)`:
     - `SessionStore.find(sid)` -> `null` / 期限切れなら未認証
   - `logout(sid)`:
     - `SessionStore.delete(sid)`
5. `AuthController`（Cookieルートのみ）を実装する
   - `POST /cookie/login`
   - `GET /cookie/me`
   - `POST /cookie/logout`
   - `Cookie` ヘッダーから `sid` を抽出
   - `200/401/404/405` を返す
6. `Main` を実装する
   - `ServerSocket(8080)` 起動
   - acceptループ
   - `AuthController` へ処理委譲
7. Phase 1 の動作確認を実施する
   - loginで `Set-Cookie` が返る
   - meで `200`
   - logout後meで `401`

Phase 1 の完了条件:
- `POST /cookie/login` が `Set-Cookie: sid=...` を返す
- `GET /cookie/me` が `Cookie sid` で `200` を返す
- `POST /cookie/logout` 後、`GET /cookie/me` が `401` になる

---

## Step 6: JWTライブラリを導入する（Phase 2）

やること:
- JWT生成/検証をライブラリに委譲する
- 依存を `lib/` に揃えて `javac/java` で扱える状態にする

最小方針:
- `com.auth0:java-jwt` を利用する
- 依存jarは実装時点の安定版を選ぶ（`JWT_VERSION` を固定して記録）

コマンド例（`JWT_VERSION` は実装時に確定）:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/13-stateful-vs-stateless

# 例: JWT_VERSION=4.5.1
JWT_VERSION=<JWT_VERSION>
curl -L -o lib/java-jwt.jar \
  "https://repo1.maven.org/maven2/com/auth0/java-jwt/${JWT_VERSION}/java-jwt-${JWT_VERSION}.jar"

# 実装時点でjava-jwtが要求するruntime依存を同時に配置する
# 例: jackson-databind / jackson-core / jackson-annotations
```

完了条件:
- `lib/` に `java-jwt` と必要依存jarが揃っている
- `javac -cp "lib/*:src"` が通る準備ができている

Step 6の次に進めるJWT実装ステップ:
1. 依存jarをそろえる
   - `lib/` に `java-jwt` と必要依存を配置する
   - `javac -cp "lib/*:src" $(find src -name '*.java')` が通る状態にする
2. `JwtProvider` を最小実装する
   - `issueToken(userId)` を実装する
   - `verifyAndGetUserId(token)` を実装する
   - まずは「発行 -> 検証で同じuserIdが取れる」ことを確認する
3. `JwtAuthService.login` を実装する
   - `id/password` を `UserRepository` で検証する
   - 成功時のみ `JwtProvider.issueToken(...)` を返す
   - 失敗時は `null` を返す
4. `JwtAuthService.me` を実装する
   - `Authorization: Bearer ...` のtokenを検証する
   - `userId` が取れたら `UserRepository.findById(...)` を返す
   - 検証失敗時は `null` を返す
5. `AuthController` にJWT 2ルートを追加する
   - `POST /jwt/login`:
     - bodyから `id/password` を抽出する
     - 成功時 `{"token":"..."}`、失敗時 `401`
   - `GET /jwt/me`:
     - Bearer tokenを抽出して `service.me(token)` を呼ぶ
     - 成功時 `200`、失敗時 `401`
6. `curl` で正常系を通す
   - `POST /jwt/login` でtoken取得
   - `GET /jwt/me -H "Authorization: Bearer <token>"` が `200`
7. 失敗系を確認する
   - Bearerなしで `401`
   - 改ざんtokenで `401`
   - 期限切れtokenで `401`
8. 最後に整理する
   - `reasonPhrase` の `401` 対応を確認する
   - Cookie版とJWT版の挙動差をメモ化する

---

## Step 7: 認証仕様（I/F）を固定する

### 6-1. Cookie Session系
- `POST /cookie/login`
  - 入力: `{"id":"demo","password":"password123"}`
  - 成功: `200` + `Set-Cookie: sid=...; HttpOnly; SameSite=Lax`
  - 失敗: `401`
- `GET /cookie/me`
  - 入力: `Cookie: sid=...`
  - 成功: `200` + `{"id":"demo","name":"Demo User"}`
  - 失敗: `401`
- `POST /cookie/logout`
  - 入力: `Cookie: sid=...`
  - 成功: `200`（Storeからセッション削除）

### 6-2. JWT Bearer系
- `POST /jwt/login`
  - 入力: `{"id":"demo","password":"password123"}`
  - 成功: `200` + `{"accessToken":"...","tokenType":"Bearer","expiresIn":...}`
  - 失敗: `401`
- `GET /jwt/me`
  - 入力: `Authorization: Bearer <token>`
  - 成功: `200` + `{"id":"demo","name":"Demo User"}`
  - 失敗: `401`
- `POST /jwt/logout`
  - 最小実装: クライアント側破棄を前提に `200`
  - 発展実装: `JwtRevocationStore` による失効を追加可能

完了条件:
- 2方式の正常系/異常系ステータスが固定されている

---

## Step 8: 実装順を固定する（セッション先行）

推奨実装順:
1. `model`（`User`, `Session`）
2. `repository`（`UserRepository`）
3. `store`（`SessionStore`）
4. `service`（`CookieSessionAuthService`）
5. `controller`（`AuthController` のCookieルート）
6. `Main`（起動・配線）
7. Cookieシナリオ検証（`login -> me -> logout`）
8. `security`（`JwtProvider`）
9. `service`（`JwtAuthService`）
10. `store`（`JwtRevocationStore` は任意）
11. `controller`（JWTルート追加）
12. JWTシナリオ検証

完了条件:
- 実行フローと実装順の違いを説明できる

---

## Step 9: コンパイル・起動手順を固定する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/13-stateful-vs-stateless
javac -cp "lib/*:src" $(find src -name "*.java")
java -cp "lib/*:src" Main
```

完了条件:
- サーバーが `http://127.0.0.1:8080` で待ち受ける

---

## Step 10: セッションの用途と呼び出し順を固定する

セッションの用途:
1. ログイン済み状態をサーバー側で保持するために使う
2. `sid` から「誰のセッションか」「期限切れか」を判定するために使う
3. `logout` 時にセッションを削除して即時失効させるために使う

呼び出し順（Cookie Session）:
1. `POST /cookie/login`
2. `AuthController` が `CookieSessionAuthService.login(...)` を呼ぶ
3. `UserRepository.findByCredentials(...)` で認証
4. 成功時に `SessionStore.save(sid, session)` を呼ぶ
5. `Set-Cookie: sid=...` を返す
6. `GET /cookie/me` で `sid` を受ける
7. `SessionStore.find(sid)` でセッション参照
8. `isExpired(...)` で期限判定
9. `POST /cookie/logout` で `SessionStore.delete(sid)` を実行

完了条件:
- 「セッションは何のために存在するか」を1分で説明できる
- `login -> save -> find -> delete` の順序を説明できる

---

## Step 11: 検証シナリオを先に固定する

### 11-1. Cookie Session検証
```bash
# login（Cookie保存）
curl -i -X POST http://127.0.0.1:8080/cookie/login \
  -H "Content-Type: application/json" \
  -d '{"id":"demo","password":"password123"}' \
  -c /tmp/cookie13.txt

# me（Cookieで認証）
curl -i http://127.0.0.1:8080/cookie/me -b /tmp/cookie13.txt

# logout
curl -i -X POST http://127.0.0.1:8080/cookie/logout -b /tmp/cookie13.txt

# 再アクセス（401期待）
curl -i http://127.0.0.1:8080/cookie/me -b /tmp/cookie13.txt
```

### 11-2. JWT Bearer検証
```bash
# login（token取得）
TOKEN=$(curl -s -X POST http://127.0.0.1:8080/jwt/login \
  -H "Content-Type: application/json" \
  -d '{"id":"demo","password":"password123"}' | jq -r '.accessToken')

# me（Bearerで認証）
curl -i http://127.0.0.1:8080/jwt/me -H "Authorization: Bearer ${TOKEN}"

# logout（最小実装ではクライアント破棄扱い）
curl -i -X POST http://127.0.0.1:8080/jwt/logout -H "Authorization: Bearer ${TOKEN}"

# 再アクセス（最小実装では有効のままになる可能性を確認）
curl -i http://127.0.0.1:8080/jwt/me -H "Authorization: Bearer ${TOKEN}"
```

### 11-3. 失敗系共通
- 未送信（Cookieなし/Bearerなし）で `401`
- 改ざんトークンで `401`
- 期限切れトークンで `401`

完了条件:
- 正常系/失敗系の実行結果を再現できる

---

## Step 12: 比較結果を成果物として残す

やること:
- 比較結果を `README.md` または手元メモに記録する
- 最低限、次の5項目を埋める

比較項目:
1. サーバー状態参照の要否
2. logout後の即時失効性
3. スケール時の難易度
4. クライアント実装の単純さ
5. 主なリスク（XSS観点）

完了条件:
- 「要件AならCookie Session / 要件BならJWT Bearer」と選定理由を書ける

---

## Step 13: この観点を軸に記事作成と実装を進める

記事と実装の進め方（この章の原則）:
1. 先に実装して事実（挙動差）を作る
2. 次に比較表で根拠を整理する
3. 最後に記事で「混同を解消し、選定理由を説明できる状態」をゴールとしてまとめる

記事で必ず含める要素:
- 認証方式と状態管理方式の2軸整理
- 同一要件での実装差分（Cookie Session / JWT Bearer）
- 選定理由のテンプレート（要件 -> 根拠 -> 結論）

---
