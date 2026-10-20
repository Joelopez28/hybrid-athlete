package com.ruky.training.postgrest;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.EliteStandard;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

@Repository
public class EliteRepository extends PostgrestRepository {

    private final RestClient serviceClient;

    public EliteRepository(@Qualifier("postgrestUserClient") RestClient userClient,
                           @Qualifier("postgrestServiceClient") RestClient serviceClient) {
        super(userClient);
        this.serviceClient = serviceClient;
    }

    public List<EliteStandard> findStandards() {
        return call(() -> {
            EliteStandard[] rows = client.get()
                    .uri(builder -> builder.path("/elite_standards")
                            .queryParam("select", "*")
                            .queryParam("active", "eq.true")
                            .queryParam("order", "code.asc")
                            .build())
                    .retrieve()
                    .body(EliteStandard[].class);
            return rows == null ? List.of() : Arrays.asList(rows);
        });
    }

    public boolean promote(UUID athleteId) {
        return call(() -> {
            Athlete[] rows = serviceClient.patch()
                    .uri(builder -> builder.path("/athletes")
                            .queryParam("id", "eq." + athleteId)
                            .build())
                    .header("Prefer", "return=representation")
                    .body(Map.of("tier", "ELITE"))
                    .retrieve()
                    .body(Athlete[].class);
            return rows != null && rows.length > 0;
        });
    }
}
