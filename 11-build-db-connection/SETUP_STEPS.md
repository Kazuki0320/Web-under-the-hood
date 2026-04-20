# DB接続実装 手順書（11）

このファイルは、`11-build-db-connection` を順番に進めるための実行手順です。  
各ステップで「やること」と「完了条件」を分けています。

## Step 0: 事前確認

やること:
- Java実行環境が利用できることを確認する
- 作業ディレクトリを `11-build-db-connection` に合わせる

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/11-build-db-connection
java -version
javac -version
```

完了条件:
- `java` / `javac` が利用可能
- `11-build-db-connection` 配下で作業開始できる

---

## Step 1: 最小構成のファイルを準備する

やること:
- `src` 配下にレイヤー別ディレクトリを作成する
- 最小構成の実装ファイルを用意する

コマンド:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/11-build-db-connection
mkdir -p src/controller src/service src/repository src/model src/db lib
touch src/Main.java
touch src/controller/UserController.java
touch src/service/UserService.java
touch src/repository/UserRepository.java
touch src/model/User.java
touch src/db/Database.java
```

完了条件:
- `Controller / Service / Repository / Model / DB` の最小ファイルが揃っている

---

## Step 2: JDBCドライバを準備する

やること:
- `sqlite-jdbc` を `lib/` に配置する
- コンパイル/実行時にクラスパスへ含める

コマンド例:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/11-build-db-connection
curl -L -o lib/sqlite-jdbc.jar \
  https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.51.2.0/sqlite-jdbc-3.51.2.0.jar
ls -lh lib
```

完了条件:
- `lib/sqlite-jdbc.jar` が存在する

---

## Step 3: DB接続と最小永続化を実装する

やること:
- 接続URL `jdbc:sqlite:sample.db` でSQLiteへ接続する
- `users` テーブル作成、`INSERT`、`SELECT` を実装する
- `Controller -> Service -> Repository` の流れで呼び出す

実装例（各ファイル）:

`src/db/Database.java`
```java
package db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class Database {
    private static final String URL = "jdbc:sqlite:sample.db";

    public static Connection getConnection() throws SQLException {
        Driver driver = new org.sqlite.JDBC();
        Connection connection = driver.connect(URL, new Properties());

        if (connection == null) {
            throw new SQLException("Failed to create SQLite connection for URL: " + URL);
        }
        return connection;
    }
}
```

`src/model/User.java`
```java
package model;

public class User {
    private final int id;
    private final String name;

    public User(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
```

`src/repository/UserRepository.java`
```java
package repository;

import db.Database;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    public void initializeTable() throws SQLException {
        String sql =
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL" +
            ")";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    public void insert(String name) throws SQLException {
        String sql = "INSERT INTO users(name) VALUES(?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, name FROM users ORDER BY id";
        List<User> users = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(new User(rs.getInt("id"), rs.getString("name")));
            }
        }

        return users;
    }
}
```

`src/service/UserService.java`
```java
package service;

import model.User;
import repository.UserRepository;

import java.sql.SQLException;
import java.util.List;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void initialize() throws SQLException {
        userRepository.initializeTable();
    }

    public void registerUser(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        userRepository.insert(name.trim());
    }

    public List<User> getAllUsers() throws SQLException {
        return userRepository.findAll();
    }
}
```

`src/controller/UserController.java`
```java
package controller;

import model.User;
import service.UserService;

import java.sql.SQLException;
import java.util.List;

public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void runDemo() throws SQLException {
        userService.initialize();
        userService.registerUser("Alice");
        userService.registerUser("Bob");

        List<User> users = userService.getAllUsers();
        for (User user : users) {
            System.out.println(user.getId() + ": " + user.getName());
        }
    }
}

```

`src/Main.java`
```java
import controller.UserController;
import repository.UserRepository;
import service.UserService;

public class Main {
    public static void main(String[] args) {
        try {
            UserRepository userRepository = new UserRepository();
            UserService userService = new UserService(userRepository);
            UserController userController = new UserController(userService);

            userController.runDemo();
        } catch (Exception e) {
            System.err.println("Failed to run app: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

完了条件:
- アプリ起動時にDBへ1件以上保存できる
- 保存済みデータを取得して表示できる

---

## Step 4: コンパイルして実行する

やること:
- JDBCドライバをクラスパスに含めてコンパイル/実行する

コマンド例:
```bash
cd /Users/ktoyo/Documents/Web-under-the-hood/11-build-db-connection
javac -cp "lib/*:src" $(find src -name "*.java")
java -cp "lib/*:src" Main
```

補足:
- `javac` に `lib/*` を含める理由:
  - JDBC関連クラス参照を解決してコンパイルするため
- `java` に `lib/*` を含める理由:
  - 実行時にSQLiteドライバ実装を読み込んで接続するため
  - 旧バージョン（例: `3.46.0.0`）では `slf4j` 追加が必要な場合がある

完了条件:
- 実行エラーなく起動できる
- `sample.db` が作成され、`users` データが確認できる

---

## Step 5: テストケースを設計する

やること:
- 自分で実装するテスト用に、先にケースを整理する

テストケース（実装は任意の方法でOK）:
1. 正常系: `registerUser("Alice")` で1件保存される
2. 正常系: `registerUser("  Alice  ")` で前後空白が除去されて保存される
3. 異常系: `registerUser(null)` で `IllegalArgumentException` になる
4. 異常系: `registerUser("   ")` で `IllegalArgumentException` になる
5. 正常系: 2件登録後の `getAllUsers()` が登録順で取得できる
6. 正常系: 初期化後（`initialize()`）に `users` テーブルが存在する

完了条件:
- 最低5ケース以上のテスト観点が定義できている

