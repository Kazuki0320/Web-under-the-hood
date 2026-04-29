# 13 Stateful vs Stateless

`13` では、同一要件を **Stateful（Cookie Session）** と **Stateless（JWT Bearer）** の2方式で実装し、認証設計の違いを比較する。

## 目的
「同じ要件を Stateful（Cookie Session）と Stateless（JWT Bearer）で実装し、選定理由を説明できる状態になること」

- 認証方式の軸を分離して理解する
認証情報の運び方（Cookie / Authorizationヘッダー）と 状態保持（サーバー保存 / トークン自己完結）を混同しない。

- 実装差を体で理解する
同じ POST /login GET /me POST /logout を2方式で作り、コード上の責務差を把握する。

- セキュリティ差を比較できる
CSRF、XSS、盗難トークン再利用、失効（logout/revoke）の難易度を説明できる。

- 運用差を比較できる
スケール時の構成、即時失効のしやすさ、監視しやすさを比較できる。

- 設計判断を言語化できる
「この要件ならCookie Session」「この要件ならJWT Bearer」と根拠付きで言える。

## 最小ゴール
- Cookie Session 方式で `POST /login` / `GET /me` / `POST /logout` を実装できる
- JWT Bearer 方式で `POST /login` / `GET /me` / `POST /logout`（擬似失効含む）を実装できる
- 両方式の失敗系（未送信・期限切れ・改ざん・ログアウト後再利用）を `curl` で再現できる
- 比較表（セキュリティ、運用性、拡張性、性能）を1枚にまとめられる

## 比較対象（今回の前提）
- Stateful: Cookie Session（`sid -> session` をサーバー側Storeで管理）
- Stateless: JWT Bearer（署名検証で自己完結）

## 実装ステップ（この順番で進める）

Step 1: 共通の最小API境界を作る
- `POST /login`
- `GET /me`
- `POST /logout`

Step 2: Cookie Session 版を実装する
- ログイン成功時に `Set-Cookie: sid=...; HttpOnly; SameSite=Lax` を返す
- `GET /me` は `Cookie` から `sid` を読み、Session Storeを参照して認証する
- `POST /logout` はStoreから `sid` を削除して失効させる

Step 3: JWT Bearer 版を実装する
- ログイン成功時に署名付きJWTを返す
- `GET /me` は `Authorization: Bearer <token>` の署名・期限を検証する
- `POST /logout` は最小実装ではクライアント破棄、発展版でdenylist運用を検討する

Step 4: 失敗系を再現する
- 未送信（Cookieなし / Bearerなし）
- 期限切れ
- 改ざんトークン
- ログアウト後再利用

Step 5: 比較結果を整理する
- 認証情報の保存場所
- CSRF/XSS観点の注意点
- 失効のしやすさ
- マルチデバイス運用
- スケール時の設計（集中Store有無）

## 完了条件
- 同一の機能要件を2方式で実装し、挙動差を説明できる
- 主要な失敗パターンを再現ログ付きで示せる
- 「どの要件ならどちらを選ぶか」を根拠付きで説明できる



