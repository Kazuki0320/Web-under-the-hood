package chapter13.service;

import chapter13.model.User;
import chapter13.repository.UserRepository;
import chapter13.security.JwtProvider;

public class JwtAuthService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    // TODO: POST /jwt/login の認証フローを実装する。
    // TODO: UserRepositoryで資格情報を検証する。
    // TODO: JwtProviderでJWTを発行する。
    // TODO: GET /jwt/me でJWTを検証して認証する。
    // TODO: POST /jwt/logout の挙動（最小実装または失効管理）を実装する。

    public JwtAuthService(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    public String login(String id, String password) {
        User user = userRepository.findByCredentials(id, password);
        if (user == null) {
            return null;
        }
        return jwtProvider.issueToken(user.getId());
    }

    public User me(String token) {
        String userId = jwtProvider.verifyAndGetUserId(token);
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId);
    }
}
