package com.example.taskflow.postgrest;

import com.example.taskflow.domain.Task;
import java.util.List;

public record TaskPage(List<Task> items, long total) {

    public TaskPage {
        items = List.copyOf(items);
    }

    public int totalPages(int size) {
        return size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}
