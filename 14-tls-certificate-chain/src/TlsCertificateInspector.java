import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

public class TlsCertificateInspector {
    // TODO: コマンドライン引数でホスト名とポートを受け取る（省略時は example.com 443）。
    // TODO: Step 1  SSLSocketFactory.getDefault() と createSocket(host, port) で SSLSocket を作る。
    // TODO: Step 2  SSLParameters#setEndpointIdentificationAlgorithm("HTTPS") でホスト名検証を有効にする。
    // TODO: Step 3  startHandshake() を呼ぶ（ここで JSSE が証明書チェーンを検証する）。
    // TODO: Step 4  getSession() から TLSバージョン・暗号スイート・証明書チェーンを取得して表示する。
    // TODO: Step 5  表示処理は CertificatePrinter に任せる。
    // TODO: Step 6  チェーンの一番上の証明書から、TrustAnchorFinder でルートCAを探して表示する。
    // TODO: このクラスは「接続とチェーン取得」だけに責務を限定する。
    private static final String DEFAULT_HOST = "example.com";
    private static final int DEFAULT_PORT = 443;

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        System.out.println("接続先: " + host + ":" + port);

        X509Certificate[] chain = fetchCertificateChain(host, port);

        // Step 5: chain を CertificatePrinter で1枚ずつ表示する
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

        // Step 6: [0] から順に「Issuer がトラストストアにあるか」を調べ、最初に見つかったルートCAを表示する
        //         （一番上だけを見ると、クロス証明書の発行元がトラストストアに無い場合に見つからないため）
        System.out.println();
        Optional<X509Certificate> root = Optional.empty();
        int foundAt = -1;
        for (int i = 0; i < chain.length; i++) {
            root = TrustAnchorFinder.find(chain[i]);
            if (root.isPresent()) {
                foundAt = i;
                break;
            }
        }

        if (root.isPresent()) {
            System.out.println("[Trust Anchor] Javaが信頼しているルートCA（[" + foundAt + "] の Issuer で見つかった）");
            System.out.println("Subject: " + root.get().getSubjectX500Principal());
            if (foundAt < chain.length - 1) {
                System.out.println("※ [" + (foundAt + 1) + "] 以降はこの検証には不要（クロス証明書など）");
            }
        } else {
            System.out.println("[Trust Anchor] トラストストアに一致するルートCAが見つからない");
        }
    }

    static X509Certificate[] fetchCertificateChain(String host, int port) throws Exception {
        // TODO: Step 1〜4 を実装し、サーバーから届いた証明書チェーンを返す。
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            System.out.println("TCP接続: " + socket.isConnected());

            SSLParameters parameters = socket.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(parameters);

            socket.startHandshake();

            SSLSession session = socket.getSession();
            System.out.println("TLS:" + session.getProtocol());
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
