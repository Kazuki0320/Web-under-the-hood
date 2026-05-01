package chapter13.service;

import java.time.Instant;
import java.util.UUID;

import chapter13.repository.UserRepository;
import chapter13.model.Session;
import chapter13.model.User;
import chapter13.store.SessionStore;

public class CookieSessionAuthService {

    private final UserRepository userRepository;
    private final SessionStore sessionStore;
    // TODO: POST /cookie/login の認証フローを実装する。
    // TODO: UserRepositoryで資格情報を検証する。
    // TODO: sidを発行し、SessionStoreへ sid->session を保存する。
    // TODO: GET /cookie/me でsidからセッションを参照して認証する。
    // TODO: POST /cookie/logout でセッションを削除する。

    public CookieSessionAuthService(UserRepository userRepository, SessionStore sessionStore) {
        this.userRepository = userRepository;
        this.sessionStore = sessionStore;
    }

    public String login(String id, String password) {
        int SESSION_TTL_SECONDS = 3600;
        User result = userRepository.findByCredentials(id, password);
        if (result == null) {
            return null;
        }
        String sid = UUID.randomUUID().toString();
        long expiresAt = Instant.now().getEpochSecond() + SESSION_TTL_SECONDS;
        Session session = new Session(sid, result.getId(), expiresAt);
        sessionStore.save(sid, session);
        return sid;
    }
}
