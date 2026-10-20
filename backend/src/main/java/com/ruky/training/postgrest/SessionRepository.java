package com.ruky.training.postgrest;

import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.TrainingSession;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

@Repository
public class SessionRepository extends PostgrestRepository {

    private static final String SESSIONS = "/training_sessions";
    private static final String WITH_ENTRIES = "*,session_entries(*)";

    public SessionRepository(RestClient client) {
        super(client);
    }

    public SessionPage findPage(UUID athleteId, Discipline discipline, SessionType type,
                                 int limit, int offset) {
        return call(() -> {
            ResponseEntity<TrainingSession[]> response = client.get()
                    .uri(builder -> {
                        builder.path(SESSIONS)
                                .queryParam("select", WITH_ENTRIES)
                                .queryParam("athlete_id", "eq." + athleteId)
                                .queryParam("order", "performed_on.desc,created_at.desc")
                                .queryParam("limit", limit)
                                .queryParam("offset", offset);
                        if (discipline != null) {
                            builder.queryParam("discipline", "eq." + discipline.name());
                        }
                        if (type != null) {
                            builder.queryParam("session_type", "eq." + type.name());
                        }
                        return builder.build();
                    })
                    .header("Prefer", "count=exact")
                    .retrieve()
                    .toEntity(TrainingSession[].class);

            List<TrainingSession> sessions = sorted(response.getBody());
            return new SessionPage(sessions, parseTotal(response.getHeaders(), sessions.size()));
        });
    }

    public List<TrainingSession> findAllSince(UUID athleteId, LocalDate from) {
        return call(() -> {
            TrainingSession[] rows = client.get()
                    .uri(builder -> builder.path(SESSIONS)
                            .queryParam("select", WITH_ENTRIES)
                            .queryParam("athlete_id", "eq." + athleteId)
                            .queryParam("performed_on", "gte." + from)
                            .queryParam("order", "performed_on.asc")
                            .queryParam("limit", 500)
                            .build())
                    .retrieve()
                    .body(TrainingSession[].class);
            return sorted(rows);
        });
    }

    public List<TrainingSession> findAllSince(LocalDate from) {
        return call(() -> {
            TrainingSession[] rows = client.get()
                    .uri(builder -> builder.path(SESSIONS)
                            .queryParam("select", WITH_ENTRIES)
                            .queryParam("performed_on", "gte." + from)
                            .queryParam("order", "performed_on.asc")
                            .queryParam("limit", 2000)
                            .build())
                    .retrieve()
                    .body(TrainingSession[].class);
            return sorted(rows);
        });
    }

    public Optional<TrainingSession> findById(UUID id) {
        return call(() -> {
            TrainingSession[] rows = client.get()
                    .uri(builder -> builder.path(SESSIONS)
                            .queryParam("select", WITH_ENTRIES)
                            .queryParam("id", "eq." + id)
                            .build())
                    .retrieve()
                    .body(TrainingSession[].class);
            return rows == null || rows.length == 0
                    ? Optional.empty()
                    : Optional.of(rows[0].withSortedEntries());
        });
    }

    public TrainingSession create(SessionWrite write) {
        return call(() -> {
            UUID id = client.post()
                    .uri("/rpc/create_session")
                    .body(java.util.Map.of("payload", write))
                    .retrieve()
                    .body(UUID.class);
            if (id == null) {
                throw new PostgrestException(org.springframework.http.HttpStatusCode.valueOf(502),
                        "PostgREST no devolvió el id de la sesión creada", null);
            }
            return findById(id).orElseThrow(() -> new PostgrestException(
                    org.springframework.http.HttpStatusCode.valueOf(502),
                    "La sesión se creó pero no se pudo leer", null));
        });
    }

    public boolean delete(UUID id) {
        return call(() -> {
            TrainingSession[] rows = client.delete()
                    .uri(builder -> builder.path(SESSIONS)
                            .queryParam("id", "eq." + id)
                            .build())
                    .header("Prefer", "return=representation")
                    .retrieve()
                    .body(TrainingSession[].class);
            return rows != null && rows.length > 0;
        });
    }

    private static List<TrainingSession> sorted(TrainingSession[] rows) {
        if (rows == null) {
            return List.of();
        }
        return Arrays.stream(rows).map(TrainingSession::withSortedEntries).toList();
    }
}
