package com.ruky.training.service;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.AthleteCredential;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.Sex;
import com.ruky.training.error.UnauthorizedException;
import com.ruky.training.postgrest.AuthRepository;
import com.ruky.training.security.JwtService;
import com.ruky.training.web.AthleteResponse;
import com.ruky.training.web.AuthResponse;
import com.ruky.training.web.LoginRequest;
import com.ruky.training.web.RegisterAthleteRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthRepository repository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterAthleteRequest request) {
        String handle = request.handle().trim().toLowerCase();
        String email = request.email().trim().toLowerCase();

        if (repository.findAthleteByHandle(handle).isPresent()) {
            throw new IllegalArgumentException("Ese usuario ya está registrado");
        }
        if (repository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ese email ya está registrado");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("handle", handle);
        payload.put("display_name", request.displayName().trim());
        payload.put("email", email);
        payload.put("password_hash", passwordEncoder.encode(request.password()));
        payload.put("sex", (request.sex() == null ? Sex.OTHER : request.sex()).name());
        if (request.bodyweightKg() != null) {
            payload.put("bodyweight_kg", request.bodyweightKg());
        }
        if (request.country() != null && !request.country().isBlank()) {
            payload.put("country", request.country().trim());
        }
        List<Discipline> disciplines = request.disciplines() == null ? List.of() : request.disciplines();
        payload.put("disciplines", disciplines.stream().map(Enum::name).toList());

        UUID athleteId = repository.register(payload);
        if (athleteId == null) {
            throw new IllegalStateException("No se pudo crear el atleta");
        }
        Athlete athlete = repository.findAthleteById(athleteId)
                .orElseThrow(() -> new IllegalStateException("No se pudo leer el atleta creado"));
        return new AuthResponse(jwtService.forAthlete(athleteId), AthleteResponse.from(athlete));
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        AthleteCredential credential = (identifier.contains("@")
                ? repository.findByEmail(identifier.toLowerCase())
                : repository.findByHandle(identifier.toLowerCase()))
                .orElseThrow(() -> new UnauthorizedException("Usuario o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.password(), credential.passwordHash())) {
            throw new UnauthorizedException("Usuario o contraseña incorrectos");
        }
        Athlete athlete = credential.athlete();
        if (athlete == null) {
            throw new UnauthorizedException("El atleta no está disponible");
        }
        return new AuthResponse(jwtService.forAthlete(athlete.id()), AthleteResponse.from(athlete));
    }
}
