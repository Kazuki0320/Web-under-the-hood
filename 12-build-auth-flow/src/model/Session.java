package model;

public class Session {
    private final String userId;
    private final long expiresAt;

    public Session(String userId, long expiresAt) {
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public String getUserId() {
        return userId;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
