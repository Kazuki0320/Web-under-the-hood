# 14-auth0-oidc-minimal / SETUP_STEPS

## 目的
Auth0 の最小構成をローカルで通し、OIDC認可コードフロー（Authorization Code Flow）を実装ベースで説明できる状態にする。

## ゴール
- ローカルで `ログイン -> コールバック -> トークン取得` が動く
- `ID Token / Access Token` を取得して `iss / aud / exp` を確認できる
- Access Token 検証（署名・期限）まで実装して説明できる

## 事前に知っておくこと（UI/ライブラリ）

- **Auth0ダッシュボード**:
  - Auth0の管理画面（Web UI）
  - URL例: `https://manage.auth0.com/`
  - ここでアプリ作成、Callback URL設定、ログ確認を行う

- **自前実装かSDK利用か**:
  - 学習目的でフロー理解を優先するなら「自前で `/login` `/callback` 実装」
  - 早く動かすなら公式SDK利用（Node/Java/Spring など）

- **よく使う公式SDK/ライブラリ例**:
  - `auth0-spa-js`（SPA）
  - `express-openid-connect`（Node + Express）
  - `auth0-java` / Spring Security OIDC連携（Java）

---

## Step 1: Auth0アプリを作成する

やること:
- Auth0ダッシュボードで `Applications` -> `Create Application`
- 種別は `Regular Web Application` を選択

UI操作の目安:
1. 左メニュー `Applications`
2. `Applications` 一覧へ移動
3. `Create Application` をクリック
4. App Name を入力
5. Application Type で `Regular Web Application` を選択
6. `Create`

記録する値:
- Domain
- Client ID
- Client Secret

補足:
- `Domain`: `xxxxx.us.auth0.com` の形式
- `Client ID/Secret`: アプリ設定画面で確認

完了条件:
- `Domain / Client ID / Client Secret` を取得できている

---

## Step 2: Callback / Logout URL を設定する

やること:
- `Allowed Callback URLs` にローカルURLを追加
  - 例: `http://localhost:8080/callback`
- `Allowed Logout URLs` にローカルURLを追加
  - 例: `http://localhost:8080`
- 必要に応じて `Allowed Web Origins` も設定

UI操作の目安:
1. 対象アプリを開く
2. `Settings` タブを開く
3. 以下を入力して保存
   - `Allowed Callback URLs`
   - `Allowed Logout URLs`
   - （必要なら）`Allowed Web Origins`

完了条件:
- ローカルURLが保存され、Auth0側のバリデーションエラーが出ない

---

## Step 3: ローカル最小アプリを用意する

やること:
- `GET /login` と `GET /callback` を持つ最小サーバーを作る
- `/login` でAuth0認可エンドポイントへリダイレクトする

SDK利用で進める場合（参考）:
- Node/Expressなら `express-openid-connect` を使うと、`/login` `/callback` を短く構築できる
- Java/Springなら Spring Security の OAuth2 Client を使うと、認可コードフローの配線を標準で持てる
- 学習用途では、最初にSDKで成功体験を作り、その後に自前実装へ落とし込む進め方でもよい

実装メモ:
- 認可リクエストに含める主要パラメータ
  - `response_type=code`
  - `client_id`
  - `redirect_uri`
  - `scope=openid profile email`
  - `state`

完了条件:
- `/login` へアクセスするとAuth0ログイン画面へ遷移する

---

## Step 4: コールバックで認可コードを受け取る

やること:
- `/callback` で `code` と `state` を受け取る
- `state` を検証する（CSRF対策）

完了条件:
- `code` をサーバー側で受け取れる

つまずきやすい点:
- Callback URL不一致（Auth0設定値と完全一致しているか）
- `http` / `https` やポート番号の違い
- `state` 未検証（CSRF対策不足）

---

## Step 5: トークン交換を実装する

やること:
- Auth0のトークンエンドポイントへ `POST` して交換
- 取得対象:
  - `id_token`
  - `access_token`
  - `token_type`
  - `expires_in`

完了条件:
- `/callback` 処理でトークン取得に成功する

つまずきやすい点:
- `client_id` / `client_secret` の取り違え
- `redirect_uri` が認可リクエスト時と一致していない
- Token Endpoint URLの打ち間違い

---

## Step 6: Tokenクレームを確認する

やること:
- `ID Token` または `Access Token` のクレームを確認する
- 最低限チェックする項目:
  - `iss`
  - `aud`
  - `exp`

完了条件:
- `iss / aud / exp` を説明できる

---

## Step 7: Access Token 検証を実装する

やること:
- 署名検証と期限検証を実装する
- 検証失敗時のエラーハンドリングを追加する

完了条件:
- 正常トークンは通過し、不正/期限切れトークンは拒否できる

補足:
- SDK使用時でも「何を検証しているか（iss/aud/exp/署名）」は必ず言語化する
- 目的は「動かす」だけでなく「説明できる」状態にすること

---

## Step 8: 動作確認シナリオを実施する

確認項目:
- 正常系: `login -> callback -> token取得 -> 検証成功`
- 失敗系:
  - `state` 不一致
  - 改ざんトークン
  - 期限切れトークン

完了条件:
- 正常系/失敗系の挙動差を説明できる

---

## Step 9: READMEに結果を残す

やること:
- 実行日、使用URL、設定値（秘匿情報は除く）を記録
- 詰まったポイントと解決方法を記録
- 最後に「何が説明できるようになったか」を3行でまとめる

完了条件:
- `README.md` に実装・検証ログが残っている
