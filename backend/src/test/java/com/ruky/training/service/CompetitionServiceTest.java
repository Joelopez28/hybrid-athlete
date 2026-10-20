package com.ruky.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.Competition;
import com.ruky.training.domain.CompetitionDivision;
import com.ruky.training.domain.CompetitionEvent;
import com.ruky.training.domain.CompetitionRegistration;
import com.ruky.training.domain.CompetitionStatus;
import com.ruky.training.domain.RegistrationStatus;
import com.ruky.training.domain.ScoreDirection;
import com.ruky.training.domain.ScoreType;
import com.ruky.training.domain.Sex;
import com.ruky.training.error.ResourceNotFoundException;
import com.ruky.training.postgrest.CompetitionRepository;
import com.ruky.training.postgrest.RegistrationWrite;
import com.ruky.training.postgrest.ResultWrite;
import com.ruky.training.web.RegisterRequest;
import com.ruky.training.web.SubmitResultRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    private static final String SLUG = "strong-fit-ruky-1";
    private static final UUID COMP_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DIVISION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID EVENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID REGISTRATION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private CompetitionRepository repository;

    @Mock
    private CompetitionScoring scoring;

    @Mock
    private CurrentAthleteService currentAthlete;

    private CompetitionService service;

    private final Athlete eliteAthlete = athlete(AthleteTier.ELITE);
    private final CompetitionDivision eliteDivision = new CompetitionDivision(DIVISION_ID, COMP_ID,
            "Ruky Elite", AthleteTier.ELITE, 10);
    private final CompetitionEvent timeEvent = new CompetitionEvent(EVENT_ID, COMP_ID, "Fran", null,
            ScoreType.TIME, ScoreDirection.ASC, 1);

    @BeforeEach
    void setUp() {
        service = new CompetitionService(repository, scoring, currentAthlete);
    }

    @Test
    void shouldRejectOpenAthleteInEliteDivision() {
        Athlete openAthlete = athlete(AthleteTier.OPEN);
        stubLoad(CompetitionStatus.LIVE, openAthlete, List.of(), List.of());

        assertThatThrownBy(() -> service.register(SLUG, new RegisterRequest(DIVISION_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Elite");
        verify(repository, never()).upsertRegistration(any());
    }

    @Test
    void shouldRegisterEliteAthlete() {
        stubLoad(CompetitionStatus.LIVE, eliteAthlete, List.of(), List.of());

        service.register(SLUG, new RegisterRequest(DIVISION_ID));

        ArgumentCaptor<RegistrationWrite> captor = ArgumentCaptor.forClass(RegistrationWrite.class);
        verify(repository).upsertRegistration(captor.capture());
        assertThat(captor.getValue().divisionId()).isEqualTo(DIVISION_ID);
        assertThat(captor.getValue().athleteId()).isEqualTo(eliteAthlete.id());
        assertThat(captor.getValue().status()).isEqualTo(RegistrationStatus.REGISTERED);
    }

    @Test
    void shouldRejectResultWithoutRegistration() {
        stubLoad(CompetitionStatus.LIVE, eliteAthlete, List.of(), List.of());

        assertThatThrownBy(() -> service.submitResult(SLUG, EVENT_ID,
                new SubmitResultRequest(new BigDecimal("240"), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inscribite");
    }

    @Test
    void shouldFormatTimeAndSubmitResult() {
        CompetitionRegistration registration = registration();
        stubLoad(CompetitionStatus.LIVE, eliteAthlete, List.of(registration), List.of(registration));

        service.submitResult(SLUG, EVENT_ID, new SubmitResultRequest(new BigDecimal("240"), null, " ok "));

        ArgumentCaptor<ResultWrite> captor = ArgumentCaptor.forClass(ResultWrite.class);
        verify(repository).upsertResult(captor.capture());
        assertThat(captor.getValue().displayScore()).isEqualTo("04:00");
        assertThat(captor.getValue().rawScore()).isEqualByComparingTo("240");
        assertThat(captor.getValue().notes()).isEqualTo("ok");
    }

    @Test
    void shouldRejectResultsWhenCompetitionIsNotLive() {
        stubLoad(CompetitionStatus.DRAFT, eliteAthlete, List.of(registration()), List.of(registration()));

        assertThatThrownBy(() -> service.submitResult(SLUG, EVENT_ID,
                new SubmitResultRequest(new BigDecimal("240"), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abierta");
    }

    @Test
    void shouldWithdrawRegistration() {
        CompetitionRegistration registration = registration();
        stubLoad(CompetitionStatus.LIVE, eliteAthlete, List.of(registration), List.of(registration));
        when(repository.deleteRegistration(REGISTRATION_ID)).thenReturn(true);

        service.withdraw(SLUG);

        verify(repository).deleteRegistration(REGISTRATION_ID);
    }

    @Test
    void shouldFailWithdrawWhenNotRegistered() {
        stubLoad(CompetitionStatus.LIVE, eliteAthlete, List.of(), List.of());

        assertThatThrownBy(() -> service.withdraw(SLUG))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubLoad(CompetitionStatus status, Athlete athlete,
                          List<CompetitionRegistration> registrations,
                          List<CompetitionRegistration> scoringRegistrations) {
        Competition competition = new Competition(COMP_ID, SLUG, "Strong Fit", null, status,
                LocalDate.now(), LocalDate.now().plusDays(2), "Santiago");
        when(repository.findBySlug(SLUG)).thenReturn(Optional.of(competition));
        when(repository.findDivisions(COMP_ID)).thenReturn(List.of(eliteDivision));
        when(repository.findEvents(COMP_ID)).thenReturn(List.of(timeEvent));
        when(repository.findRegistrations(COMP_ID)).thenReturn(registrations);
        when(repository.findResults(COMP_ID)).thenReturn(List.of());
        when(currentAthlete.get()).thenReturn(athlete);
        when(scoring.score(any(), any(), any(), any())).thenReturn(List.of());
    }

    private CompetitionRegistration registration() {
        return new CompetitionRegistration(REGISTRATION_ID, COMP_ID, DIVISION_ID,
                eliteAthlete.id(), RegistrationStatus.REGISTERED, eliteAthlete);
    }

    private static Athlete athlete(AthleteTier tier) {
        return new Athlete(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"), "ruky", "Ruky",
                Sex.MALE, null, new BigDecimal("82"), "CL", null, tier, List.of());
    }
}
