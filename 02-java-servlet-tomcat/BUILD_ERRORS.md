# Build Errors Log

## Error 001: `docker compose build --no-cache` で `no source files`

実行コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/02-java-servlet-tomcat
docker compose build --no-cache --progress=plain
```

主なエラー出力:
```text
find: './src/main/java': No such file or directory
error: no source files
```

どこで失敗したか:
- Docker build の `RUN ... javac ... $(find ./src/main/java -name "*.java")` ステップ

原因:
- `Dockerfile` 内で `COPY` したJavaファイルの場所と、`find ./src/main/java` で探す場所が一致していなかった。
- その結果、`javac` に渡すソースファイルが0件になって失敗した。

修正方針:
- `COPY` と `javac` の参照パスを一致させる。
- 例:
  - `COPY src ./src` して `find ./src/main/java -name "*.java"` を使う
  - もしくは `COPY ... /tmp/HelloServlet.java` したなら `javac ... /tmp/HelloServlet.java` を使う

再確認コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/02-java-servlet-tomcat
docker compose build --no-cache --progress=plain
```
