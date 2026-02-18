# Dockerfile 設定ステップ（02-java-servlet-tomcat）

このドキュメントは、`Dockerfile` を自分で設定するための手順に限定したメモです。  
各ステップで「何を書くか」と「それをする意味」をセットで記載します。

## 完成イメージ

最終的には、次の `Dockerfile` を目指す。

```Dockerfile
FROM tomcat:10.1-jdk17-temurin

WORKDIR /work
COPY src ./src
COPY webapp ./webapp

RUN mkdir -p /usr/local/tomcat/webapps/ROOT/WEB-INF/classes \
    && javac -cp /usr/local/tomcat/lib/servlet-api.jar \
       -d /usr/local/tomcat/webapps/ROOT/WEB-INF/classes \
       $(find ./src/main/java -name "*.java")

RUN rm -rf /usr/local/tomcat/webapps/ROOT/*
RUN cp -r ./webapp/* /usr/local/tomcat/webapps/ROOT/ 2>/dev/null || true
```

## Step 1: ベースイメージを決める

記述:
```Dockerfile
FROM tomcat:10.1-jdk17-temurin
```

意味:
- Tomcat 10.1 を使って Servlet コンテナを用意する。
- JDK入りイメージを使うことで、後続の `javac` コンパイルが可能になる。

## Step 2: 作業ディレクトリを決める

記述:
```Dockerfile
WORKDIR /work
```

意味:
- 以降の `COPY` や `RUN` を `/work` 基準で実行できる。
- パス指定が短くなり、設定ミスを減らせる。

## Step 3: ソースとwebリソースをコピーする

記述:
```Dockerfile
COPY src ./src
COPY webapp ./webapp
```

意味:
- `src` は Javaソース（Servlet本体）。
- `webapp` は `web.xml` などTomcat配備用リソース。
- ビルド時に必要な素材をコンテナ内へ持ち込む。

## Step 4: ビルド時コンパイルを設定する（構文エラー検出）

記述:
```Dockerfile
RUN mkdir -p /usr/local/tomcat/webapps/ROOT/WEB-INF/classes \
    && javac -cp /usr/local/tomcat/lib/servlet-api.jar \
       -d /usr/local/tomcat/webapps/ROOT/WEB-INF/classes \
       $(find ./src/main/java -name "*.java")
```

意味:
- `javac` を Docker build 中に実行する。
- Java構文ミスがあるとこのステップで失敗し、イメージが作られない。
- `-cp /usr/local/tomcat/lib/servlet-api.jar` で Servlet API を解決する。
- `-d .../WEB-INF/classes` で `.class` をTomcatの読み込み先に出力する。

## Step 5: Tomcatの配備先を初期化する

記述:
```Dockerfile
RUN rm -rf /usr/local/tomcat/webapps/ROOT/*
```

意味:
- デフォルトROOTアプリを消し、今回の最小アプリだけを載せる。
- 不要ファイルの混在を避け、挙動をシンプルにする。

## Step 6: webappをROOTに配置する

記述:
```Dockerfile
RUN cp -r ./webapp/* /usr/local/tomcat/webapps/ROOT/ 2>/dev/null || true
```

意味:
- `web.xml` などを `ROOT` アプリとして配備する。
- `|| true` は、コピー対象が空だった場合でもビルドを落としにくくするため。

## 動作確認コマンド

構文チェック込みでビルドする:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/02-java-servlet-tomcat && docker compose build --no-cache
```

補足:
- このビルドで失敗した場合、`javac` のエラー出力に構文ミス箇所（ファイル名・行番号）が出る。
