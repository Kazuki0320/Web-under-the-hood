# QA_MEMO_DNS

## Q1. digコマンドで何ができる？

疑問点:
- 「そもそも、digコマンドで何ができるの？」

回答:
- DNSに対して、指定したレコードの問い合わせ結果を確認できる。
- `A` / `AAAA` / `CNAME` / `MX` / `TXT` などを個別に引ける。
- `status`（成功/失敗）、`ANSWER`、TTL、問い合わせ先DNSサーバーを確認できる。
- 失敗系（`NXDOMAIN` など）を再現して、名前解決失敗を切り分けできる。
- `+short` で結果だけを短く表示できる。

確認コマンド例:
```bash
dig example.com A
dig example.com AAAA
dig www.github.com CNAME
dig example.com MX
dig example.com TXT
dig +short example.com A
```

## Q2. AとかAAAAは、解決したい名前を指定してる感じ？

疑問点:
- 「AとかAAAAとかは、URLに対して解決したい名前を指定してる感じ？」

回答:
- 近いが正確には、`A` / `AAAA` は「名前」ではなく「レコード種別」。
- 指定するのは `example.com` のようなドメイン名（ホスト名）。
- `A` はIPv4、`AAAA` はIPv6を取得するための指定。
- `dig` はURL全体ではなく、主にドメイン名部分を対象にする。

## Q3. レコードという概念がまだわからない

疑問点:
- 「レコードの概念がまだわからない。DNSの仕組みと交えて知りたい」

回答:
- DNSは「名前と情報の対応表」。
- その対応表の1行（1種類の情報）がレコード。
- 例:
  - `A`: IPv4アドレス
  - `AAAA`: IPv6アドレス
  - `CNAME`: 別名（他の名前への参照）
- 通信の流れは `DNS解決 -> 接続 -> HTTP/HTTPS`。

## Q4. 対応表の具体例を見たい

疑問点:
- 「対応表の具体例を見たい」

回答:
- 例:
  - `example.com` + `A` -> `93.184.216.34`
  - `example.com` + `AAAA` -> `2606:...`
  - `www.example.com` + `CNAME` -> `example.com`
  - `example.com` + `MX` -> `mail.example.com`
  - `example.com` + `TXT` -> `v=spf1 ...`
- 同じ名前でも、レコード種別ごとに返る情報が異なる。

## Q5. DNSはデータベース？

疑問点:
- 「DNSはデータベース？」

回答:
- 概念的には分散データベースに近い。
- ただし一般DBではなく、名前解決専用の分散・階層型システム。
- `Root -> TLD -> 権威DNS` とキャッシュ（TTL）で動く。

## Q6. DNSはMacに標準搭載されている？

疑問点:
- 「DNSはMacのOSに元々標準搭載されているの？」

回答:
- はい。macOSには名前解決の仕組み（リゾルバ）が標準搭載されている。
- 通常のアプリはOSのリゾルバを使ってDNS解決する。
- `dig` / `nslookup` も多くの環境で利用できる。

## Q7. dig と nslookup の違い

疑問点:
- 「digとnslookupの違いを知りたい」

回答:
- `dig`: 詳細確認向け（調査・学習・トラブルシュートに向く）
- `nslookup`: 簡易確認向け（素早い動作確認に向く）
- `dig` は `status` / `ANSWER` / TTL / 利用DNSなど、観測に必要な情報が多い。
- 使い分け:
  - ざっくり確認: `nslookup example.com`
  - 深掘り調査: `dig example.com A`
