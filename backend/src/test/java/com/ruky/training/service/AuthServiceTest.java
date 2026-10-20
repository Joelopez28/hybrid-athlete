package com.ruky.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteCredential;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.Sex;
import com.ruky.training.error.UnauthorizedException;
import com.ruky.training.postgrest.AuthRepository;
import com.ruky.training.security.JwtService;
import com.ruky.training.web.AuthResponse;
import com.ruky.training.web.LoginRequest;
import com.ruky.training.web.RegisterAthleteRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID ATHLETE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private AuthRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService service;

    private final Athlete athlete = new Athlete(ATHLETE_ID, "ruky", "Ruky", Sex.MALE, null,
            new BigDecimal("82"), "CL", null, AthleteTier.ELITE, List.of(Discipline.CROSSFIT));

    @Test
    void shouldLoginWithEmailAndIssueToken() {
        when(repository.findByEmail("ruky@ruky.demo")).thenReturn(Optional.of(
                new AthleteCredential(ATHLETE_ID, "ruky@ruky.demo", "$2a$hash", athlete)));
        when(passwordEncoder.matches("ruky1234", "$2a$hash")).thenReturn(true);
        when(jwtService.forAthlete(ATHLETE_ID)).thenReturn("token");

        AuthResponse response = service.login(new LoginRequest("ruky@ruky.demo", "ruky1234"));

        assertThat(response.token()).isEqualTo("token");
        assertThat(response.athlete().handle()).isEqualTo("ruky");
    }

    @Test
    void shouldRejectWrongPassword() {
        when(repository.findByHandle("ruky")).thenReturn(Optional.of(
                new AthleteCredential(ATHLETE_ID, "ruky@ruky.demo", "$2a$hash", athlete)));
        when(passwordEncoder.matches("bad", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("ruky", "bad")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("incorrectos");
    }

    @Test
    void shouldRegisterNormalizedAthleteAndIssueToken() {
        when(repository.findAthleteByHandle("newuser")).thenReturn(Optional.empty());
        when(repository.findByEmail("new@ruky.demo")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hash");
        when(repository.register(anyMap())).thenReturn(ATHLETE_ID);
        when(repository.findAthleteById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
        when(jwtService.forAthlete(ATHLETE_ID)).thenReturn("token");

        service.register(new RegisterAthleteRequest("  New User  ", "newuser", " NEW@ruky.demo ",
                "password123", Sex.FEMALE, new BigDecimal("64.5"), "CL",
                List.of(Discipline.HYROX)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(repository).register(captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertThat(payload)
                .containsEntry("handle", "newuser")
                .containsEntry("display_name", "New User")
                .containsEntry("email", "new@ruky.demo")
                .containsEntry("password_hash", "$2a$hash")
                .doesNotContainKey("tier");
    }

    @Test
    void shouldRejectDuplicateHandle() {
        when(repository.findAthleteByHandle("ruky")).thenReturn(Optional.of(athlete));

        assertThatThrownBy(() -> service.register(new RegisterAthleteRequest("Ruky", "ruky",
                "other@ruky.demo", "password123", Sex.MALE, null, null, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("usuario");
    }
}
