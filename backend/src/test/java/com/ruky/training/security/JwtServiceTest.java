package com.ruky.training.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.ruky.training.config.JwtProperties;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService service = new JwtService(
            new JwtProperties("a-very-long-test-secret-with-32-chars", Duration.ofHours(1)));

    @Test
    void shouldIssueAthleteTokenWithRoleAndSubject() {
        UUID athleteId = UUID.randomUUID();

        DecodedJWT decoded = service.verify(service.forAthlete(athleteId));

        assertThat(decoded.getSubject()).isEqualTo(athleteId.toString());
        assertThat(decoded.getClaim("role").asString()).isEqualTo("app_user");
        assertThat(decoded.getIssuer()).isEqualTo("ruky");
    }

    @Test
    void shouldIssueServiceTokenWithServiceRole() {
        DecodedJWT decoded = service.verify(service.serviceToken());

        assertThat(decoded.getSubject()).isEqualTo("backend");
        assertThat(decoded.getClaim("role").asString()).isEqualTo("app_service");
    }
}
