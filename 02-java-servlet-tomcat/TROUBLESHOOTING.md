# Troubleshooting (`curl` / Tomcat)

## 1. まずHTTPステータスを見る

```bash
curl -i http://localhost:8081/hello
```

- `200`: アプリは正常
- `404`: URLまたはServletマッピング問題
- `500`: アプリ内部エラー（クラスロード失敗、例外など）
- 接続失敗（`Failed to connect`）: コンテナ未起動/ポート不一致

## 2. 最初に確認するコマンド

起動状態:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/02-java-servlet-tomcat
docker compose ps
```

ログ確認:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/02-java-servlet-tomcat
docker compose logs --no-color --tail=200 tomcat
```

配備ファイル確認:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/02-java-servlet-tomcat
docker compose exec -T tomcat sh -lc 'find /usr/local/tomcat/webapps/ROOT -maxdepth 4 -type f | sort'
```

## 3. 今回実際に起きた404の原因

症状:
- `curl -i http://localhost:8081/hello` が `HTTP/1.1 404`

原因:
- `web.xml` が `ROOT/web.xml` に配置されていた
- Tomcatが読む標準配置は `ROOT/WEB-INF/web.xml`

修正:
- `webapp/WEB-INF/web.xml` を作成し、そこへServletマッピングを定義

## 4. Java構文エラーを検出する方法

```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/02-java-servlet-tomcat
docker compose build --no-cache --progress=plain
```

- `Dockerfile` の `RUN javac ...` で構文エラーを検出する
- 失敗時は `javac` がファイル名と行番号を出力する
