import java.security.MessageDigest;
import java.security.cert.X509Certificate;

public class CertificatePrinter {
    // TODO: Step 5  証明書1枚分の情報を表示する。
    //   - Subject / Issuer
    //   - シリアル番号
    //   - 有効期限（notBefore / notAfter）
    //   - 署名アルゴリズム
    //   - 公開鍵のアルゴリズム（例: RSA, EC）
    //   - SHA-256フィンガープリント
    // TODO: 見出しは index に応じて変える（0 = サーバー証明書、それ以外 = 中間CA証明書）。
    // TODO: このクラスは「表示」だけに責務を限定する。

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
        // TODO: MessageDigest.getInstance("SHA-256") で certificate.getEncoded() のハッシュを求め、
        //       "AB:CD:..." のような16進表記の文字列にして返す。
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
