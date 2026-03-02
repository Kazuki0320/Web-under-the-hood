# LEARNING_TASKS

このファイルは、今後やることリストをまとめた専用ノートです。

## 今後学ぶリスト

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
