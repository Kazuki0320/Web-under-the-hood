# Web-under-the-hood

このリポジトリは、**Webページが表示されるまでに何が起きているのか**を
最小構成から段階的に分解・体験するための学習プロジェクトです。

目的はフレームワークやクラウドを使わずに

- Webサーバーの責務
- アプリケーションサーバーは何をしているのか
- 通信はどこをどう流れているのか

を**自分の手で再現して、説明できる**ようになることです。

---

## 全体の考え方

- 実務構成の縮小版を理解する
- 比較できるところは比較する
- 全体をラフに描いてから、複雑な部分に着手する

---
## プロジェクト構成

web-under-the-hood/
├── 01-static-web-nginx/
├── 02-java-servlet-tomcat/
├── 03-nginx-reverse-proxy/
├── 04-apache-vs-nginx/
└── 05-network-observation/


---

## 各プロジェクトの概要

### 01-static-web-nginx  
**最小構成で「Webとは何か」を理解する**

ブラウザからのHTTPリクエストを受け取り、  
静的HTMLを返すだけの最小Webサーバー構成。

- Nginxの最小責務
- ポートを公開するとはどういうことか
- ブラウザ → Webサーバー → HTML の直線的な流れ

を体験する。

---

### 02-java-servlet-tomcat  
**Java Webアプリの正体を知る**

Tomcat上で最小のServletを動かし、  
JavaアプリケーションがWeb上でどう振る舞うかを理解する。

- TomcatがServletコンテナである意味
- Javaアプリは「ただのプロセス」であること
- HTTPとJavaコードの接点

を明確にする。

---

### 03-nginx-reverse-proxy  
**Webアプリらしい構成を完成させる**

Nginxをリバースプロキシとして配置し、  
リクエストをTomcatへ中継する構成。

- Webサーバーとアプリサーバーの責務分離
- なぜTomcatを直接外に出さないのか
- `/api` などパスベースルーティングの意味

を理解する。

---

### 04-apache-vs-nginx  
**ApacheとNginxを比較して思想を理解する**

同じ静的HTMLをApacheとNginxで配信し、  
設定や思想の違いを観察する。

- 両者の設計思想の違い
- なぜNginxがフロントに使われやすいのか
- 設定ファイルが表す責務の差

を比較から学ぶ。

---

### 05-network-observation  
**通信を分解して観測する**

Dockerネットワークを使い、  
リクエストがどこをどう通っているかを可視化する。

- localhost の正体
- Docker NAT / コンテナ間通信
- 「つながらない」時の切り分け方

をネットワーク視点で理解する。

---

## このリポジトリで最終的に得られること

- Webページ表示までの流れを図で説明できる
- Nginx / Apache / Tomcat の役割を言語化できる
- Dockerやクラウド構成の理解が一段深くなる
- 障害時に「どこを見るべきか」が分かる

---

## 進め方のおすすめ順

1. 01-static-web-nginx  
2. 02-java-servlet-tomcat  
3. 03-nginx-reverse-proxy  
4. 04-apache-vs-nginx  
5. 05-network-observation  

---

このリポジトリは「完成させる」ことよりも、  
**理解を深め続けるための土台**として育てていく。

