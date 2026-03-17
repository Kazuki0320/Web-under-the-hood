import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

public class Main {
    // TODO: サーバー待受ポートを定義する（例: 8080）
    private static final int PORT = 8080;
    // TODO: keep-alive用のアイドルタイムアウト(ms)を定義する
    private static final int IDLE_TIMEOUT_MS = 5000;
    // TODO: 1接続あたりの最大リクエスト数を定義する
    private static final int MAX_REQUESTS_PER_CONNECTION = 5;

    public static void main(String[] args) {
        // TODO:
        // 1. ServerSocketをPORTで起動する
        // 2. acceptループで接続を受ける
        // 3. 接続ごとにhandleConnection(...)を呼ぶ
    }

    private static void handleConnection(Socket client) throws IOException {
        // TODO:
        // 1. clientにIDLE_TIMEOUT_MSを設定
        // 2. 入出力ストリームを作成
        // 3. requestCount < MAX_REQUESTS_PER_CONNECTION の間ループ
        // 4. readRequest(...)で1件読む
        // 5. Connection: close または上限到達ならcloseConnection=true
        // 6. writeResponse(...)で返す
        // 7. closeConnectionならループ終了
    }

    private static Request readRequest(BufferedReader reader, InputStream rawIn) throws IOException {
        // TODO:
        // 1. リクエストラインを読む（null/空なら終了扱い）
        // 2. method/path/versionを取り出す
        // 3. 空行までヘッダーを読む（Mapへ格納）
        // 4. Content-Lengthがあれば本文をconsumeBody(...)で読み切る
        // 5. Requestオブジェクトを返す
        return null;
    }

    private static int parseContentLength(String value) {
        // TODO: nullや不正値を0として扱い、正の整数を返す
        return 0;
    }

    private static void consumeBody(InputStream in, int contentLength) throws IOException {
        // TODO:
        // contentLengthぶんだけ読み切る
        // （読み残しがあると次リクエストの境界が壊れる）
    }

    private static void writeResponse(
        OutputStream out,
        Request request,
        boolean closeConnection
    ) throws IOException {
        // TODO:
        // 1. method/pathでルーティングして status/contentType/body を決める
        // 2. Content-Lengthを計算する
        // 3. Connectionヘッダーを keep-alive / close で切り替える
        // 4. HTTPレスポンスを書き込んでflushする
    }

    private static final class Request {
        // TODO: 必要ならフィールドを追加する（例: version, body など）
        private final String method;
        private final String path;
        private final Map<String, String> headers;

        private Request(String method, String path, Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.headers = headers;
        }
    }
}
