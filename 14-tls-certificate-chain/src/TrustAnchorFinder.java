import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class TrustAnchorFinder {
    // TODO: Step 6  Javaが信頼するルートCA（トラストアンカー）を探す。
    //   1. TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()) を作り、
    //      init((KeyStore) null) でデフォルトのトラストストア（cacerts）を読み込む。
    //   2. X509TrustManager#getAcceptedIssuers() でルートCA候補を取得する。
    //   3. top の Issuer と、候補の Subject が一致するものを探す。
    //   4. DNの一致だけでなく、top.verify(候補.getPublicKey()) で署名も確認する。
    // TODO: これは JSSE が startHandshake() の内部で済ませたことを、学習用になぞり直す処理。
    // TODO: このクラスは「ルートCAの探索」だけに責務を限定する。

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
        // 見つからなければ空の Optional を返す
        return Optional.empty();
    }
}
