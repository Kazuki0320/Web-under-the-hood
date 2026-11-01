# 14 TLS Certificate Chain: 練習シート（穴埋め＋理解度テスト）

使い方:
- Part A は、コードを見ずに `【①】` などの空欄を埋める。埋めたら実際に `src` に書いて動かす。
- Part B は、自分の言葉で回答を書く。
- 不明点は `要復習` にチェックして、最後に見直す。
- 答え合わせは `SELF_TEST_KEY.md`（全部埋めてから開く）。
- 回答は、このファイルをコピーして `SELF_TEST_ANSWER_YYYY-MM-DD.md` に書く。

---

## 回答者情報

- 日付:
- 所要時間:
- 目標スコア（自己評価）:

---

## Part A: 穴埋め（fetchCertificateChain）

ヒント: 型をたどる。各行は「前の行が返した型の、次のメソッド」を呼んでいるだけ。
`SSLSocketFactory` → `SSLSocket` → `SSLSession` → `Certificate[]` → `X509Certificate`

```java
static X509Certificate[] fetchCertificateChain(String host, int port) throws Exception {
    // Step 1: TCP接続
    SSLSocketFactory factory = (【①】) SSLSocketFactory.【②】();
    try (SSLSocket socket = (SSLSocket) factory.【③】(host, port)) {
        System.out.println("TCP接続: " + socket.isConnected());

        // Step 2: ホスト名検証をオンにする
        SSLParameters parameters = socket.【④】();
        parameters.setEndpointIdentificationAlgorithm("【⑤】");
        socket.【⑥】(parameters);

        // Step 3: TLSハンドシェイク
        socket.【⑦】();

        // Step 4: 接続情報と証明書チェーンを取り出す
        SSLSession session = socket.【⑧】();
        System.out.println("TLS: " + session.【⑨】());
        System.out.println("暗号スイート: " + session.【⑩】());

        【⑪】[] certificates = session.【⑫】();
        X509Certificate[] chain = new X509Certificate[certificates.length];
        for (int i = 0; i < certificates.length; i++) {
            chain[i] = (【⑬】) certificates[i];
        }
        return chain;
    }
}
```

| 空欄 | 回答 | 要復習 |
|---|---|---|
| ① |  | [ ] |
| ② |  | [ ] |
| ③ |  | [ ] |
| ④ |  | [ ] |
| ⑤ |  | [ ] |
| ⑥ |  | [ ] |
| ⑦ |  | [ ] |
| ⑧ |  | [ ] |
| ⑨ |  | [ ] |
| ⑩ |  | [ ] |
| ⑪ |  | [ ] |
| ⑫ |  | [ ] |
| ⑬ |  | [ ] |

### A-2: import を書く
上のコードを動かすために必要な import を、すべて書いてください（6行）。

- 回答:
- 要復習: [ ]

---

## Part B: 理解度テスト（10問）

### Q1
このプログラムで「自分が作る側」はクライアントとサーバーのどちら？ 相手側は何？

- 回答:
- 要復習: [ ]

### Q2
`createSocket(host, port)` を呼んだ時点で、何が終わっていて、何がまだ起きていない？

- 回答:
- 要復習: [ ]

### Q3
`(SSLSocketFactory)` や `(SSLSocket)` のキャストが必要なのはなぜ？

- 回答:
- 要復習: [ ]

### Q4
`try (SSLSocket socket = ...) { }` と書くと、何が自動で行われる？ `return` で抜けた場合は？

- 回答:
- 要復習: [ ]

### Q5
Step 2（`setEndpointIdentificationAlgorithm("HTTPS")`）を Step 3 より前に書く理由は？ 書かなかったらどうなる？

- 回答:
- 要復習: [ ]

### Q6
`startHandshake()` の内部で JSSE が確認している4項目は？

- 回答:
- 要復習: [ ]

### Q7
`getPeerCertificates()` が返す配列の `[0]` は何？ 後ろの要素ほど何になる？

- 回答:
- 要復習: [ ]

### Q8
`[0]` の Issuer と `[1]` の Subject は、どんな関係になっている？

- 回答:
- 要復習: [ ]

### Q9
ルートCA証明書が配列に入っていないのはなぜ？ ではJavaはどこでルートCAを確かめている？

- 回答:
- 要復習: [ ]

### Q10
TLSバージョンと暗号スイートは、証明書から取るのか、接続（SSLSession）から取るのか。理由も書いてください。

- 回答:
- 要復習: [ ]

---

## Part C: わざと壊して確かめる（実行して結果を書く）

| やること | 予想 | 実際の結果 |
|---|---|---|
| Step 2 の3行を消して実行する |  |  |
| `startHandshake()` を消して実行する |  |  |
| 存在しないホスト名（例: `no-such-host.invalid`）につなぐ |  |  |
| ポートを `80` にして実行する |  |  |

---

## 振り返り

- 一番詰まった空欄・設問:
- 次に復習すること:
