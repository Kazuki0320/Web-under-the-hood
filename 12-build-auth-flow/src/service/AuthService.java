package service;

import repository.UserRepository;
import store.TokenStore;

public class AuthService {
	private final UserRepository userRepository;
	private final TokenStore tokenStore;

	public AuthService(UserRepository userRepository, TokenStore tokenStore) {
		this.userRepository = userRepository;
		this.tokenStore = tokenStore;
	}
}
