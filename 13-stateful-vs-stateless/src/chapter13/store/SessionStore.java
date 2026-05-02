package chapter13.store;

import chapter13.model.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
    // TODO: Cookie認証用の sid->session マッピングを保持する。
    // TODO: save/find/delete の基本操作を提供する。
    // TODO: 必要に応じて有効期限チェックを実装する。
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public void save(String sid, Session session) {
        sessions.put(sid, session);
    }
    
    public Session find(String sid) {
        return sessions.get(sid);
    }

    public void delete(String sid) {
        sessions.remove(sid);
    }
}
