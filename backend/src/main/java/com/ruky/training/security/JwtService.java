package com.ruky.training.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.ruky.training.config.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ISSUER = "ruky";
    private static final String ROLE_CLAIM = "role";
    private static final String USER_ROLE = "app_user";
    private static final String SERVICE_ROLE = "app_service";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final Duration ttl;

    public JwtService(JwtProperties properties) {
        this.algorithm = Algorithm.HMAC256(properties.secret());
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        this.ttl = properties.ttl();
    }

    public String forAthlete(UUID athleteId) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(athleteId.toString())
                .withClaim(ROLE_CLAIM, USER_ROLE)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(ttl))
                .sign(algorithm);
    }

    public String serviceToken() {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject("backend")
                .withClaim(ROLE_CLAIM, SERVICE_ROLE)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(Duration.ofDays(365)))
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }
}
