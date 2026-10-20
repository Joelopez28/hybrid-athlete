package com.ruky.training.service;

import com.ruky.training.domain.Athlete;
import com.ruky.training.error.ResourceNotFoundException;
import com.ruky.training.postgrest.AthleteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Demo mode: the app has no auth yet, so every write belongs to the configured
 * athlete. Replace this with the authenticated principal when auth lands.
 */
@Service
public class CurrentAthleteService {

    private final AthleteRepository repository;
    private final String handle;

    public CurrentAthleteService(
            AthleteRepository repository,
            @Value("${app.demo.athlete-handle:ruky}") String handle) {
        this.repository = repository;
        this.handle = handle;
    }

    public Athlete get() {
        return repository.findByHandle(handle)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el atleta demo '" + handle + "'. Reiniciá la base con make db-reset."));
    }

    public String handle() {
        return handle;
    }
}
