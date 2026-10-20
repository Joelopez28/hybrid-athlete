package com.ruky.training.web;

import com.ruky.training.domain.AthleteTier;
import com.ruky.training.service.LeaderboardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService service;

    public LeaderboardController(LeaderboardService service) {
        this.service = service;
    }

    @GetMapping
    public List<LeaderboardEntryResponse> get(
            @RequestParam(defaultValue = "4") int weeks,
            @RequestParam(defaultValue = "VOLUME") LeaderboardMetric metric,
            @RequestParam(required = false) AthleteTier tier) {
        return service.get(weeks, metric, tier);
    }
}
