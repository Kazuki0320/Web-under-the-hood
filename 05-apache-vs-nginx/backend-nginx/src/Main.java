/*
正しい実装フロー（ステップ）
1. Mainクラスとmainメソッドを作成する。
2. ServerSocket(8080)を作成して待ち受けを開始する。
3. while (true) で接続待ちループを回す。
4. accept()で1クライアント分のSocketを受け取り、接続ごとに処理する。
5. InputStreamからリクエスト行を読み、method/path/protocolに分解する。
6. ヘッダーを空行まで読み取る（必要ならHostやContent-Lengthを保持する）。
7. リクエスト形式が不正なら400
8. ルーティングでpathを判定し、status/content-type/bodyを決定する（例: 200/404）。
9. Content-Lengthを本文バイト数で計算し、HTTPレスポンス文字列を組み立てる。
10. OutputStreamへヘッダーと本文を書いてflushし、Socketを閉じる。
11. 例外発生時はログを出し、必要に応じて500を返す。
12. 次のaccept()に戻り、次の接続を処理する。
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
	public static void main(String [] args) {
		int port = 8080;

		try (ServerSocket server = new ServerSocket(port)) {
			System.out.println("Server listening on port: " + port);

			while (true) {
				try (Socket client = server.accept()) {
					BufferedReader reader = new BufferedReader(
						new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
					);

					String requestLine = reader.readLine();
					if (requestLine == null || requestLine.isEmpty()) {
						continue;
					}

					String line;
					while ((line = reader.readLine()) != null && !line.isEmpty()) {
					}

					String[] parts = requestLine.split(" ");
					String method = parts.length > 0 ? parts[0] : "";
					String path = parts.length > 1 ? parts[1] : "";

					String status;
					String contentType;
					String responseBody;

					if ("GET".equals(method) && "/hello".equals(path)) {
						status = "200 OK";
						contentType = "text/plain; charset=UTF-8";
						responseBody = "Hello World";
					} else {
						status = "404 Not Found";
						contentType = "text/plain; charset=UTF-8";
						responseBody = "Not Found";
					}

					byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
					String headers = 
						"HTTP/1.1 " + status + "\r\n" +
						"Content-Type: " + contentType + "\r\n" +
						"Content-Length: " + bodyBytes.length + "\r\n" +
						"Connection: close\r\n" +
						"\r\n";

					OutputStream out = client.getOutputStream();
					out.write(headers.getBytes(StandardCharsets.UTF_8));
					out.write(bodyBytes);
					out.flush();
				} catch (IOException e) {
					System.err.println("Failed to handle client: " + e.getMessage());
				}
			}
		} catch (IOException e) {
			System.err.println("Failed to start server: " + e.getMessage());
		}
	}
}