# DNS 名前解決フロー 手順書（07）

このファイルは、`07-dns-name-resolution` を順番に進めるための実行手順です。  
各ステップで「やること」と「完了条件」を分けています。

## Step 0: 事前確認（コマンドとネットワーク）

やること:
- `dig` / `nslookup` / `curl` が使えるか確認する
- DNS問い合わせが可能なネットワーク状態か確認する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/07-dns-name-resolution
which dig
which nslookup
which curl
```

補足:
- `dig` が無い場合は `nslookup` だけでも進行可能

完了条件:
- `dig` か `nslookup` のどちらかが使える
- `curl` が使える

---

## Step 1: A / AAAA / CNAME を引いて差分を見る

やること:
- 同じドメインに対して `A` / `AAAA` / `CNAME` を引く
- レコードタイプごとの返り値差分を確認する

コマンド例:
```bash
# Aレコード

dig example.com A

# AAAAレコード
dig example.com AAAA

# CNAME（あるドメインで実施）
dig www.github.com CNAME

# 簡易表示

dig +short example.com A
dig +short example.com AAAA
dig +short www.github.com CNAME
```

完了条件:
- `A` と `AAAA` の違い（IPv4/IPv6）を説明できる
- `CNAME` が別名であることを結果で説明できる

---

## Step 2: DNS問い合わせの基本情報を読む

やること:
- `dig` 出力の `QUESTION` / `ANSWER` / `AUTHORITY` / `ADDITIONAL` を確認する
- `status`（`NOERROR` など）を確認する

コマンド例:
```bash
dig example.com A
```

見るポイント:
- `status: NOERROR` か
- `ANSWER SECTION` に期待レコードがあるか
- `Query time` と `SERVER` が何か

完了条件:
- どこを見れば成功/失敗判定できるか説明できる

---

## Step 3: TTLとキャッシュ挙動を観測する

やること:
- 同じ問い合わせを複数回実行し、TTLの変化を確認する
- `+short` ではなく通常出力でTTLを確認する

コマンド例:
```bash
dig example.com A
sleep 3
dig example.com A
sleep 3
dig example.com A
```

補足:
- TTLは環境やリゾルバによって見え方が異なる
- 毎回同じ値にならなくても問題ない

完了条件:
- TTLとキャッシュの関係を説明できる
- 「結果が固定でない理由」を説明できる

---

## Step 4: 失敗ケース（NXDOMAIN）を確認する

やること:
- 存在しないドメインを問い合わせる
- 失敗時の `status` や出力差分を確認する

コマンド例:
```bash
dig definitely-not-exist-example-12345.com A
nslookup definitely-not-exist-example-12345.com
```

完了条件:
- 失敗時の判定ポイント（`NXDOMAIN` など）を説明できる
- 成功時出力との違いを説明できる

---

## Step 5: DNSとHTTP/HTTPS接続をつなげる

やること:
- `curl -v` で接続先IPを確認する
- DNS解決後にTCP/TLSへ進む流れを確認する

コマンド例:
```bash
curl -v https://example.com
```

見るポイント:
- `Trying ...`（接続先IP）
- TLSハンドシェイク開始のログ
- HTTPレスポンス行

完了条件:
- 「DNS解決 -> 接続 -> HTTPS通信」の順を説明できる

---

## Step 6: 最小ログを残す

やること:
- ここまでの観測結果を1ファイルにまとめる
- 成功ケースと失敗ケースを1つずつ残す

コマンド例:
```bash
{
  echo "# DNS観測ログ";
  date;
  echo;
  echo "## A";
  dig +short example.com A;
  echo;
  echo "## AAAA";
  dig +short example.com AAAA;
  echo;
  echo "## NXDOMAIN";
  dig definitely-not-exist-example-12345.com A | rg "status:";
  echo;
  echo "## HTTPS";
  curl -I https://example.com;
} > DNS_OBSERVATION_LOG.md
```

完了条件:
- `DNS_OBSERVATION_LOG.md` が作成されている
- 成功/失敗/HTTPS接続確認の3要素が記録されている

---

## よくある詰まりポイント

- `dig +short` だけ使ってTTLなど詳細を見落とす
- `CNAME` の先を追わずに結果解釈を誤る
- DNS失敗をHTTP障害と混同する
- VPNや社内DNSの影響で環境差が出ることを考慮しない

## 最終チェック

- `A` / `AAAA` / `CNAME` を実際に確認した
- TTLとキャッシュの挙動を説明できる
- `NXDOMAIN` の失敗ケースを説明できる
- DNS解決からHTTPS到達までの流れを口頭で説明できる
