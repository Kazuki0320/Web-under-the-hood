# 14 TLS Certificate Chain

`14` では、Java標準の `SSLSocket` で `example.com:443` にTLS接続し、サーバーから届いた **証明書チェーン** と、Javaが信頼している **ルートCA（トラストアンカー）** を表示するCLIツール `TlsCertificateInspector` を作る。

ブラウザが裏で行っている「この証明書は信頼できるか？」の判断を、Javaで実際に動かして中身を観察するのが目的。

## 目的
「サーバー証明書 → 中間CA → ルートCA と信頼がつながる仕組みを、実物の証明書で説明できる状態になること」

- 役割を分けて理解する
  自分が作るのは **クライアント**（ブラウザと同じ立場）。サーバー（example.com）は相手側で、自分では作らない。
- 信頼の判断と観察を分けて理解する
  信頼するかどうかの判断は Java標準の **JSSE**（`startHandshake()` の内部）が行う。自分で作るのは、その結果を取り出して表示・確認する部分。
- ルートCAが送られない理由を説明できる
  サーバーが送るのは通常「サーバー証明書＋中間CA」まで。ルートCAはクライアント手元のトラストストア（`cacerts`）と突き合わせる。

## 役割の整理

| 立場 | 実体 | やること |
|---|---|---|
| クライアント（自分が作る） | `TlsCertificateInspector` | 接続して、届いた証明書を受け取り、表示・確認する |
| 検証処理（Java標準） | JSSE（`startHandshake()`） | 期限・署名・ホスト名・ルートCAへの到達を検証する |
| トラストストア（Java標準） | JDK付属の `cacerts` | 信頼するルートCAの一覧（ブラウザのルートストアに相当） |
| サーバー（相手側） | `example.com:443` | サーバー証明書と中間CA証明書を送る |

## 最小ゴール
- `example.com` に接続して、サーバー証明書の Subject と Issuer を表示できる（最初の課題・目安30分）

## 実装ステップ（この順番で進める）

Step 1: TLS接続の土台を作る（`SSLSocketFactory.getDefault()` / `createSocket`）
Step 2: ホスト名検証を有効にする（`setEndpointIdentificationAlgorithm("HTTPS")`）
Step 3: TLSハンドシェイクする（`startHandshake()`）
Step 4: 証明書チェーンを取得する（`getSession().getPeerCertificates()`）
Step 5: 証明書の詳細を表示する（Subject / Issuer / 期限 / 署名アルゴリズム / 公開鍵 / SHA-256指紋）
Step 6: Javaが信頼するルートCAを探す（`TrustManagerFactory` / `getAcceptedIssuers()` / `verify()`）

手順の詳細は `SETUP_STEPS.md` を参照。

## 完成条件
- コマンドライン引数でホスト名とポートを受け取れる
- TLSバージョンと暗号スイートを表示できる
- サーバー証明書と中間CA証明書を表示できる
- Subject と Issuer のつながりを確認できる
- Javaが信頼するルートCAを見つけられる
- ルートCAがサーバーから送られない理由を説明できる

## ファイル構成

```text
14-tls-certificate-chain/
├── README.md                     ← この章の概要
├── SETUP_STEPS.md                ← 実装手順
├── MEMO.md                       ← 学びメモ
├── IMPLEMENTATION_OVERVIEW.html  ← 仕組みの解説（3Dモデル付き）
└── src/
    ├── TlsCertificateInspector.java  ← 接続・チェーン取得（Step 1〜4）
    ├── CertificatePrinter.java       ← 証明書の表示（Step 5）
    └── TrustAnchorFinder.java        ← ルートCAの探索（Step 6）
```

最初は `TlsCertificateInspector.java` だけで「接続して Subject と Issuer を表示」まで進め、動いてから表示処理とルートCA探索を分ける。

## 実行方法

```bash
cd 14-tls-certificate-chain/src
javac *.java
java TlsCertificateInspector example.com 443
```

## 発展（余裕があれば）
- 失敗パターンを再現する: 期限切れ・ホスト名不一致・自己署名（例: badssl.com のテスト用サブドメイン ※利用可否は実行時に確認）
- ブラウザ（Chrome）で同じサイトの証明書を表示し、Javaの結果と比べる
