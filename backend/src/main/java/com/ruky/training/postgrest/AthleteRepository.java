package com.ruky.training.postgrest;

import com.ruky.training.domain.Athlete;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

@Repository
public class AthleteRepository extends PostgrestRepository {

    public AthleteRepository(RestClient client) {
        super(client);
    }

    public Optional<Athlete> findByHandle(String handle) {
        return call(() -> {
            Athlete[] rows = client.get()
                    .uri(builder -> builder.path("/athletes")
                            .queryParam("select", "*")
                            .queryParam("handle", "eq." + handle)
                            .build())
                    .retrieve()
                    .body(Athlete[].class);
            return first(rows);
        });
    }

    public Optional<Athlete> findById(UUID id) {
        return call(() -> {
            Athlete[] rows = client.get()
                    .uri(builder -> builder.path("/athletes")
                            .queryParam("select", "*")
                            .queryParam("id", "eq." + id)
                            .build())
                    .retrieve()
                    .body(Athlete[].class);
            return first(rows);
        });
    }

    public List<Athlete> findAll() {
        return call(() -> {
            Athlete[] rows = client.get()
                    .uri(builder -> builder.path("/athletes")
                            .queryParam("select", "*")
                            .queryParam("order", "display_name.asc")
                            .build())
                    .retrieve()
                    .body(Athlete[].class);
            return rows == null ? List.of() : Arrays.asList(rows);
        });
    }

    private static Optional<Athlete> first(Athlete[] rows) {
        return rows == null || rows.length == 0 ? Optional.empty() : Optional.of(rows[0]);
    }
}
