package com.ruky.training.web;

import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.SessionType;
import com.ruky.training.service.SessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<SessionResponse> list(
            @RequestParam(required = false) Discipline discipline,
            @RequestParam(required = false) SessionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = service.list(discipline, type, page, size);
        return PageResponse.of(
                result.items().stream().map(SessionResponse::from).toList(),
                page,
                size,
                result.total());
    }

    @GetMapping("/{id}")
    public SessionResponse get(@PathVariable UUID id) {
        return SessionResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
        SessionResponse created = SessionResponse.from(service.create(request));
        return ResponseEntity.created(URI.create("/api/sessions/" + created.id())).body(created);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
