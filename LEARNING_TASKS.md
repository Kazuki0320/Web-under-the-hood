# LEARNING_TASKS

このファイルは、今後やることリストをまとめた専用ノートです。

## 今後学ぶリスト

- Java HTTPサーバーをkeep-alive対応にする
  - 目的: 1回のTCP接続を再利用して複数リクエストを処理し、HTTP/1.1の接続管理を理解する
  - 最小ステップ:
    - 1接続1リクエストで閉じる実装を、同一ソケット内でのリクエスト読み取りループに変更する
    - `Connection: close` ヘッダーを受けた場合に接続を終了する
    - アイドルタイムアウトと最大リクエスト数を設定し、過剰な接続保持を防ぐ
    - `curl` もしくは `nc` で同一接続上の連続リクエストを検証する

- Java HTTPサーバーをスレッドプール対応にする
  - 目的: 接続ごとに`new Thread`する方式との違いを理解し、同時接続時の安定性と処理性能を改善する
  - 最小ステップ:
    - `ExecutorService`（固定サイズ）で接続処理を実行するように変更する
    - 同時リクエスト時に、スレッド数・待機キュー・応答時間の変化を観測する
    - プールサイズを変えて挙動比較し、IO主体ワークロードでの妥当な設定を考える
    - サーバー停止時に`shutdown`/`awaitTermination`で安全終了できるようにする

- Servlet Filter を触る
  - 目的: Servlet本体の前後に共通処理を差し込む責務分離を理解する
  - 最小ステップ:
    - 全リクエストのURLと処理時間をログ出力するFilterを作る
    - `/admin` の簡易認可チェック（未認可なら`401`）を作る
    - `/*` と `/api/*` で適用範囲を変えて挙動を比較する

- JVM観測を深掘りする
  - 目的: Javaアプリの遅延・詰まり・異常時に、JVMとOSの境界で原因を切り分けられるようにする
  - 最小ステップ:
    - `jps -l` で対象プロセスを特定し、`jstack <pid>` と `jstack -l <pid>` を読めるようにする
    - `Main.main -> ServerSocket.accept` の待機状態を説明できるようにする
    - `jcmd <pid> VM.version` / `jcmd <pid> VM.system_properties` で実行環境を確認する
    - `lsof -nP -iTCP:8080 -sTCP:LISTEN` と突き合わせて、JVM側とOS側の見え方を対応づける
    - `jcmd <pid> GC.heap_info` などの基本メトリクスを取り、メモリ観測の入口を作る

- Dockerネットワーク観測を行う（今後追加）
  - 目的: Docker特有の通信レイヤー（port mapping/NAT/bridge/サービス名解決）を切り分けて説明できるようにする
  - 最小ステップ:
    - `localhost -> Docker公開ポート -> 前段サーバー -> バックエンド` の経路を観測し、到達順序を確認する
    - `docker inspect` と `docker network inspect` でIP/ネットワーク接続を確認する
    - `ss` / `tcpdump` で待ち受けと実パケットを確認し、設定との差分を特定する
    - 障害パターン（サービス名不一致・network未参加・port不一致）を再現し、失敗ポイントを記録する

- CORS制約とプリフライトを最小構成で検証する
  - 目的: ブラウザの同一オリジン制約とCORSヘッダー、プリフライトの発生条件を実装ベースで理解する
  - 最小ステップ:
    - FE（`http://localhost:5173`）とBE（`http://localhost:3000`）の別オリジン最小アプリを作る
    - `GET /hello` でシンプルリクエストを通し、`Access-Control-Allow-Origin` の効果を確認する
    - `POST /echo` + `Content-Type: application/json` + `X-Test` でプリフライト（`OPTIONS`）発生を確認する
    - `Access-Control-Allow-Methods` / `Access-Control-Allow-Headers` / Origin不一致の失敗パターンを再現して記録する
    - `credentials: include` と `Access-Control-Allow-Credentials: true` の組み合わせを検証し、`Allow-Origin: *` と併用不可を確認する
    - 最後に「発生条件・必要ヘッダー・失敗時の見分け方」を1枚にまとめる

- TLS/SSL（HTTPS）とクライアント証明書認証（mTLS）を段階的に検証する
  - 目的: 平文HTTPとの違いを、証明書検証・ハンドシェイク・認証失敗の見え方まで含めて説明できるようにする
  - 最小ステップ:
    - 自己署名CAを作成し、サーバー証明書を発行して `https://localhost` の最小サーバーを起動する
    - `curl -v` で「証明書未信頼時の失敗」と「CAを信頼した時の成功」を比較する
    - 証明書のCN/SAN不一致・期限切れ・中間証明書欠落の失敗パターンを再現して記録する
    - mTLSを有効化し、クライアント証明書なし・不正証明書・正しい証明書で挙動を比較する
    - 最後に「誰を認証しているか（サーバー認証/クライアント認証）」「どこで失敗するか（TLS層/HTTP層）」を1枚にまとめる

## 次の勉強題材メモ（Build your own系）

- Webアプリのレイヤーを1段ずつ自作する流れで進める
  - ねらい: フレームワーク理解を深めるために、小さいフレームワークを自分で作る

- 推奨順序（現時点）
  - 1. Routerを作る（`URL -> Controller`、`Map<String, Handler>`）
  - 2. ミニMVC（Dispatcher -> Router -> Controller -> Service -> Repository）
  - 3. JSONシリアライザ（Java Object -> JSON）
  - 4. 自作DIコンテナ（Reflectionで依存注入）


- 参考学習スタイル
  - Build your own X 系プロジェクトを題材に、段階的に実装して理解を深める

## 今後進める順序（Step 1-7）

- Step 1: 06 keep-alive を完了する
  - ねらい: 接続再利用、終了条件（`Connection: close` / timeout / max requests）を実装と観測で説明できるようにする

- Step 2: Java HTTPサーバーをスレッドプール対応にする
  - ねらい: 同時接続時の安定性、待機キュー、スループットの関係を理解する

- Step 3: TLS/SSL（HTTPS）と mTLS を検証する
  - ねらい: サーバー認証とクライアント認証の違い、証明書エラー時の見え方を理解する

- Step 4: CORS制約とプリフライトを検証する
  - ねらい: ブラウザ制約とサーバーヘッダー設定の因果関係を説明できるようにする

- Step 5: JVM観測を深掘りする
  - ねらい: `jstack` / `jcmd` / `lsof` で「詰まり」「待機」「設定差分」を切り分ける

- Step 6: Dockerネットワーク観測を行う
  - ねらい: port mapping / NAT / bridge / サービス名解決の差分を観測で説明できるようにする

- Step 7: 自作ロードバランサ（最小版）に取り組む
  - ねらい: L4/L7の基本、バックエンド振り分け、ヘルスチェックの入口を理解する
