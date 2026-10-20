package com.ruky.training.postgrest;

import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.Exercise;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

@Repository
public class ExerciseRepository extends PostgrestRepository {

    public ExerciseRepository(@Qualifier("postgrestUserClient") RestClient client) {
        super(client);
    }

    public List<Exercise> findAll(Discipline discipline) {
        return call(() -> {
            Exercise[] rows = client.get()
                    .uri(builder -> {
                        builder.path("/exercises")
                                .queryParam("select", "*")
                                .queryParam("order", "discipline.asc,name.asc");
                        if (discipline != null) {
                            builder.queryParam("discipline", "eq." + discipline.name());
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(Exercise[].class);
            return rows == null ? List.of() : Arrays.asList(rows);
        });
    }

    public Optional<Exercise> findById(UUID id) {
        return call(() -> {
            Exercise[] rows = client.get()
                    .uri(builder -> builder.path("/exercises")
                            .queryParam("select", "*")
                            .queryParam("id", "eq." + id)
                            .build())
                    .retrieve()
                    .body(Exercise[].class);
            return rows == null || rows.length == 0 ? Optional.empty() : Optional.of(rows[0]);
        });
    }
}
