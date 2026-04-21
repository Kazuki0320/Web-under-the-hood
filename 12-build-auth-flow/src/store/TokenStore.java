package store;

import java.util.HashMap;
import java.util.Map;

import model.Session;

public class TokenStore {
	private final Map<String, Session> sessions = new HashMap<>();

	public void save(String token, Session session) {
		sessions.put(token, session);
	}

	public Session find (String token){
		return sessions.get(token);
	}

	public void remove(String token) {
		sessions.remove(token);
	}
} 
