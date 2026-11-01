# TLS証明書チェーン 実装 手順書（14）

このファイルは、`14-tls-certificate-chain` を順番に進めるための実行手順です。
まずは実装前に「今回の目的」と「自分が作る側／相手側」を固定します。

## Step 0: 今回の学習目的を固定する

やること:
- 自分が作るのは **クライアント**（`TlsCertificateInspector`）。ブラウザと同じ立場で `example.com` に接続する
- 信頼の判断は Java標準の JSSE が行う。自分で作るのは「取り出して表示・確認する」部分
- サーバー証明書 → 中間CA → ルートCA のつながりを、実物の証明書で観察する

この章で学ぶ到達目標:
1. Javaで TLS接続し、証明書チェーンを取得できる
2. Subject と Issuer のつながりを説明できる
3. ルートCAがサーバーから送られず、トラストストアで突き合わせる理由を説明できる

完了条件:
- 「自分が作る側（クライアント）」と「相手側（サーバー）」を区別して説明できる

---

## Step 1: TLS接続の土台を作る

やること:
- `SSLSocketFactory.getDefault()` でファクトリを取得する
- `createSocket(host, port)` で `SSLSocket` を作る

```java
SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
```

確認ポイント:
- この時点ではTCP接続ができただけ。証明書はまだ届いていない

---

## Step 2: ホスト名検証を有効にする

やること:
- `SSLParameters` に `"HTTPS"` を設定して、ソケットに戻す

```java
SSLParameters parameters = socket.getSSLParameters();
parameters.setEndpointIdentificationAlgorithm("HTTPS");
socket.setSSLParameters(parameters);
```

確認ポイント:
- `SSLSocket` を直接使う場合、これを設定しないとホスト名検証が行われない
- これにより「証明書が本当に `example.com` 向けか」も検証される

---

## Step 3: TLSハンドシェイクする

やること:
- `socket.startHandshake()` を呼ぶ

確認ポイント（この内部でJSSEがやること）:
1. サーバーから証明書チェーンを受け取る
2. 有効期限を確認する
3. 署名を確認する（上位の証明書の公開鍵で、下位の署名を検証）
4. ホスト名を確認する
5. トラストストアのルートCAまでつながるか確認する

- どれかに失敗すると、ここで `SSLHandshakeException` になる

---

## Step 4: 証明書チェーンを取得する

やること:
- `socket.getSession()` から `SSLSession` を取得する
- `getPeerCertificates()` で証明書配列を取得し、`X509Certificate` にキャストする
- TLSバージョン（`getProtocol()`）と暗号スイート（`getCipherSuite()`）を表示する

```java
SSLSession session = socket.getSession();
Certificate[] certificates = session.getPeerCertificates();
for (Certificate certificate : certificates) {
    X509Certificate x509 = (X509Certificate) certificate;
    System.out.println(x509.getSubjectX500Principal());
    System.out.println(x509.getIssuerX500Principal());
}
```

確認ポイント:
- 配列の先頭 `[0]` がサーバー自身の証明書
- ルートCAは通常含まれない

完了条件（最初の課題）:
- `example.com` の Subject と Issuer が表示される

---

## Step 5: 証明書の詳細を表示する

やること（`CertificatePrinter` に分ける）:
- `getSubjectX500Principal()` / `getIssuerX500Principal()`
- `getSerialNumber()`
- `getNotBefore()` / `getNotAfter()`
- `getSigAlgName()`
- `getPublicKey().getAlgorithm()`
- SHA-256フィンガープリント

```java
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] fingerprint = digest.digest(x509.getEncoded());
```

確認ポイント:
- `[0]` の Issuer と `[1]` の Subject が一致していること

---

## Step 6: Javaが信頼するルートCAを探す

やること（`TrustAnchorFinder` に分ける）:
- `TrustManagerFactory` でデフォルトのトラストストアを読み込む
- `X509TrustManager#getAcceptedIssuers()` でルートCA候補を取得する
- チェーンの一番上の証明書の Issuer と、ルートCAの Subject が一致するものを探す
- DNの一致だけでなく、`verify()` で署名も確認する

```java
TrustManagerFactory factory = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
factory.init((KeyStore) null);
// X509TrustManager#getAcceptedIssuers() から Issuer = Subject を探す
topCertificate.verify(rootCertificate.getPublicKey());
```

確認ポイント:
- これは JSSE が Step 3 の内部で済ませたことを、学習用に自分の手でなぞり直す処理
- 見つかったルートCAを `[Trust Anchor]` として表示する

---

## Step 7: 理解を言語化する

やること:
- `MEMO.md` に次を書く
  - ルートCAがサーバーから送られない理由
  - JSSE が検証している4項目（期限・署名・ホスト名・ルートCA）
  - ブラウザとJavaで信頼するルート一覧が別であること（Chrome独自のルートストア／OS と JDKの `cacerts`）

完了条件:
- `README.md` の完成条件をすべて満たしている
