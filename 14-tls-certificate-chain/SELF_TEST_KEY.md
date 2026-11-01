# 14 練習シート 参考解答

`SELF_TEST_TEMPLATE.md` を全部埋めてから開く。Part B は自分の言葉で同じ意味が書けていれば正解。

---

## Part A

| 空欄 | 解答 | 理由 |
|---|---|---|
| ① | `SSLSocketFactory` | `getDefault()` は親の型（`SocketFactory`）で返るため、TLS用の型にキャストする |
| ② | `getDefault` | Java標準設定（デフォルトのトラストストア等）のファクトリを取得する |
| ③ | `createSocket` | ソケットを作り、TCP接続まで行う |
| ④ | `getSSLParameters` | 今の設定を取り出す |
| ⑤ | `HTTPS` | HTTPS と同じ規則でホスト名を検証する |
| ⑥ | `setSSLParameters` | 変更した設定をソケットに戻す（戻さないと効かない） |
| ⑦ | `startHandshake` | TLSハンドシェイク。ここで証明書が届き検証される |
| ⑧ | `getSession` | ハンドシェイク結果（接続情報）を取得する |
| ⑨ | `getProtocol` | 例: `TLSv1.3` |
| ⑩ | `getCipherSuite` | 例: `TLS_AES_256_GCM_SHA384` |
| ⑪ | `Certificate` | `java.security.cert.Certificate`（証明書全般の型） |
| ⑫ | `getPeerCertificates` | 相手（サーバー）から届いた証明書チェーン |
| ⑬ | `X509Certificate` | Subject などの詳しい情報を使うためにキャストする |

### A-2: import

```java
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
```

---

## Part B

- Q1: 自分が作るのはクライアント（ブラウザと同じ立場）。相手側は example.com のサーバー。
- Q2: TCP接続は終わっている。TLSハンドシェイクと証明書の受け取りはまだ。
- Q3: `getDefault()` / `createSocket()` は汎用の親の型で返るので、TLS専用のメソッド（`getSSLParameters` など）を使うために子の型として扱う必要がある。
- Q4: ブロックを抜けるとソケットが自動で閉じられる（`close()` が呼ばれる）。`return` や例外で抜けても閉じられる。
- Q5: ハンドシェイク中に検証されるので、その前に設定しておく必要がある。書かないと、`SSLSocket` を直接使う場合はホスト名検証が行われず、別のサイト向けの証明書でも通ってしまう。
- Q6: 有効期限、署名、ホスト名、トラストストアのルートCAまでつながるか。
- Q7: `[0]` はサーバー自身の証明書。後ろほど発行元（上位のCA）。
- Q8: `[0]` の Issuer と `[1]` の Subject が一致する（`[1]` が `[0]` を発行・署名した）。
- Q9: クライアントは自分のトラストストアにあるルートCAしか信用しないので、サーバーから送っても意味が薄い（TLS 1.3 の仕様でも省略してよいとされる）。Javaはトラストストア（`cacerts`）で確かめている。
- Q10: 接続（SSLSession）から取る。TLSバージョンと暗号スイートは、そのハンドシェイクで決まった「接続の条件」で、証明書の中身ではないため。

---

## Part C（予想される結果。実際の表示は実行して確認する）

- Step 2 を消す: example.com では通常どおり成功する。違いが出るのは、証明書の名前と接続先が食い違うとき（そのときでも通ってしまう）。
- `startHandshake()` を消す: `getSession()` の呼び出し時にハンドシェイクが自動で行われるため、多くの場合は動く（暗黙のハンドシェイク）。※要実行確認
- 存在しないホスト: `createSocket` の時点で `UnknownHostException`（名前解決の失敗。TLSより前の段階）。
- ポート 80: 相手はTLSを話さないので、ハンドシェイクで `SSLException` 系の例外になる（メッセージは実行して確認）。
