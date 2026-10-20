package com.ruky.training.service;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.Competition;
import com.ruky.training.domain.CompetitionDivision;
import com.ruky.training.domain.CompetitionEvent;
import com.ruky.training.domain.CompetitionRegistration;
import com.ruky.training.domain.CompetitionResult;
import com.ruky.training.domain.CompetitionStatus;
import com.ruky.training.domain.RegistrationStatus;
import com.ruky.training.error.ResourceNotFoundException;
import com.ruky.training.postgrest.CompetitionRepository;
import com.ruky.training.postgrest.RegistrationWrite;
import com.ruky.training.postgrest.ResultWrite;
import com.ruky.training.web.CompetitionDetailResponse;
import com.ruky.training.web.CompetitionDivisionResponse;
import com.ruky.training.web.CompetitionEventResponse;
import com.ruky.training.web.CompetitionRegistrationResponse;
import com.ruky.training.web.CompetitionSummaryResponse;
import com.ruky.training.web.EventScoreResponse;
import com.ruky.training.web.LeaderboardResponse;
import com.ruky.training.web.LeaderboardRowResponse;
import com.ruky.training.web.MyResultResponse;
import com.ruky.training.web.RegisterRequest;
import com.ruky.training.web.SubmitResultRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CompetitionService {

    private final CompetitionRepository repository;
    private final CompetitionScoring scoring;
    private final CurrentAthleteService currentAthlete;

    public CompetitionService(CompetitionRepository repository,
                              CompetitionScoring scoring,
                              CurrentAthleteService currentAthlete) {
        this.repository = repository;
        this.scoring = scoring;
        this.currentAthlete = currentAthlete;
    }

    public List<CompetitionSummaryResponse> list() {
        return repository.findAll().stream().map(CompetitionSummaryResponse::from).toList();
    }

    public CompetitionDetailResponse detail(String slug) {
        Context context = load(slug);
        Map<UUID, Long> participantsByDivision = context.registrations().stream()
                .filter(registration -> registration.status() == RegistrationStatus.REGISTERED)
                .collect(Collectors.groupingBy(CompetitionRegistration::divisionId, Collectors.counting()));

        List<CompetitionDivisionResponse> divisions = context.divisions().stream()
                .map(division -> CompetitionDivisionResponse.from(division,
                        participantsByDivision.getOrDefault(division.id(), 0L).intValue()))
                .toList();

        CompetitionRegistration registration = context.registration();
        String divisionName = registration == null ? null : context.divisions().stream()
                .filter(division -> division.id().equals(registration.divisionId()))
                .map(CompetitionDivision::name)
                .findFirst()
                .orElse(null);

        CompetitionRegistrationResponse myRegistration = registration == null
                ? null
                : CompetitionRegistrationResponse.from(registration, divisionName);

        List<MyResultResponse> myResults = new ArrayList<>();
        for (CompetitionEvent event : context.events()) {
            CompetitionResult result = context.results().stream()
                    .filter(row -> row.eventId().equals(event.id()))
                    .filter(row -> row.athleteId().equals(context.athlete().id()))
                    .findFirst()
                    .orElse(null);
            if (result == null) {
                continue;
            }
            EventScoreResponse score = findMyEventScore(context, registration, result.eventId());
            myResults.add(new MyResultResponse(
                    result.eventId(),
                    result.rawScore(),
                    result.displayScore(),
                    result.notes(),
                    score == null ? null : score.place(),
                    score == null ? 0 : score.points()));
        }

        return new CompetitionDetailResponse(
                CompetitionSummaryResponse.from(context.competition()),
                divisions,
                context.events().stream().map(CompetitionEventResponse::from).toList(),
                myRegistration,
                myResults,
                context.leaderboards());
    }

    public void register(String slug, RegisterRequest request) {
        Context context = load(slug);
        requireLive(context.competition());

        CompetitionDivision division = context.divisions().stream()
                .filter(candidate -> candidate.id().equals(request.divisionId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No existe la división elegida"));

        Athlete athlete = context.athlete();
        if (division.tier() == AthleteTier.ELITE && athlete.tier() != AthleteTier.ELITE) {
            throw new IllegalArgumentException("La división Ruky Elite es solo para atletas Elite");
        }

        boolean alreadyInDivision = context.registration() != null
                && context.registration().divisionId().equals(division.id());
        boolean hasResults = context.results().stream()
                .anyMatch(result -> result.athleteId().equals(athlete.id()));
        if (context.registration() != null && !alreadyInDivision && hasResults) {
            throw new IllegalArgumentException(
                    "Ya cargaste resultados; no podés cambiar de división en esta competencia");
        }
        long registered = context.registrations().stream()
                .filter(row -> row.status() == RegistrationStatus.REGISTERED)
                .filter(row -> row.divisionId().equals(division.id()))
                .count();
        if (!alreadyInDivision && division.capacity() != null && registered >= division.capacity()) {
            throw new IllegalArgumentException("La división está completa");
        }

        repository.upsertRegistration(new RegistrationWrite(
                context.competition().id(),
                division.id(),
                athlete.id(),
                RegistrationStatus.REGISTERED));
    }

    public void withdraw(String slug) {
        Context context = load(slug);
        requireLive(context.competition());
        if (context.registration() == null) {
            throw new ResourceNotFoundException("No estás inscripto en esta competencia");
        }
        if (!repository.deleteRegistration(context.registration().id())) {
            throw new ResourceNotFoundException("No se pudo eliminar la inscripción");
        }
    }

    public void submitResult(String slug, UUID eventId, SubmitResultRequest request) {
        Context context = load(slug);
        requireLive(context.competition());

        CompetitionEvent event = context.events().stream()
                .filter(candidate -> candidate.id().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No existe la prueba elegida"));

        if (context.registration() == null) {
            throw new IllegalArgumentException("Primero inscribite a la competencia");
        }
        if (request.rawScore() == null || request.rawScore().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El resultado no puede ser negativo");
        }
        if (event.scoreType() == com.ruky.training.domain.ScoreType.TIME
                && request.rawScore().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Un tiempo debe ser mayor a cero");
        }

        repository.upsertResult(new ResultWrite(
                context.competition().id(),
                event.id(),
                context.athlete().id(),
                request.rawScore(),
                displayScore(request, event),
                trimToNull(request.notes())));
    }

    private Context load(String slug) {
        Competition competition = repository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la competencia '" + slug + "'"));
        List<CompetitionDivision> divisions = repository.findDivisions(competition.id());
        List<CompetitionEvent> events = repository.findEvents(competition.id());
        List<CompetitionRegistration> registrations = repository.findRegistrations(competition.id());
        List<CompetitionResult> results = repository.findResults(competition.id());
        Athlete athlete = currentAthlete.get();
        CompetitionRegistration registration = registrations.stream()
                .filter(row -> row.athleteId().equals(athlete.id()))
                .filter(row -> row.status() == RegistrationStatus.REGISTERED)
                .findFirst()
                .orElse(null);
        List<LeaderboardResponse> leaderboards = scoring.score(divisions, events, registrations, results);
        return new Context(competition, divisions, events, registrations, results, athlete,
                registration, leaderboards);
    }

    private static EventScoreResponse findMyEventScore(Context context,
                                                       CompetitionRegistration registration,
                                                       UUID eventId) {
        if (registration == null) {
            return null;
        }
        LeaderboardResponse board = context.leaderboards().stream()
                .filter(candidate -> candidate.divisionId().equals(registration.divisionId()))
                .findFirst()
                .orElse(null);
        if (board == null) {
            return null;
        }
        LeaderboardRowResponse row = board.rows().stream()
                .filter(candidate -> candidate.athleteId().equals(context.athlete().id()))
                .findFirst()
                .orElse(null);
        if (row == null) {
            return null;
        }
        return row.eventScores().stream()
                .filter(score -> score.eventId().equals(eventId))
                .findFirst()
                .orElse(null);
    }

    private static String displayScore(SubmitResultRequest request, CompetitionEvent event) {
        if (request.displayScore() != null && !request.displayScore().isBlank()) {
            return request.displayScore().trim();
        }
        return switch (event.scoreType()) {
            case TIME -> formatSeconds(request.rawScore());
            case WEIGHT -> stripZeros(request.rawScore()) + " kg";
            case REPS -> request.rawScore().setScale(0, RoundingMode.HALF_UP) + " reps";
        };
    }

    private static String formatSeconds(BigDecimal rawSeconds) {
        int total = rawSeconds.setScale(0, RoundingMode.HALF_UP).intValue();
        int hours = total / 3600;
        int minutes = (total % 3600) / 60;
        int seconds = total % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
    }

    private static String stripZeros(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void requireLive(Competition competition) {
        if (competition.status() != CompetitionStatus.LIVE) {
            throw new IllegalArgumentException("La competencia no está abierta");
        }
    }

    private record Context(
            Competition competition,
            List<CompetitionDivision> divisions,
            List<CompetitionEvent> events,
            List<CompetitionRegistration> registrations,
            List<CompetitionResult> results,
            Athlete athlete,
            CompetitionRegistration registration,
            List<LeaderboardResponse> leaderboards) {
    }
}
