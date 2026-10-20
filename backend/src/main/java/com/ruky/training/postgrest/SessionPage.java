package com.ruky.training.postgrest;

import com.ruky.training.domain.TrainingSession;
import java.util.List;

public record SessionPage(List<TrainingSession> items, long total) {

    public SessionPage {
        items = List.copyOf(items);
    }

    public int totalPages(int size) {
        return size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}
