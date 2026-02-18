# Concepts Notes

## Servletとは何か

- Servletは、HTTPリクエストを受けてHTTPレスポンスを返すJavaクラス
- Tomcatが `web.xml` の設定に従ってServletを呼び出す
- 例: `GET /hello` -> `HelloServlet#doGet` 実行 -> レスポンス本文を返す

## `jakarta` とは何か

- 旧Java EEの仕様群は現在 `Jakarta EE`
- Servlet APIパッケージは `jakarta.servlet.*`
- Tomcat 10系では `jakarta.*` を使うのが前提

## `COPY src ./src` の向き

`Dockerfile` の `COPY <ローカル> <イメージ内>` の順番:
- 左: ローカル（ビルドコンテキスト）
- 右: Dockerイメージ内（`WORKDIR` 基準）

例:
- `COPY src ./src` はローカル `src` をイメージ内 `./src` へコピー

## `docker compose build` と `docker compose up` の違い

`docker compose build --no-cache --progress=plain`
- イメージをビルドするだけ
- `--no-cache` で毎回フルビルド
- `--progress=plain` で詳細ログ表示

`docker compose up -d`
- コンテナを作成・起動する
- `-d` はバックグラウンド実行

使い分け:
1. まず `build` でエラーを潰す
2. 問題なければ `up -d` で起動する
