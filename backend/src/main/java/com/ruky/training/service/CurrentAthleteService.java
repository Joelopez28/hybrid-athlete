package com.ruky.training.service;

import com.ruky.training.domain.Athlete;
import com.ruky.training.error.ResourceNotFoundException;
import com.ruky.training.error.UnauthorizedException;
import com.ruky.training.postgrest.AthleteRepository;
import com.ruky.training.security.AuthContext;
import org.springframework.stereotype.Service;

@Service
public class CurrentAthleteService {

    private final AthleteRepository repository;

    public CurrentAthleteService(AthleteRepository repository) {
        this.repository = repository;
    }

    public Athlete get() {
        AuthContext.Principal principal = AuthContext.principal()
                .orElseThrow(() -> new UnauthorizedException("Iniciá sesión para continuar"));
        return repository.findById(principal.athleteId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el atleta autenticado"));
    }
}
