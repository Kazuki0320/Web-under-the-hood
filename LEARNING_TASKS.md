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
