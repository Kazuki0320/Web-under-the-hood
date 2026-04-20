# 認証フロー実装 手順書（12）

このファイルは、`12-build-auth-flow` を「設計先行」で進めるためのセットアップ手順です。  
今回は実装そのものよりも、認証フロー設計と学習ポイント整理を先に固めます。

## Step 0: 事前確認

やること:
- Java実行環境と`curl`が使えることを確認する
- 作業ディレクトリを `12-build-auth-flow` に合わせる

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/12-build-auth-flow
java -version
javac -version
which curl
```

完了条件:
- `java` / `javac` / `curl` が利用可能
- `12-build-auth-flow` 配下で作業開始できる

---

## Step 1: 今回の学習ゴールを固定する

やること:
- 今回の「到達目標」と「対象外」を先に決める
- 設計判断の優先順位を決める（動くこと > 説明できること > 拡張性）

この章の到達目標:
1. `POST /login` で認証成功時にトークン発行できる
2. `GET /me` でBearerトークン検証ができる
3. `401` の失敗理由（未送信/不正/期限切れ）を区別して説明できる
4. `Controller -> Service -> Repository` の責務分離を説明できる

今回の対象外（割り切る）:
- OAuth/OIDC準拠の完全実装
- 本番向けセキュリティ（署名付きJWT、鍵管理、リフレッシュトークン）
- 分散環境向けセッション共有

完了条件:
- 「何を学ぶか / 何をやらないか」を言語化できる

---

## Step 2: 設計を先に確定する（責務分離）

やること:
- レイヤ責務を明文化する
- 各レイヤ間の依存方向を固定する

推奨レイヤ構成:
- `Main`: サーバー起動と依存組み立て
- `Controller`: HTTP入出力（ヘッダー、JSON、ステータス）
- `Service`: 認証ロジック（資格情報確認、トークン発行/検証）
- `Repository`: データアクセス（ユーザー参照、トークン保存/参照）
- `Model`: `User` / `Session(Token)` などのドメイン表現

依存ルール:
- `Controller -> Service -> Repository`
- 下位レイヤは上位レイヤを知らない

完了条件:
- 各クラスの責務を1行で説明できる
- 依存方向を逆転させない方針が決まっている

---

## Step 3: API契約（I/F）を固定する

やること:
- 先にリクエスト/レスポンス形式を決める
- 正常系と異常系のHTTPステータスを固定する

API契約（最小）:
1. `POST /login`
   - 入力: `{"id":"...","password":"..."}`
   - 成功: `200` + `{"accessToken":"...","tokenType":"Bearer","expiresIn":...}`
   - 失敗: `400`（入力不備）/ `401`（認証失敗）/ `415`（Content-Type不正）
2. `GET /me`
   - 入力: `Authorization: Bearer <token>`
   - 成功: `200` + `{"id":"...","name":"..."}`
   - 失敗: `401`（未送信/不正/期限切れ）

完了条件:
- 各エンドポイントで「何を受けて何を返すか」が確定している

---

## Step 4: トークンライフサイクルを設計する

やること:
- トークンの状態遷移を決める
- 有効期限判定のタイミングを決める

最小ルール:
1. `login` 成功時にランダムトークン発行
2. `token -> userId + expiresAt` を保存
3. `GET /me` で `存在確認 -> 期限確認` の順で検証
4. 期限切れは `401` を返し、必要なら保存領域から削除

完了条件:
- トークンが「生成 -> 保存 -> 検証 -> 失効」する流れを説明できる

---

## Step 5: 実装前の最小ファイルだけ準備する

やること:
- 実装の土台だけ作る（中身はこれから自分で書く）
- フォルダ作成時に、質問メモ（QAメモ）も同時に作る

コマンド例:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/12-build-auth-flow
mkdir -p src/controller src/service src/repository src/model src/store
touch QA_MEMO_AUTH_FLOW.md
touch src/Main.java
touch src/controller/AuthController.java
touch src/service/AuthService.java
touch src/repository/UserRepository.java
touch src/store/TokenStore.java
touch src/model/User.java
touch src/model/Session.java
```

完了条件:
- レイヤごとの最小ファイルが揃っている
- `QA_MEMO_AUTH_FLOW.md` が作成されている
- 実装開始前に迷わない骨組みができている

---

## Step 6: まず `Main` に必要な実装を入れる

やること:
- `Main` を「サーバー起動専任」にする（ビジネスロジックは書かない）
- `ServerSocket` 待受ループと例外ハンドリングだけ先に作る
- `AuthController` 呼び出しの入口を作る（中身実装は後でOK）

`Main` で最低限必要なimport:
- `java.io.IOException`
- `java.net.ServerSocket`
- `java.net.Socket`

`Main` の実装チェックリスト:
1. `private static final int PORT = 8080;` を定数化する
2. `try (ServerSocket server = new ServerSocket(PORT))` で待受開始する
3. 起動ログを出す（例: `Server listening on port 8080`）
4. `while (true)` で `accept()` ループを作る
5. 各接続を `handleClient(Socket client, AuthController controller)` に委譲する
6. 接続処理失敗時に `System.err` へログを出す
7. 起動失敗時に `Failed to start server: ...` を出す

推奨メソッド構成（`Main` 内）:
- `main(String[] args)`: 起動・依存組み立て・acceptループ
- `createAuthController()`: `Repository -> Service -> Controller` を組み立てる
- `handleClient(Socket client, AuthController controller)`: 1リクエスト処理（詳細はControllerへ委譲）

`src/Main.java`（見本実装）:
```java
import controller.AuthController;
import repository.UserRepository;
import service.AuthService;
import store.TokenStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        AuthController controller = createAuthController();

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                try (Socket client = server.accept()) {
                    System.out.println("Accepted: " + client.getRemoteSocketAddress());
                    handleClient(client, controller);
                } catch (IOException e) {
                    System.err.println("Failed to handle client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private static AuthController createAuthController() {
        UserRepository userRepository = new UserRepository();
        TokenStore tokenStore = new TokenStore();
        AuthService authService = new AuthService(userRepository, tokenStore);
        return new AuthController(authService);
    }

    private static void handleClient(Socket client, AuthController controller) throws IOException {
        controller.handle(client);
    }
}
```

補足（この見本を動かすための最小シグネチャ）:
- `AuthController(AuthService authService)`
- `void handle(Socket client) throws IOException`
- `AuthService(UserRepository userRepository, TokenStore tokenStore)`

完了条件:
- `Main` だけで「起動できる」「接続を受け取れる」「Controllerへ渡せる」状態になっている
- `Main` に認証ロジックを直接書いていない

---

## この章で押さえる学習ポイント

1. 認証と認可の違い（今回は認証中心）
2. HTTP境界（Controller）と業務ロジック（Service）の分離
3. トークンの状態管理（存在・期限・失効）
4. エラーの粒度（`400`/`401`/`415` の使い分け）
5. 「実装できる」だけでなく「フローを説明できる」状態にすること
