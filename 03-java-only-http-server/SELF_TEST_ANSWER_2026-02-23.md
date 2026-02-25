# 03 Java Only HTTP Server: 理解度テストテンプレート

使い方:
- 各設問に対して、自分の言葉で回答を書く。
- 不明点は `要復習` にチェックして、最後に見直す。

---

## 回答者情報

- 日付:2026/02/23
- 所要時間:15分
- 目標スコア（自己評価）:
10点（100点満点中）

---

## 設問（15問）

### Q1
`new ServerSocket(8080)` を実行すると、JavaとOSでそれぞれ何が起きる？

- 回答:
Java:
- ソケット通信用のインスタンスを作成

OS:
- ソケット通信するクライアントIPとソケットをbindする？
- 要復習: [Check]

Answer:
`ServerSocket生成時に、JVMがOSに対してsocket/bind/listenを依頼して、8080をLISTEN状態にする`

### Q2
`bind` とは何か。`listen` とは何か。違いを説明してください。
- 回答:
bind:
- ソケットにポートをbindすること

listen:
- ソケットがリクエストを待っている状態

- 要復習: [ ]

### Q3
`accept()` が返す `Socket` と `ServerSocket` の役割の違いは？

- 回答:
Socket:
- 

ServerSocket:

- 要復習: [Check]

### Q4
`curl -v http://localhost:8080/hello` で、`GET /hello HTTP/1.1` は誰が作っている？

- 回答:
- curlコマンド側
詳細な回答:`curlがURlを解析して、HTTP request lineを組み立てている`

- 要復習: [ ]

### Q5
`Host: localhost:8080` ヘッダーが必要な理由は？

- 回答:localhostに紐づくポートを指定することで、それを読み取れる
詳細な回答：`Hostヘッダーは、どのホストのHTTP要求であるかを認識するために使う`
- 要復習: [ ]

### Q6
`BufferedReader` と `requestLine` は、それぞれ何のために使っている？

- 回答:
BufferedReader:
- curlのヘッダーを読み込んでいる

requestLine:
- リクエストで送られてきたURLの文字列を分析

- 要復習: [ ]

### Q7
なぜ `\r\n` を使う必要がある？ `\n` だけでは何が問題になる？

- 回答:
\rってなんだっっけーな
- 要復習: [check]

### Q8
`Content-Type` の役割を説明し、`text/plain` と `application/json` の違いを書いてください。

- 回答:
Content-Type:
- 本文のメディアタイプを示し、クライアントの解釈方法を決める

text/plain:
- 文章を返す

application/json:
- json形式で返す

- 要復習: [ ]

### Q9
`Content-Length` が不正なときに起こり得る不具合は？

- 回答:
指定されているContent-Typeではない場合、404を返す

- 要復習: [ ]

### Q10
`Connection: close` を返している理由は？ 学習上のメリットは？

- 回答:
1プロセスの接続終了を伝える。
学習上のメリットはわからない。

- 要復習: [ ]

### Q11
`Connection reset by peer`（RST）は何を意味する？

- 回答:
レスポンスがまだない状態で、サーバーからレスポンスが返ってこなかったことを意味する
TCPによる接続自体はできている

- 要復習: [ ]

### Q12
`Accepted: /[::1]:63183` の `::1` と `63183` は何を表す？

- 回答:
::1は、Ipv6のIPとそれに紐づくポート番号
- 要復習: [ ]

### Q13
サーバーの待受ポート（例:8080）とクライアント一時ポートの両方が必要な理由は？

- 回答:
クライアントポートは、リクエストした後に返ってくるデータの帰り道を確保するために必要で、サーバー側は、そのポートを通してデータを取得する。

- 要復習: [ ]

### Q14
Step 5 のルーティング条件（`/`, `/hello`, `/api/hello`, 404）を口頭で説明してください。

- 回答:
/:"/"では、GET リクエストが/であることで、この条件に入り、status=200を返し、contentTypeではtext/htmlとして、responseBodyには、Welcomeを返す
/hello:"/hello"は、200 OKを返し、contentTypeは、text/plainを返すので文字列を返す。responseBodyには、Hello Worldを含める。
/api/hello:200 OKを返し、contentTypeは、application/jsonとしてjson形式。responseBodyには、messageを返す。
404:Not Foundを返す

- 要復習: [ ]

### Q15
Step 6 の観点で、`status` / `Content-Type` / `Content-Length` をどう検証するか書いてください。

- 回答:
curl -v http://localhost:8080に、指定のない文字列を含めてレスポンスの中身がどう返ってくるかで検証する。

- 要復習: [ ]

---

## 最終自己評価

- 理解できた点:
理解ができている点が現状かなり少ない。わかった部分としては、各リクエストによって返り値が異なるという点。
ネットワークやOS、JVMに関してはまだまだ理解が浅い印象。

- あいまいな点:

- 次に復習するファイル:
  - `SETUP_STEPS.md`
  - `JVM_OS_QA.md`
