# digで叩いて理解する自作DNSサーバー入門

## はじめに

今回は、`dig` でクエリを送りながら、名前解決の流れを自作DNSサーバーで確認しました。  
DNSサーバーが問い合わせを受け取り、最終的にIPアドレスを返すまでの仕組みを、最小構成で実装しています。

---

## DNSサーバーとは

DNSサーバーは、ドメイン名とIPアドレスの対応を管理しているサーバーです。  
ブラウザでURLにアクセスすると、まずDNSサーバーに問い合わせて接続先のIPアドレスを取得します。  
この名前解決ができてはじめて、HTTP/HTTPSの通信に進めます。

逆にDNSが使えないと、`example.com` のようなドメイン名では接続できません。  
名前をIPアドレスに変換できないためです。  
※接続先IPを直接指定した場合だけ通信できるケースはあり

DNSの問い合わせでUDPがよく使われるのは、1回の問い合わせ/応答が小さく、接続確立が不要なぶん速く処理しやすいからです。  

---

## 今回の実装方針（最小）

まずは、最小構成のDNSサーバーを作りました。  
実装の範囲は次の3点に限定しています。

- UDP `:10053` でDNSクエリを受信する
- `A` クエリ（IPv4アドレス問い合わせ）のみ処理する
- 問い合わせ名に対して固定値 `127.0.0.1` を返す

この段階では、再帰問い合わせやキャッシュ、`AAAA` 対応は入れていません。  
あくまで「DNSクエリを受けて、DNSレスポンスを返す」最小ループを確認するための実装です。

具体的には、`DatagramSocket` で受信したバイト列から `QTYPE` と `QCLASS` を読み取り、  
`A` + `IN` のときだけ回答部を組み立てて返す、という流れにしています。

---

## 実装全体（Main.java）

まず最初に、今回使った最小実装全体を載せます。

`src/Main.java`（最小実装例）:
```java
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class Main {
    private static final int PORT = 10053;

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

全体像を見たうえで、次に処理ごとのポイントを見ていきます。

### 1. UDPでクエリを受信する

```java
byte[] buf = new byte[512];
DatagramPacket packet = new DatagramPacket(buf, buf.length);
socket.receive(packet);
```

- 512バイトの受信バッファを用意
- `DatagramPacket` を受け皿にして `receive` で1パケット受信

### 2. `buildResponse` でAクエリだけ処理する

```java
int qtype = ((query[qnameEnd] & 0xFF) << 8) | (query[qnameEnd + 1] & 0xFF);
int qclass = ((query[qnameEnd + 2] & 0xFF) << 8) | (query[qnameEnd + 3] & 0xFF);

if (qtype != 1 || qclass != 1) {
    return null;
}
```

- `QTYPE` / `QCLASS` を取り出す
- `A(1)` かつ `IN(1)` 以外は未対応として `null` を返す

### 3. 回答部に `127.0.0.1` を詰めて返す

```java
response[ans + 6] = 0x00;
response[ans + 7] = 0x00;
response[ans + 8] = 0x00;
response[ans + 9] = 0x3C; // TTL = 60秒
response[ans + 10] = 0x00;
response[ans + 11] = 0x04; // IPv4は4バイト
response[ans + 12] = 127;
response[ans + 13] = 0;
response[ans + 14] = 0;
response[ans + 15] = 1;
```

- 回答レコードのRDATAとして `127.0.0.1` を固定設定
- これにより、`my.local A` に対して常に `127.0.0.1` を返す
- TTLは `response[ans + 6]` 〜 `response[ans + 9]` の4バイトで設定
- 今回は `0x0000003C` を入れており、TTLは60秒

### 4. クライアントへ応答を返す

```java
DatagramPacket reply = new DatagramPacket(
    response,
    response.length,
    packet.getAddress(),
    packet.getPort()
);
socket.send(reply);
```

- 問い合わせ元のIP/Portに向けて応答パケットを返す

---

## 実行して確認する手順

まずはサーバーを起動します。

```bash
cd 08-build-dns-server
javac src/Main.java
java -cp src Main
```

別ターミナルで、`dig` を実行します。

```bash
dig @127.0.0.1 -p 10053 my.local A
```

このコマンドは、**「ローカルPCの10053番で動くDNSサーバーに、`my.local` のAレコードを聞く」** という意味です。

期待する確認ポイント:

- `status: NOERROR` が返る
  - 問い合わせ自体が正常に処理されたことを確認するため
- `ANSWER SECTION` に `A 127.0.0.1` が表示される
  - 自作DNSサーバーが意図した固定IPを返せていることを確認するため

実際の出力例:

```text
;; Warning: Message parser reports malformed message packet.

; <<>> DiG 9.10.6 <<>> @127.0.0.1 -p 10053 my.local A
; (1 server found)
;; global options: +cmd
;; Got answer:
;; WARNING: .local is reserved for Multicast DNS
;; You are currently testing what happens when an mDNS query is leaked to DNS
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: <ID>
;; flags: qr rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 4096
;; QUESTION SECTION:
;my.local.                      IN      A

;; ADDITIONAL SECTION:
my.local.               60      IN      A       127.0.0.1

;; Query time: <N> msec
;; SERVER: 127.0.0.1#10053(127.0.0.1)
;; WHEN: <TIMESTAMP>
;; MSG SIZE  rcvd: 53
```

基本的な見方（成功例）:

- `status: NOERROR`
  - 問い合わせ自体は正常終了している。
- `QUERY: 1, ANSWER: 1`
  - 問い合わせ1件に対して、回答1件を返している。
- `SERVER: 127.0.0.1#10053`
  - ローカルの自作DNSサーバーに実際に問い合わせできている。
- `A 127.0.0.1`
  - `my.local` のAレコードとして固定IPを返せている。
- `malformed message packet`
  - 応答パケットの構造に不整合があり、最小実装としては今後の改善ポイント。

失敗例（未対応クエリ）:

```text
$ dig @127.0.0.1 -p 10053 my.local AAAA

; <<>> DiG 9.10.6 <<>> @127.0.0.1 -p 10053 my.local AAAA
; (1 server found)
;; global options: +cmd
;; connection timed out; no servers could be reached
```

この実装では `A` のみを処理対象にしているため、`AAAA` は応答せずタイムアウトになります。

基本的な見方（失敗例）:

- `connection timed out`
  - DNSサーバーから応答が返らなかった。
- `no servers could be reached`
  - 問い合わせ先に到達できなかった、または応答を受信できなかった。
- 今回のケース
  - `AAAA` 未対応で `null` を返しているため、結果として無応答タイムアウトになっている。

---

## まとめ

- DNSサーバーは、ドメイン名をIPアドレスに変換する役割を持ち、通信の入口になる
- 今回はUDPベースの最小構成で、`A` クエリにだけ応答する自作DNSサーバーを実装した
- `dig @127.0.0.1 -p 10053 my.local A` で、固定値 `127.0.0.1` を返す動作を確認できた
- `AAAA` クエリは未対応のため、応答せずタイムアウトになることも確認できた
