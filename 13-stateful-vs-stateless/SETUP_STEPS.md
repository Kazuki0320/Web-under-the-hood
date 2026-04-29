# Stateful vs Stateless 実装 手順書（13）

このファイルは、`13-stateful-vs-stateless` を順番に進めるための実行手順です。  
まずは実装前に「今回の目的」と「ステートレス/ステートフルが使われる部分」を固定します。

## Step 0: 今回の学習目的を固定する

やること:
- 同一要件を `Cookie Session`（Stateful）と `JWT Bearer`（Stateless）で実装し、差分を説明できる状態を目指す
- 「認証情報の運搬方法」と「認証状態の保持方法」を分けて整理する
- 認証方式と状態管理方式を混同せずに説明できるようにし、方式選定理由を根拠付きで説明できるようにする
- 失敗系（未送信・期限切れ・改ざん・ログアウト後再利用）の挙動差を比較する
- 最後に「どの要件ならどちらを選ぶか」を根拠付きで言語化する

この章で学ぶ到達目標:
1. `POST /login` / `GET /me` / `POST /logout` を2方式で実装できる
2. Stateful/Stateless の違いをコードと実行結果で説明できる
3. 認証方式と状態管理方式を分離して説明し、選定理由を根拠付きで説明できる
4. セキュリティと運用のトレードオフを比較表で示せる

完了条件:
- 「何を学ぶ章か」を3行で説明できる
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
3. XSS観点での注意点（CSRFは今回の対象外）
4. スケール時の構成難易度（集中Storeの要否）
5. クライアント実装のシンプルさ（ブラウザ/モバイル）

完了条件:
- 上記5軸で、実装後に方式比較できる準備ができている

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
- 「今回はやらないこと（CSRF）」が明文化されている

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

touch QA_MEMO_STATEFUL_STATELESS.md
touch AUTH_COMPARISON_TABLE.md
touch TEST_LOG.md
```

完了条件:
- 主要クラスとログファイルが揃っている

---

## Step 5: JWTライブラリを導入する

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

---

## Step 6: 認証仕様（I/F）を固定する

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

## Step 7: 実装順を固定する（依存が下位から上位）

推奨実装順:
1. `model`（`User`, `Session`）
2. `store`（`SessionStore`, `JwtRevocationStore`）
3. `repository`（`UserRepository`）
4. `security`（`JwtProvider`）
5. `service`（`CookieSessionAuthService`, `JwtAuthService`）
6. `controller`（`AuthController`）
7. `Main`（起動・配線）

完了条件:
- 実行フローと実装順の違いを説明できる

---

## Step 8: コンパイル・起動手順を固定する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/13-stateful-vs-stateless
javac -cp "lib/*:src" $(find src -name "*.java")
java -cp "lib/*:src" Main
```

完了条件:
- サーバーが `http://127.0.0.1:8080` で待ち受ける

---

## Step 9: 検証シナリオを先に固定する

### 9-1. Cookie Session検証
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

### 9-2. JWT Bearer検証
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

### 9-3. 失敗系共通
- 未送信（Cookieなし/Bearerなし）で `401`
- 改ざんトークンで `401`
- 期限切れトークンで `401`

完了条件:
- 正常系/失敗系ログを `TEST_LOG.md` に残せる

---

## Step 10: 比較結果を成果物として残す

やること:
- `AUTH_COMPARISON_TABLE.md` に比較結果を記録する
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

## Step 11: この観点を軸に記事作成と実装を進める

記事と実装の進め方（この章の原則）:
1. 先に実装して事実（挙動差）を作る
2. 次に比較表で根拠を整理する
3. 最後に記事で「混同を解消し、選定理由を説明できる状態」をゴールとしてまとめる

記事で必ず含める要素:
- 認証方式と状態管理方式の2軸整理
- 同一要件での実装差分（Cookie Session / JWT Bearer）
- 選定理由のテンプレート（要件 -> 根拠 -> 結論）

完了条件:
- 学習記録（SETUP/TEST_LOG/比較表）から記事へ一貫して説明できる
