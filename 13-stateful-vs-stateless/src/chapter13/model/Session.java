package chapter13.model;

public class Session {
    // TODO: Cookie認証用セッション情報（sid、userId、expiresAt）を保持する。
    // TODO: 不変フィールドとコンストラクタを定義する。
    // TODO: 必要に応じて有効期限判定ヘルパーを用意する。
    // sid=cookieの値
    // 期限判定用=いつ失効するか
    private final String sid;
    private final String userId;
    private final long expiresAtEpochSecond;

    public Session(String sid, String userId, long expiresAtEpochSecond) {
        this.sid = sid;
        this.userId = userId;
        this.expiresAtEpochSecond = expiresAtEpochSecond;
    }

    public String getSid() {
        return sid;
    }

    public String getUserId() {
        return userId;
    }

    public long getExpiresAtEpochSecond() {
        return expiresAtEpochSecond;
    }

    public boolean isExpired(long nowEpochSecond) {
        return nowEpochSecond >= expiresAtEpochSecond;
    }
}
