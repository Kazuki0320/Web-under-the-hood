# Apache vs Nginx（前段プロキシ比較）手順書（05）

このファイルは、`05-apache-vs-nginx` を一つずつ確認しながら進めるための実行手順です。  
03で作成したJava HTTPサーバーを同一バックエンドとして使い、Nginx（`:8080`）とApache（`:8083`）を比較します。

## Step 0: 前提コマンドを確認する

やること:
- `docker`, `docker compose`, `curl` が使える状態か確認する
- 03のバックエンドが手元にあることを確認する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood

docker --version
docker compose version
curl --version
ls 03-java-only-http-server/src/Main.java
```

完了条件:
- 上記コマンドがエラーなく実行できる
- `03-java-only-http-server/src/Main.java` が存在する

---

## Step 1: 05用の設定ファイル配置を作る

やること:
- 05配下に `nginx` と `apache` の設定ディレクトリを作る
- 比較実験で使う `docker-compose.yml` を作る

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/05-apache-vs-nginx
mkdir -p nginx apache
```

完了条件:
- `05-apache-vs-nginx/nginx` と `05-apache-vs-nginx/apache` が存在する

---

## Step 2: docker-composeを作る

やること:
- `backend`（03のJavaサーバー）
- `nginx`（前段: `localhost:8080`）
- `apache`（前段: `localhost:8083`）

の3サービスを同一ネットワークで起動できるようにする。

`docker-compose.yml`:
```yaml
services:
  backend:
    image: eclipse-temurin:17-jdk
    container_name: wuth-backend
    working_dir: /app
    volumes:
      - ../03-java-only-http-server/src:/app/src:ro
    command: >
      sh -c "javac src/Main.java && java -cp src Main"
    expose:
      - "8080"

  nginx:
    image: nginx:1.27-alpine
    container_name: wuth-nginx
    depends_on:
      - backend
    ports:
      - "8080:80"
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf:ro

  apache:
    image: httpd:2.4
    container_name: wuth-apache
    depends_on:
      - backend
    ports:
      - "8083:80"
    volumes:
      - ./apache/httpd.conf:/usr/local/apache2/conf/httpd.conf:ro
```

完了条件:
- `docker compose config` が成功する

---

## Step 3: Nginxのプロキシ設定を書く

やること:
- `/api/` を `backend:8080` にプロキシする
- `Host` と `X-Forwarded-*` を転送する

`nginx/default.conf`:
```nginx
server {
    listen 80;
    server_name _;

    location = / {
        return 200 'nginx proxy is running\n';
        add_header Content-Type text/plain;
    }

    location /api/ {
        proxy_pass http://backend:8080/;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

補足:
- `location /api/` + `proxy_pass .../` の組み合わせで、`/api/hello` はバックエンド側で `/hello` になる。

完了条件:
- `nginx/default.conf` が保存されている

---

## Step 4: Apacheのプロキシ設定を書く

やること:
- `mod_proxy` / `mod_proxy_http` / `mod_headers` を有効化する
- `/api/` を `backend:8080` にプロキシする

`apache/httpd.conf`:
```apache
ServerName localhost
Listen 80

LoadModule mpm_event_module modules/mod_mpm_event.so
LoadModule authn_core_module modules/mod_authn_core.so
LoadModule authz_core_module modules/mod_authz_core.so
LoadModule unixd_module modules/mod_unixd.so
LoadModule dir_module modules/mod_dir.so
LoadModule log_config_module modules/mod_log_config.so
LoadModule mime_module modules/mod_mime.so
LoadModule headers_module modules/mod_headers.so
LoadModule proxy_module modules/mod_proxy.so
LoadModule proxy_http_module modules/mod_proxy_http.so

User daemon
Group daemon

ServerAdmin you@example.com
DocumentRoot "/usr/local/apache2/htdocs"

<Directory "/usr/local/apache2/htdocs">
    AllowOverride None
    Require all granted
</Directory>

ErrorLog /proc/self/fd/2
CustomLog /proc/self/fd/1 combined

ProxyPreserveHost On
RequestHeader set X-Forwarded-Proto "http"

ProxyPass "/api/" "http://backend:8080/"
ProxyPassReverse "/api/" "http://backend:8080/"
```

完了条件:
- `apache/httpd.conf` が保存されている

---

## Step 5: 3サービスを起動する

やること:
- Composeで `backend` / `nginx` / `apache` を起動する
- 全サービスが `Up` になることを確認する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/05-apache-vs-nginx

docker compose up -d

docker compose ps
```

完了条件:
- `backend`, `nginx`, `apache` がすべて `Up` 表示

---

## Step 6: 同一エンドポイントで挙動を比較する

やること:
- Nginx経由（`:8080`）とApache経由（`:8083`）で、同じバックエンドレスポンスになるか確認する
- 200系と404系を両方確認する

コマンド:
```bash
# ヘルス確認
curl -i http://localhost:8080/
curl -i http://localhost:8083/

# 200系（03のStep 5で /hello が実装済み前提）
curl -i http://localhost:8080/api/hello
curl -i http://localhost:8083/api/hello

# 404系
curl -i http://localhost:8080/api/notfound
curl -i http://localhost:8083/api/notfound
```

完了条件:
- 両プロキシから同等のレスポンス（200/404）が返る
- `/api/hello` がバックエンドで処理されている

---

## Step 7: ログを見て差分を観察する

やること:
- 各コンテナログを確認し、アクセスログの見え方を比較する
- どの層で失敗しているかを切り分けられるようにする

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/05-apache-vs-nginx

docker compose logs --tail=100 nginx
docker compose logs --tail=100 apache
docker compose logs --tail=100 backend
```

確認ポイント:
- NginxとApacheでログ形式が異なる
- バックエンドログに `method=GET, path=/hello` などが出る

完了条件:
- Nginx側・Apache側・バックエンド側のどこで問題が起きたか説明できる

---

## Step 8: 終了とクリーンアップ

やること:
- コンテナを停止し、比較環境をクリーンに戻す

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/05-apache-vs-nginx

docker compose down
```

完了条件:
- `docker compose ps` で05のコンテナが終了している

---

## よくある詰まりポイント

- Apacheで `mod_proxy` 系の `LoadModule` を入れ忘れて起動エラーになる
- `ProxyPass` / `proxy_pass` の末尾 `/` の有無でパスが二重化・欠落する
- バックエンド（03）が `/hello` を返さない実装だと `/api/hello` が404になる
- 既にローカルで `8080` や `8083` を使っているとポート競合になる

## 最終チェック

- `localhost:8080`（Nginx）と `localhost:8083`（Apache）の両方で `/api/hello` が成功する
- 404系の挙動も同条件で比較できる
- 設定差（Nginx: `location/proxy_pass`、Apache: `ProxyPass/ProxyPassReverse`）を説明できる
