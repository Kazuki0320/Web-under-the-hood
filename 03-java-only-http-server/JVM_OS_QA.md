# JVM / OS 観測Q&A（03-java-only-http-server）

このファイルは、`SETUP_STEPS.md` で出た「周辺知識」の質問をまとめた補助ノートです。  
実装手順とは分けて、仕組み理解と調査観点に集中します。

## 1. `Main.java` は本当にサーバーになっている？

はい。`new ServerSocket(8080)` を実行すると、OS上で `8080` が `LISTEN` 状態になります。  
`while (true) { server.accept(); }` で接続待ちを繰り返すため、Javaプロセス自体がサーバーとして動作します。

確認コマンド:
```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

## 2. ループバックとは？

ループバックは「自分のPCに戻る仮想ネットワーク経路」です。

- IPv4: `127.0.0.1`
- IPv6: `::1`
- `localhost` は通常このどちらかに解決される

`curl http://localhost:8080` は外部ネットワークに出ず、同一マシン内で通信します。

## 3. なぜ `curl` から `Main.java` に通信できる？

流れ:
1. Javaが `ServerSocket(8080)` で待ち受ける
2. `curl` が `localhost:8080` に `connect()` する
3. OSのTCPスタックが同一マシン内の待ち受けソケットに接続を渡す
4. Javaの `accept()` が接続を受け取る

Step 2ではレスポンスを書かないため、`curl` 側は `Empty reply from server` になります。

## 3-1. 何が起きているか（ポート占有 -> `curl` 呼び出し）

1. `java -cp src Main` を実行すると、JVMが `Main` クラスを読み込む。
2. `new ServerSocket(8080)` でJavaがOSに「ソケット作成」「`bind(8080)`」「`listen`」を依頼する。
3. OSはそのプロセスに `8080` を割り当て、`LISTEN` 状態にする（ポート占有）。
4. Javaは `accept()` で接続待ちし、接続が来るまでブロックする。
5. `curl -v http://localhost:8080` を実行すると、`localhost` が `::1`（または `127.0.0.1`）に解決される。
6. `curl` 側OSはクライアント用の一時ポート（例: `52201`）を選び、`::1:8080` へ `connect()` する。
7. TCP 3ウェイハンドシェイク（`SYN -> SYN/ACK -> ACK`）が行われ、接続は `ESTABLISHED` になる。
8. サーバー側OSは接続済みソケットを `accept` キューに入れる。
9. `accept()` が解除され、Javaに `Socket client` が返る。
10. `client.getRemoteSocketAddress()` は相手情報を返し、`[::1]:52201` のように表示される。
11. `curl` はHTTPリクエスト行とヘッダー（例: `GET / HTTP/1.1`）を送信する。
12. Step 2 のコードはレスポンスを書かないため、`curl` は `Empty reply from server` になる。
13. `try (Socket client = ...)` を抜けると、その接続用ソケットだけ閉じる。
14. `while (true)` によりサーバーは再び `accept()` に戻り、`8080` の `LISTEN` は継続する。
15. Javaプロセス終了（`Ctrl + C`）または `ServerSocket.close()` で `8080` の占有が解放される。

## 3-2. ソケット通信の基本

- ソケットは「通信の端点」。
- サーバー側には役割が2つある:
1. `ServerSocket`: 待ち受け専用（`LISTEN`）
2. `Socket`（`accept()` が返す）: その接続専用の送受信用
- TCPは「バイト列の連続」を運ぶだけで、メッセージ境界は保証しない。
- そのためHTTP側で「開始行・ヘッダー・空行・本文」のルールをアプリが守る必要がある。
- 再送・順序保証・輻輳制御はTCP（OSカーネル）の担当。
- Javaコードの主な責務は「いつ `accept/read/write/close` するか」を決めること。

## 4. 名前解決は誰がやる？（OS or JVM）

通常はOSの名前解決機構が担当します。  
Javaやcurlは基本的にOSのresolverを利用します。

- `localhost` は多くの環境で `/etc/hosts` から解決される
- JVMは解決結果をキャッシュする（`InetAddress` キャッシュ）

確認コマンド:
```bash
rg -n 'localhost' /etc/hosts
dscacheutil -q host -a name localhost
```

## 5. `cp` と `java -cp` は別物？

- `cp`（シェルコマンド）: ファイルコピー
- `java -cp` の `-cp`: `classpath` の指定（クラス探索パス）

`java -cp src Main` は「`src` から `Main.class` を探して実行する」だけで、コピーはしません。

## 6. `Accept: /[::1]:52201` の数字は何？

- `::1` はlocalhost（IPv6）
- `52201` はクライアント側の一時ポート（ephemeral port）

サーバー側固定ポート `8080` とは別です。接続ごとに変わるのが正常です。

## 7. `curl -v` の意味

`-v` は `--verbose` で、以下を表示します。

- 名前解決結果
- 接続先と接続成否
- 送信リクエスト行/ヘッダー
- 受信レスポンス行/ヘッダー
- 接続終了理由

HTTP学習では、実際の通信内容を確認するために有効です。

## 8. スレッドダンプ（`jstack`）の読み方（初心者向け）

最重要ポイントは `main` スレッドのスタックです。

例:
```text
at java.net.ServerSocket.accept(...)
at Main.main(Main.java:13)
```

意味:
- `Main.java:13` で `accept()` 待ち
- サーバーは停止していない
- 「接続待ち中」の正常状態

補足:
- `Reference Handler` や `Finalizer`、`CompilerThread` はJVMの内部スレッドで通常存在する
- `RUNNABLE` 表示でも、ネイティブI/O待機を含むことがある

## 9. 実務でどこに効く？

障害切り分けで有効です。

1. API遅延/タイムアウト: どこ待ちか（DB/外部API/ロック）を特定
2. CPU高騰: 無限ループやホットパスを特定
3. 503/処理詰まり: accept済みか、アプリ処理で詰まるかを分離
4. デッドロック: ロック競合箇所を特定
5. 停止しない問題: 生き残っているスレッドを特定

## 10. よく使う観測コマンド

```bash
# Javaプロセス
jps -l

# スレッドダンプ
jstack <pid>

# JVM情報
jcmd <pid> VM.version
jcmd <pid> VM.system_properties | rg 'java.net|os\\.|java\\.home'

# 8080待ち受け状態
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8080
```

## 11. `jstack` のよく使う使い方（最小）

1. PID確認:
```bash
jps -l
```

2. まず取得（基本）:
```bash
jstack <pid>
```

3. ロック情報つき（競合調査）:
```bash
jstack -l <pid>
```

4. 詳細つき（必要なときだけ）:
```bash
jstack -e <pid>
jstack -l -e <pid>
```

5. 調査ログとして保存:
```bash
jstack -l <pid> > jstack_$(date +%Y%m%d_%H%M%S).txt
```

## 用語メモ

- 輻輳制御: `ふくそうせいぎょ`
