package com.ruky.training.web;

import com.ruky.training.service.CompetitionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/competitions")
public class CompetitionController {

    private final CompetitionService service;

    public CompetitionController(CompetitionService service) {
        this.service = service;
    }

    @GetMapping
    public List<CompetitionSummaryResponse> list() {
        return service.list();
    }

    @GetMapping("/{slug}")
    public CompetitionDetailResponse detail(@PathVariable String slug) {
        return service.detail(slug);
    }

    @PostMapping("/{slug}/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@PathVariable String slug, @Valid @RequestBody RegisterRequest request) {
        service.register(slug, request);
    }

    @DeleteMapping("/{slug}/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable String slug) {
        service.withdraw(slug);
    }

    @PostMapping("/{slug}/events/{eventId}/result")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitResult(@PathVariable String slug,
                             @PathVariable UUID eventId,
                             @Valid @RequestBody SubmitResultRequest request) {
        service.submitResult(slug, eventId, request);
    }
}
