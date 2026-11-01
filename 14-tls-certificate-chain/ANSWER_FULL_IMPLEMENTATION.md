# 14 完成版の実装（Step 1〜6 すべて）

`src` の3ファイルを、このまま置き換えれば完成する形。こちらでコンパイルが通ること、`TrustAnchorFinder` がトラストストアからルートCAを見つけられることは確認済み。example.com への実際の接続は未確認（こちらの環境の制限）。

## 全体の流れ

```text
main
 ├─ fetchCertificateChain()      … Step 1〜4: 接続して X509Certificate[] を受け取る
 ├─ CertificatePrinter.print()   … Step 5: 1枚ずつ表示する
 ├─ [i] の Issuer と [i+1] の Subject を比べる … チェーンのつながりを確認
 └─ TrustAnchorFinder.find(top)  … Step 6: 一番上の証明書から、cacerts のルートCAを探す
```

## 1. TlsCertificateInspector.java（全体）

```java
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class TlsCertificateInspector {
    private static final String DEFAULT_HOST = "example.com";
    private static final int DEFAULT_PORT = 443;

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        System.out.println("接続先: " + host + ":" + port);

        // Step 1〜4: 接続して証明書チェーンを受け取る
        X509Certificate[] chain = fetchCertificateChain(host, port);

        // Step 5: 1枚ずつ表示する
        for (int i = 0; i < chain.length; i++) {
            CertificatePrinter.print(i, chain[i]);
        }

        // Step 5（確認）: [i] の Issuer と [i+1] の Subject がつながっているか
        System.out.println();
        for (int i = 0; i < chain.length - 1; i++) {
            boolean linked = chain[i].getIssuerX500Principal()
                    .equals(chain[i + 1].getSubjectX500Principal());
            System.out.println("[" + i + "] の Issuer と [" + (i + 1) + "] の Subject: "
                    + (linked ? "一致（つながっている）" : "不一致"));
        }

        // Step 6: チェーンの一番上の証明書から、Javaが信頼するルートCAを探す
        X509Certificate top = chain[chain.length - 1];
        Optional<X509Certificate> root = TrustAnchorFinder.find(top);

        System.out.println();
        if (root.isPresent()) {
            System.out.println("[Trust Anchor] Javaが信頼しているルートCA");
            System.out.println("Subject: " + root.get().getSubjectX500Principal());
        } else {
            System.out.println("[Trust Anchor] トラストストアに一致するルートCAが見つからない");
        }
    }

    static X509Certificate[] fetchCertificateChain(String host, int port) throws Exception {
        // Step 1: TCP接続（ソケットを作った時点でつながる）
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            System.out.println("TCP接続: " + socket.isConnected());

            // Step 2: ホスト名検証をオンにする
            SSLParameters parameters = socket.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(parameters);

            // Step 3: TLSハンドシェイク（証明書が届き、Javaが検証する）
            socket.startHandshake();

            // Step 4: 接続情報と証明書チェーンを取り出す
            SSLSession session = socket.getSession();
            System.out.println("TLS: " + session.getProtocol());
            System.out.println("暗号スイート: " + session.getCipherSuite());

            Certificate[] certificates = session.getPeerCertificates();
            X509Certificate[] chain = new X509Certificate[certificates.length];
            for (int i = 0; i < certificates.length; i++) {
                chain[i] = (X509Certificate) certificates[i];
            }
            return chain;
        }
    }
}
```

ポイント:
- `main` は「順番に呼ぶだけ」。表示とルートCA探索は他のクラスに任せている。
- つながりの確認は `getIssuerX500Principal().equals(次の getSubjectX500Principal())`。名前が同じなら「上の証明書が下を発行した」ことを示す。
- `Optional<X509Certificate>` は「見つかったかもしれないし、見つからなかったかもしれない」入れ物。`isPresent()` で有無を確かめ、`get()` で中身を取り出す。

## 2. CertificatePrinter.java（Step 5）

```java
import java.security.MessageDigest;
import java.security.cert.X509Certificate;

public class CertificatePrinter {

    public static void print(int index, X509Certificate certificate) throws Exception {
        String title = index == 0 ? "サーバー証明書" : "中間CA証明書";
        System.out.println();
        System.out.println("[" + index + "] " + title);
        System.out.println("Subject:        " + certificate.getSubjectX500Principal());
        System.out.println("Issuer:         " + certificate.getIssuerX500Principal());
        System.out.println("シリアル番号:   " + certificate.getSerialNumber().toString(16));
        System.out.println("有効期間:       " + certificate.getNotBefore() + " 〜 " + certificate.getNotAfter());
        System.out.println("署名アルゴリズム: " + certificate.getSigAlgName());
        System.out.println("公開鍵:         " + certificate.getPublicKey().getAlgorithm());
        System.out.println("SHA-256指紋:    " + sha256Fingerprint(certificate));
    }

    static String sha256Fingerprint(X509Certificate certificate) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(certificate.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            if (i > 0) {
                sb.append(":");
            }
            sb.append(String.format("%02X", hash[i]));
        }
        return sb.toString();
    }
}
```

ポイント:
- `getSerialNumber()` は `BigInteger`。`toString(16)` で16進表記にする（証明書ビューアの表示に近づける）。
- `getNotBefore()` / `getNotAfter()` は `java.util.Date`。そのまま文字列にすると `Thu Apr 01 09:00:00 JST 2021` のような形で表示される。
- `getPublicKey().getAlgorithm()` は `RSA` や `EC` などの鍵の種類。
- SHA-256指紋は「証明書の中身（`getEncoded()` のバイト列）を SHA-256 でハッシュしたもの」。ブラウザの証明書ビューアに出る指紋と同じ値になる。
  - `String.format("%02X", b)` … 1バイトを2桁の大文字16進にする（例: `0x0b` → `0B`）
  - バイトの間に `:` をはさむため、2個目以降だけ `:` を足している
- `print()` に `throws Exception` が付いたのは、指紋の計算（`MessageDigest` / `getEncoded`）が例外を投げる可能性があるため。

表示例（トラストストアのルートCAで試した実際の出力）:

```text
[0] サーバー証明書
Subject:        CN=Certainly Root E1, O=Certainly, C=US
Issuer:         CN=Certainly Root E1, O=Certainly, C=US
シリアル番号:   62533b1470333275cf98d9ab9bfccf8
有効期間:       Thu Apr 01 09:00:00 JST 2021 〜 Sun Apr 01 09:00:00 JST 2046
署名アルゴリズム: SHA384withECDSA
公開鍵:         EC
SHA-256指紋:    B4:58:5F:22:E4:AC:75:6A:...（省略）
```

※ これはルートCAなので Subject と Issuer が同じ（自己署名）。example.com のサーバー証明書では Issuer が中間CAになる。

## 3. TrustAnchorFinder.java（Step 6）

```java
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class TrustAnchorFinder {

    public static Optional<X509Certificate> find(X509Certificate top) throws Exception {
        // 1. Java標準のトラストストア（cacerts）を読み込んだ TrustManagerFactory を作る
        TrustManagerFactory factory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);

        for (TrustManager trustManager : factory.getTrustManagers()) {
            if (!(trustManager instanceof X509TrustManager)) {
                continue;
            }
            // 2. 信頼しているルートCAの候補を取り出す
            X509Certificate[] candidates = ((X509TrustManager) trustManager).getAcceptedIssuers();

            for (X509Certificate candidate : candidates) {
                // 3. top の Issuer と、候補の Subject が一致するか
                if (!candidate.getSubjectX500Principal().equals(top.getIssuerX500Principal())) {
                    continue;
                }
                // 4. 名前だけでなく、署名も確かめる（候補の公開鍵で top の署名を検証）
                try {
                    top.verify(candidate.getPublicKey());
                    return Optional.of(candidate);
                } catch (Exception e) {
                    // 名前は同じでも鍵が違う（別世代のルートCAなど）→ 次の候補へ
                }
            }
        }
        return Optional.empty();
    }
}
```

ポイント:
- `factory.init((KeyStore) null)` … `null` を渡すと「Java標準のトラストストア（cacerts）を使う」という意味になる。
- `getTrustManagers()` は配列で返る。その中から `X509TrustManager`（X.509証明書を扱うもの）を `instanceof` で探す。
- `getAcceptedIssuers()` … 信頼しているルートCAの一覧（こちらの確認環境のJDKでは120件。件数はJDKのバージョンで変わる）。
- 名前（Subject / Issuer）の一致だけでは不十分。同じ名前でも鍵が違う世代のルートCAがあるため、`verify()` で署名を確かめて初めて「このルートCAが発行した」と言える。
- `verify()` は、署名が合わないと例外を投げる。そのため `try` / `catch` で受けて、次の候補に進んでいる。

## 実行して確認すること

```bash
cd 14-tls-certificate-chain/src
javac *.java
java TlsCertificateInspector example.com 443
```

- `[0] サーバー証明書` と `[1] 中間CA証明書` の詳細が表示される
- `[0] の Issuer と [1] の Subject: 一致（つながっている）` と表示される
- 最後に `[Trust Anchor] Javaが信頼しているルートCA` とルートCAの Subject が表示される
- 別のサイトでも試す（例: `java TlsCertificateInspector www.google.com 443`）。チェーンの長さや発行元が違うことを確かめる
