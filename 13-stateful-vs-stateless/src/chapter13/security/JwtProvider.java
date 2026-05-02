package chapter13.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Instant;
import java.util.Date;

public class JwtProvider {
    // TODO: java-jwtのアルゴリズム・issuer・secret・有効期限を設定する。
    // TODO: クレーム付きJWTを生成する。
    // TODO: JWTを検証してクレームを取り出す。
    // TODO: 不正/期限切れトークン時のエラーを明確に返せるようにする。
    private static final String SECRET = "dev-secret-change-me";
    private static final String ISSUER = "chapter13-auth-server";
    private static final long EXPIRES_SECONDS = 3600L;

    public String issueToken(String userId) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(EXPIRES_SECONDS);
        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(userId)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm);
    }

    public String verifyAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
            DecodedJWT decoded = verifier.verify(token);
            return decoded.getSubject();
        } catch (JWTVerificationException e) {
            return null;
        }
    }
} 
