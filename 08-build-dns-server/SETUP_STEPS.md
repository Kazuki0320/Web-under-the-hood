# 自作DNSサーバー 手順書（08）

このファイルは、`08-build-dns-server` を順番に進めるための実行手順です。  
各ステップで「やること」と「完了条件」を分けています。

## Step 0: 作業ディレクトリを準備する

やること:
- `08-build-dns-server` 配下で作業できる状態にする
- 最小実装の対象を「UDP + Aレコード固定応答」に絞る

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/08-build-dns-server
mkdir -p src
```

完了条件:
- `src` ディレクトリが存在する
- 実装範囲（固定応答DNS）が明確になっている

---

## Step 1: 最小DNSサーバーを実装する

やること:
- UDP `:5353` でDNSクエリを受信する
- `A` クエリのみ処理する
- 問い合わせ名に対して固定IP（例: `127.0.0.1`）を返す

`src/Main.java`（最小実装例）:
```java
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class Main {
    private static final int PORT = 5353;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("DNS server listening on UDP " + PORT);

            while (true) {
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                byte[] query = Arrays.copyOf(packet.getData(), packet.getLength());
                byte[] response = buildResponse(query);
                if (response == null) {
                    continue;
                }

                DatagramPacket reply = new DatagramPacket(
                    response,
                    response.length,
                    packet.getAddress(),
                    packet.getPort()
                );
                socket.send(reply);
            }
        } catch (IOException e) {
            System.err.println("Failed to run DNS server: " + e.getMessage());
        }
    }

    private static byte[] buildResponse(byte[] query) {
        if (query.length < 12) {
            return null;
        }

        int qdCount = ((query[4] & 0xFF) << 8) | (query[5] & 0xFF);
        if (qdCount != 1) {
            return null;
        }

        int idx = 12;
        while (idx < query.length && query[idx] != 0) {
            idx += (query[idx] & 0xFF) + 1;
        }
        if (idx + 5 >= query.length) {
            return null;
        }

        int qnameEnd = idx + 1;
        int qtype = ((query[qnameEnd] & 0xFF) << 8) | (query[qnameEnd + 1] & 0xFF);
        int qclass = ((query[qnameEnd + 2] & 0xFF) << 8) | (query[qnameEnd + 3] & 0xFF);

        if (qtype != 1 || qclass != 1) {
            return null;
        }

        byte[] response = new byte[query.length + 16];
        System.arraycopy(query, 0, response, 0, query.length);

        response[2] = (byte) 0x81;
        response[3] = (byte) 0x80;
        response[6] = 0x00;
        response[7] = 0x01;

        int ans = query.length;
        response[ans] = (byte) 0xC0;
        response[ans + 1] = 0x0C;
        response[ans + 2] = 0x00;
        response[ans + 3] = 0x01;
        response[ans + 4] = 0x00;
        response[ans + 5] = 0x01;
        response[ans + 6] = 0x00;
        response[ans + 7] = 0x00;
        response[ans + 8] = 0x00;
        response[ans + 9] = 0x3C;
        response[ans + 10] = 0x00;
        response[ans + 11] = 0x04;
        response[ans + 12] = 127;
        response[ans + 13] = 0;
        response[ans + 14] = 0;
        response[ans + 15] = 1;

        return response;
    }
}
```

完了条件:
- `src/Main.java` が作成されている
- クエリ受信と固定応答の処理が入っている

---

## Step 2: コンパイルして起動する

やること:
- Javaコードをコンパイルして起動する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/08-build-dns-server
javac src/Main.java
java -cp src Main
```

完了条件:
- `DNS server listening on UDP 5353` が表示される
- サーバーが待受状態になる

---

## Step 3: digで自作DNSサーバーへ問い合わせる

やること:
- 別ターミナルから `dig` でローカルサーバーへ直接問い合わせる

コマンド:
```bash
dig @127.0.0.1 -p 5353 my.local A
```

見るポイント:
- `status: NOERROR`
- `ANSWER SECTION` に `A 127.0.0.1`

完了条件:
- `my.local` に対して `127.0.0.1` を返せる
- 自作DNSがクエリに応答していることを確認できる

---

## Step 4: 失敗系を確認する

やること:
- `AAAA` など未対応クエリを送って、期待どおり応答しないことを確認する

コマンド:
```bash
dig @127.0.0.1 -p 5353 my.local AAAA
```

完了条件:
- 現実装が `A` のみ対応であることを確認できる
- 次の拡張ポイント（AAAA/NXDOMAIN対応）が明確になる

---

## Step 5: 最小検証ログを残す

やること:
- 成功系と未対応系の実行結果を記録する

コマンド例:
```bash
{
  echo "# 08 DNS Server Log";
  date;
  echo;
  echo "## A query";
  dig @127.0.0.1 -p 5353 my.local A;
  echo;
  echo "## AAAA query";
  dig @127.0.0.1 -p 5353 my.local AAAA;
} > DNS_SERVER_TEST_LOG.md
```

完了条件:
- `DNS_SERVER_TEST_LOG.md` が作成されている
- 成功系/未対応系の両方が記録されている

---

## よくある詰まりポイント

- 53番ポートを使おうとして権限エラーになる（最初は `5353` 推奨）
- `dig` 側の問い合わせ先指定（`@127.0.0.1 -p 5353`）を忘れる
- 応答フラグや`ANCOUNT`が不正で、クライアントに無視される
- IPv6問い合わせ（`AAAA`）を送っているのに、`A`実装だけで混乱する

## 最終チェック

- UDPでDNSクエリを受信できる
- `A` クエリに固定IPで応答できる
- `dig` で自作DNSの応答を確認できる
- 未対応クエリの振る舞いを説明できる
