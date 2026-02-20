# Tomcat目線メモ

このドキュメントは、会話内で説明した「Tomcat目線」の内容をまとめたものです。

## 1. `web.xml` を読み込んで起動するまで（Tomcatの視点）

1. Tomcatが起動される（例: `catalina.sh run`）。
2. `server.xml` を読み、`Service` / `Engine` / `Host` / `Context` の構造を組み立てる。
3. `webapps/` 配下（またはデプロイ対象）を見てWebアプリを検出する。
4. 各アプリの `WEB-INF/web.xml` を読み、設定をパースする。
5. `servlet` / `servlet-mapping` / `filter` / `filter-mapping` / `listener` / `welcome-file` / `error-page` などを登録する。
6. 必要に応じて `conf/web.xml`（グローバル設定）・`web-fragment.xml`・アノテーション設定と統合する。
7. `<load-on-startup>` のServletを初期化し、`ServletContextListener#contextInitialized` を実行する。
8. HTTPコネクタで待受を開始し、リクエストを受け付ける。
9. 停止時は `destroy()` や `contextDestroyed()` を呼んで終了する。

要点: `web.xml` は「このWebアプリをどう配線して動かすか」をTomcatに伝える設計図。


## この構成にJSPを混ぜる場合

基本フローは「Servletで処理し、JSPで表示」。

1. リクエストをServletが受ける。
2. Servletでデータを準備する。
3. `RequestDispatcher#forward` でJSPへ渡す。
4. JSPがHTMLを生成して返す。

### 例の配置

```text
myapp/
  WEB-INF/
    web.xml
    jsp/
      home.jsp
  index.jsp
```

### `web.xml` 例

```xml
<web-app>
  <servlet>
    <servlet-name>home</servlet-name>
    <servlet-class>com.example.HomeServlet</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>home</servlet-name>
    <url-pattern>/home</url-pattern>
  </servlet-mapping>
</web-app>
```

### Servlet側イメージ

```java
request.setAttribute("message", "Hello from Servlet");
request.getRequestDispatcher("/WEB-INF/jsp/home.jsp").forward(request, response);
```

### JSP側イメージ

```jsp
<h1>${message}</h1>
```

補足:
- `/` で表示させるなら `index.jsp` を置くか、`/` をServletにマップする。
- JSPには表示処理を寄せ、業務ロジックはServlet/Service側に置く。
