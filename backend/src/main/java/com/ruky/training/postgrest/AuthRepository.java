package com.ruky.training.postgrest;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteCredential;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

@Repository
public class AuthRepository extends PostgrestRepository {

    public AuthRepository(@Qualifier("postgrestServiceClient") RestClient client) {
        super(client);
    }

    public Optional<AthleteCredential> findByEmail(String email) {
        return call(() -> {
            AthleteCredential[] rows = client.get()
                    .uri(builder -> builder.path("/athlete_credentials")
                            .queryParam("select", "*,athlete:athletes(*)")
                            .queryParam("email", "eq." + email.toLowerCase())
                            .build())
                    .retrieve()
                    .body(AthleteCredential[].class);
            return first(rows);
        });
    }

    public Optional<AthleteCredential> findByAthleteId(UUID athleteId) {
        return call(() -> {
            AthleteCredential[] rows = client.get()
                    .uri(builder -> builder.path("/athlete_credentials")
                            .queryParam("select", "*,athlete:athletes(*)")
                            .queryParam("athlete_id", "eq." + athleteId)
                            .build())
                    .retrieve()
                    .body(AthleteCredential[].class);
            return first(rows);
        });
    }

    public Optional<AthleteCredential> findByHandle(String handle) {
        return findAthleteByHandle(handle).flatMap(athlete -> findByAthleteId(athlete.id()));
    }

    public Optional<Athlete> findAthleteByHandle(String handle) {
        return call(() -> {
            Athlete[] rows = client.get()
                    .uri(builder -> builder.path("/athletes")
                            .queryParam("select", "*")
                            .queryParam("handle", "eq." + handle.toLowerCase())
                            .build())
                    .retrieve()
                    .body(Athlete[].class);
            return rows == null || rows.length == 0 ? Optional.empty() : Optional.of(rows[0]);
        });
    }

    public Optional<Athlete> findAthleteById(UUID id) {
        return call(() -> {
            Athlete[] rows = client.get()
                    .uri(builder -> builder.path("/athletes")
                            .queryParam("select", "*")
                            .queryParam("id", "eq." + id)
                            .build())
                    .retrieve()
                    .body(Athlete[].class);
            return rows == null || rows.length == 0 ? Optional.empty() : Optional.of(rows[0]);
        });
    }

    public UUID register(Map<String, Object> payload) {
        return call(() -> client.post()
                .uri("/rpc/register_athlete")
                .body(Map.of("payload", payload))
                .retrieve()
                .body(UUID.class));
    }

    private static Optional<AthleteCredential> first(AthleteCredential[] rows) {
        return rows == null ? Optional.empty() : Arrays.stream(rows).findFirst();
    }
}
