package com.ruky.training.postgrest;

import com.ruky.training.domain.Competition;
import com.ruky.training.domain.CompetitionDivision;
import com.ruky.training.domain.CompetitionEvent;
import com.ruky.training.domain.CompetitionRegistration;
import com.ruky.training.domain.CompetitionResult;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

@Repository
public class CompetitionRepository extends PostgrestRepository {

    private static final String COMPETITIONS = "/competitions";
    private static final String REGISTRATIONS = "/competition_registrations";
    private static final String RESULTS = "/competition_results";

    public CompetitionRepository(RestClient client) {
        super(client);
    }

    public List<Competition> findAll() {
        return call(() -> {
            Competition[] rows = client.get()
                    .uri(builder -> builder.path(COMPETITIONS)
                            .queryParam("select", "*")
                            .queryParam("order", "starts_on.desc")
                            .build())
                    .retrieve()
                    .body(Competition[].class);
            return rows == null ? List.of() : Arrays.asList(rows);
        });
    }

    public Optional<Competition> findBySlug(String slug) {
        return call(() -> {
            Competition[] rows = client.get()
                    .uri(builder -> builder.path(COMPETITIONS)
                            .queryParam("select", "*")
                            .queryParam("slug", "eq." + slug)
                            .build())
                    .retrieve()
                    .body(Competition[].class);
            return rows == null || rows.length == 0 ? Optional.empty() : Optional.of(rows[0]);
        });
    }

    public List<CompetitionDivision> findDivisions(UUID competitionId) {
        return call(() -> {
            CompetitionDivision[] rows = client.get()
                    .uri(builder -> builder.path("/competition_divisions")
                            .queryParam("select", "*")
                            .queryParam("competition_id", "eq." + competitionId)
                            .queryParam("order", "name.asc")
                            .build())
                    .retrieve()
                    .body(CompetitionDivision[].class);
            return rows == null ? List.of() : Arrays.asList(rows);
        });
    }

    public List<CompetitionEvent> findEvents(UUID competitionId) {
        return call(() -> {
            CompetitionEvent[] rows = client.get()
                    .uri(builder -> builder.path("/competition_events")
                            .queryParam("select", "*")
                            .queryParam("competition_id", "eq." + competitionId)
                            .queryParam("order", "order_index.asc")
                            .build())
                    .retrieve()
                    .body(CompetitionEvent[].class);
            return rows == null ? List.of() : Arrays.asList(rows);
        });
    }

    public List<CompetitionRegistration> findRegistrations(UUID competitionId) {
        return call(() -> {
            CompetitionRegistration[] rows = client.get()
                    .uri(builder -> builder.path(REGISTRATIONS)
                            .queryParam("select", "*,athlete:athletes(*)")
                            .queryParam("competition_id", "eq." + competitionId)
                            .queryParam("order", "created_at.asc")
                            .build())
                    .retrieve()
                    .body(CompetitionRegistration[].class);
            return rows == null ? List.of() : Arrays.asList(rows);
        });
    }

    public List<CompetitionResult> findResults(UUID competitionId) {
        return call(() -> {
            CompetitionResult[] rows = client.get()
                    .uri(builder -> builder.path(RESULTS)
                            .queryParam("select", "*")
                            .queryParam("competition_id", "eq." + competitionId)
                            .build())
                    .retrieve()
                    .body(CompetitionResult[].class);
            return rows == null ? List.of() : Arrays.asList(rows);
        });
    }

    public CompetitionRegistration upsertRegistration(RegistrationWrite write) {
        return call(() -> {
            CompetitionRegistration[] rows = client.post()
                    .uri(builder -> builder.path(REGISTRATIONS)
                            .queryParam("on_conflict", "competition_id,athlete_id")
                            .queryParam("select", "*,athlete:athletes(*)")
                            .build())
                    .header("Prefer", "resolution=merge-duplicates,return=representation")
                    .body(write)
                    .retrieve()
                    .body(CompetitionRegistration[].class);
            if (rows == null || rows.length == 0) {
                throw new PostgrestException(HttpStatusCode.valueOf(502),
                        "PostgREST no devolvió la inscripción", null);
            }
            return rows[0];
        });
    }

    public boolean deleteRegistration(UUID registrationId) {
        return call(() -> {
            CompetitionRegistration[] rows = client.delete()
                    .uri(builder -> builder.path(REGISTRATIONS)
                            .queryParam("id", "eq." + registrationId)
                            .build())
                    .header("Prefer", "return=representation")
                    .retrieve()
                    .body(CompetitionRegistration[].class);
            return rows != null && rows.length > 0;
        });
    }

    public CompetitionResult upsertResult(ResultWrite write) {
        return call(() -> {
            CompetitionResult[] rows = client.post()
                    .uri(builder -> builder.path(RESULTS)
                            .queryParam("on_conflict", "event_id,athlete_id")
                            .build())
                    .header("Prefer", "resolution=merge-duplicates,return=representation")
                    .body(write)
                    .retrieve()
                    .body(CompetitionResult[].class);
            if (rows == null || rows.length == 0) {
                throw new PostgrestException(HttpStatusCode.valueOf(502),
                        "PostgREST no devolvió el resultado", null);
            }
            return rows[0];
        });
    }
}
