# 11 Build DB Connection

DB接続をテーマに、APIサーバーから永続化レイヤーへつなぐ最小構成を学ぶためのディレクトリ。

## 目的
- フレームワークなしでDB接続の基本を理解する
- Controller / Service / Repository の責務分離を前提に実装を進める

## 最小ゴール
- DBへ接続し、`INSERT` / `SELECT` の基本操作を実行できる
- APIからDB永続化までの流れを説明できる
- Controller / Service / Repository を最低限分けて実装できる

## 最小構成（レイヤー）
- Controller: HTTPリクエストを受け取り、Serviceを呼び出してレスポンスを返す
- Service: 業務ロジックを実装し、Repositoryを呼び出す
- Repository: SQLiteへの接続とSQL実行（`INSERT` / `SELECT`）を担当する

## フォルダ構成（最小）
```text
11-build-db-connection/
  README.md
  src/
    Main.java
    controller/
      UserController.java
    service/
      UserService.java
    repository/
      UserRepository.java
    model/
      User.java
```

## 事前準備（SQLite接続）
- JDBCドライバとして `org.xerial:sqlite-jdbc` を導入する
- 接続URLは `jdbc:sqlite:sample.db` を使用する

必要なimport（Java/JDBC）:
```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
```
