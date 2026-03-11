# Apache vs Nginx（前段プロキシ比較）手順書（05）

このファイルは、`05-apache-vs-nginx` を一つずつ確認しながら進めるための実行手順です。  
Nginx（`:8080`）とApache（`:8083`）を比較するため、バックエンドも**2サービスに分離**して起動します。

- `backend_nginx`（Nginx専用の接続先）
- `backend_apache`（Apache専用の接続先）

## Step 0: 前提コマンドを確認する

やること:
- `docker`, `docker compose`, `curl` が使える状態か確認する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood

docker --version
docker compose version
curl --version
```

完了条件:
- 上記コマンドがエラーなく実行できる

---

## Step 1: 05配下にサービス用ディレクトリを作る

やること:
- Nginx用/Apache用それぞれのバックエンドディレクトリを作る
- プロキシ設定ディレクトリを作る

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/05-apache-vs-nginx
mkdir -p backend-nginx/src backend-apache/src nginx apache
```

完了条件:
- `backend-nginx/src`, `backend-apache/src`, `nginx`, `apache` が存在する

---

## Step 2: 各バックエンドのMain.javaを作成する

やること:
- `backend-nginx` と `backend-apache` にそれぞれ `Main.java` を作成する
- 2つのバックエンドは同じ初期実装から開始し、以降は独立して編集する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/05-apache-vs-nginx

cat > ./backend-nginx/src/Main.java <<'JAVA'
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        int port = 8080;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
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
JAVA

cp ./backend-nginx/src/Main.java ./backend-apache/src/Main.java
```

完了条件:
- `backend-nginx/src/Main.java` と `backend-apache/src/Main.java` が存在する

---

## Step 3: バックエンド2サービスのDockerfileを作る

やること:
- 2つのバックエンドをそれぞれイメージ化する
- `volumes` 共有は使わず、イメージに `src` を取り込む

`backend-nginx/Dockerfile` と `backend-apache/Dockerfile`（同一内容）:
```Dockerfile
FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY src ./src
RUN javac src/Main.java

CMD ["java", "-cp", "src", "Main"]
```

完了条件:
- `backend-nginx/Dockerfile` と `backend-apache/Dockerfile` が保存されている

---

## Step 4: docker-composeを作る（4サービス構成）

やること:
- `backend_nginx`, `backend_apache`, `nginx`, `apache` の4サービスを定義する
- バックエンドは `expose` せず、同一Composeネットワーク内通信のみで接続する

`docker-compose.yml`:
```yaml
services:
  backend_nginx:
    build:
      context: ./backend-nginx
    container_name: wuth-backend-nginx

  backend_apache:
    build:
      context: ./backend-apache
    container_name: wuth-backend-apache

  nginx:
    image: nginx:1.27-alpine
    container_name: wuth-nginx
    depends_on:
      - backend_nginx
    ports:
      - "8080:80"
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf:ro

  apache:
    image: httpd:2.4
    container_name: wuth-apache
    depends_on:
      - backend_apache
    ports:
      - "8083:80"
    volumes:
      - ./apache/httpd.conf:/usr/local/apache2/conf/httpd.conf:ro
```

完了条件:
- `docker compose config` が成功する

---

## Step 5: Nginxのプロキシ設定を書く

やること:
- `/api/` を `backend_nginx:8080` にプロキシする
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
        proxy_pass http://backend_nginx:8080/;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

完了条件:
- `nginx/default.conf` が保存されている

---

## Step 6: Apacheのプロキシ設定を書く

やること:
- `mod_proxy` / `mod_proxy_http` / `mod_headers` を有効化する
- `/api/` を `backend_apache:8080` にプロキシする

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

ProxyPass "/api/" "http://backend_apache:8080/"
ProxyPassReverse "/api/" "http://backend_apache:8080/"
```

完了条件:
- `apache/httpd.conf` が保存されている

---

## Step 7: 4サービスを起動する

やること:
- Composeで4サービスを起動する
- バックエンドを含めて全サービスが `Up` になることを確認する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/05-apache-vs-nginx

docker compose up -d --build

docker compose ps
```

完了条件:
- `backend_nginx`, `backend_apache`, `nginx`, `apache` がすべて `Up` 表示

---

## Step 8: 同一エンドポイントで挙動を比較する

やること:
- Nginx経由（`:8080`）とApache経由（`:8083`）で、同じAPIの結果を比較する
- 200系と404系を両方確認する

コマンド:
```bash
# ヘルス確認
curl -i http://localhost:8080/
curl -i http://localhost:8083/

# 200系（/hello が実装済み前提）
curl -i http://localhost:8080/api/hello
curl -i http://localhost:8083/api/hello

# 404系
curl -i http://localhost:8080/api/notfound
curl -i http://localhost:8083/api/notfound
```

完了条件:
- 両プロキシで200/404を確認できる
- Nginx経由は `backend_nginx`、Apache経由は `backend_apache` に到達している

---

## Step 9: ログを見て差分を観察する

やること:
- 各コンテナログを確認し、プロキシごとの見え方を比較する
- Nginx経由とApache経由で別バックエンドに流れていることを確認する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/05-apache-vs-nginx

docker compose logs --tail=100 nginx
docker compose logs --tail=100 apache
docker compose logs --tail=100 backend_nginx
docker compose logs --tail=100 backend_apache
```

完了条件:
- 失敗時に「どの層で失敗しているか」を切り分けできる
- 2つのバックエンドログを別々に追える

---

## Step 10: 終了とクリーンアップ

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

## ポート競合時の対応（必要な場合）

やること:
- `8080` / `8083` を使っているローカルプロセスを確認し、必要なら停止する

コマンド:
```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8083 -sTCP:LISTEN

# 必要な場合のみ
kill <PID>
```

完了条件:
- `docker compose up -d` 時のポート競合が解消される

---

## よくある詰まりポイント

- Apacheで `mod_proxy` 系の `LoadModule` を入れ忘れて起動エラーになる
- `ProxyPass` / `proxy_pass` の末尾 `/` の有無でパスが二重化・欠落する
- 2つのバックエンドの `Main.java` を片方だけ変更して差異が出る
- ローカルの既存プロセスが `8080` / `8083` を占有している

## 最終チェック

- `localhost:8080`（Nginx）と `localhost:8083`（Apache）の両方で `/api/hello` が成功する
- 404系の挙動も同条件で比較できる
- NginxとApacheが別々のバックエンドへ中継していることを説明できる
