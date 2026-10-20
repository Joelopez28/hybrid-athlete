package com.ruky.training.postgrest;

import com.ruky.training.domain.WeeklyActivity;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

/**
 * Reads the security-definer aggregate view. It exposes weekly volume/sessions
 * without exposing individual training rows across athletes.
 */
@Repository
public class ActivityRepository extends PostgrestRepository {

    public ActivityRepository(@Qualifier("postgrestUserClient") RestClient client) {
        super(client);
    }

    public List<WeeklyActivity> findSince(LocalDate from) {
        return call(() -> {
            WeeklyActivity[] rows = client.get()
                    .uri(builder -> builder.path("/athlete_weekly_activity")
                            .queryParam("select", "*")
                            .queryParam("week_start", "gte." + from)
                            .queryParam("order", "week_start.asc")
                            .queryParam("limit", 5000)
                            .build())
                    .retrieve()
                    .body(WeeklyActivity[].class);
            return rows == null ? List.of() : Arrays.asList(rows);
        });
    }
}
