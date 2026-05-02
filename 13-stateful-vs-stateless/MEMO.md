# Chapter 13 Memo

## 2026-05-02 学びメモ（実装と理解）

1. `POST /cookie/login` の役割
- `id/password` を受け取り、認証成功時にセッションを作成する。
- `sid` を発行し、`Set-Cookie` で返す。

2. `GET /cookie/me` の役割
- `Cookie: sid=...` を受け取り、現在ログイン中のユーザー情報を返す。
- `sid` が無効・期限切れなら `401`。

3. JWTはAuth0専用ではない
- JWTは標準仕様（RFC 7519）。
- 今回は Auth0 社の `java-jwt` ライブラリで実装している。

4. JWTの3ブロック（色分け）の意味
- 1ブロック目: Header（例: `alg`, `typ`）
- 2ブロック目: Payload（例: `iss`, `sub`, `iat`, `exp`）
- 3ブロック目: Signature（改ざん検知用）

5. `ey` で始まる理由
- Base64URLエンコードされたJSONで、`{"...` がエンコードされると先頭が `ey...` になりやすい。

6. 署名（Signature）の意味
- 暗号化ではなく、完全性（改ざんされていないこと）を確認するための値。
- 検証時は `header.payload` を同じ `SECRET` で再署名して比較する。

7. JWTは暗号化されているわけではない
- Header/Payloadはデコードして読める。
- 保護しているのは「改ざん検知」であって「秘匿」ではない。

8. `Invalid Signature` の意味
- JWTの形式は正しいが、検証に使ったシークレットが発行時の `SECRET` と一致していない。

9. 署名をDB保存するか
- 通常は保存しない。
- リクエストごとに署名検証を実施する。
- 例外的に失効管理（ブラックリスト）用途で `jti` 等を保存することはある。

10. `SECRET` の保存先
- 通常はサーバー側の環境変数やSecrets Manager。
- ソースコード直書き・Git管理は避ける。

11. クライアントでのJWT保持先
- 代表例: `HttpOnly Cookie` またはメモリ保持。
- `localStorage` はXSS時の窃取リスクがあるため慎重に扱う。

12. XSS観点のリスク
- 悪意あるスクリプト実行でトークンが盗まれ、なりすましに使われる。

13. CSRF対策について（最後のやり取り）
- `HttpOnly` だけではCSRF対策にならない。
- CSRF対策は `SameSite`、CSRFトークン、`Origin/Referer` チェック等を組み合わせる。
- 整理:
  - XSS対策: `HttpOnly` が有効
  - CSRF対策: `SameSite` + CSRFトークン等が必要

