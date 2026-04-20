package controller;

import java.io.IOException;
import java.net.Socket;

import service.AuthService;

public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	public void handle(Socket client) throws IOException {
		// 次で実装予定
	}
}
