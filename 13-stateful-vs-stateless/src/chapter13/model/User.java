package chapter13.model;

public class User {
    // TODO: 認証済みユーザー情報（id、name、必要ならrole）を保持する。
    // TODO: 不変フィールドとコンストラクタを定義する。
    private final String id;
    private final String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
