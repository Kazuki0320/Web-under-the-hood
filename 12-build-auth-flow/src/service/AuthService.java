package service;

import java.time.Instant;
import java.util.UUID;

import model.Session;
import repository.UserRepository;
import store.TokenStore;

public class AuthService {
	private static final long TOKEN_TTL_SECONDS = 3600;
	private final UserRepository userRepository;
	private final TokenStore tokenStore;

	public AuthService(UserRepository userRepository, TokenStore tokenStore) {
		this.userRepository = userRepository;
		this.tokenStore = tokenStore;
	}

	public String login (String id, String password) {
		if (!"demo".equals(id) || !"password123".equals(password)) {
			return null;
		}

		String token = UUID.randomUUID().toString();
		long expiresAt = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
		tokenStore.save(token, new Session(id, expiresAt));
		return token;
	}

	public String me(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			return null;
		}
		String token = authorizationHeader.substring("Bearer ".length()).trim();
		Session session = tokenStore.find(token);
		if (session == null) {
			return null;
		}

		long now = Instant.now().getEpochSecond();
		if (now >= session.getExpiresAt()) {
			tokenStore.remove(token);
			return null;
		}
		return session.getUserId();
	}
}
