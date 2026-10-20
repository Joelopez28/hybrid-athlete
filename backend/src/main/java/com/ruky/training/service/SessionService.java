package com.ruky.training.service;

import com.ruky.training.domain.Athlete;
import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.Exercise;
import com.ruky.training.domain.SessionType;
import com.ruky.training.domain.TrainingSession;
import com.ruky.training.error.ResourceNotFoundException;
import com.ruky.training.postgrest.EntryWrite;
import com.ruky.training.postgrest.ExerciseRepository;
import com.ruky.training.postgrest.SessionPage;
import com.ruky.training.postgrest.SessionRepository;
import com.ruky.training.postgrest.SessionWrite;
import com.ruky.training.web.CreateSessionRequest;
import com.ruky.training.web.EntryRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE = 10_000;

    private final SessionRepository sessions;
    private final ExerciseRepository exercises;
    private final CurrentAthleteService currentAthlete;

    public SessionService(SessionRepository sessions,
                          ExerciseRepository exercises,
                          CurrentAthleteService currentAthlete) {
        this.sessions = sessions;
        this.exercises = exercises;
        this.currentAthlete = currentAthlete;
    }

    public SessionPage list(Discipline discipline, SessionType type, int page, int size) {
        if (page < 0 || page > MAX_PAGE) {
            throw new IllegalArgumentException("page debe estar entre 0 y " + MAX_PAGE);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_PAGE_SIZE);
        }
        Athlete athlete = currentAthlete.get();
        return sessions.findPage(athlete.id(), discipline, type, size, page * size);
    }

    public TrainingSession get(UUID id) {
        Athlete athlete = currentAthlete.get();
        TrainingSession session = sessions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la sesión " + id));
        if (!session.athleteId().equals(athlete.id())) {
            throw new ResourceNotFoundException("No existe la sesión " + id);
        }
        return session;
    }

    public TrainingSession create(CreateSessionRequest request) {
        Athlete athlete = currentAthlete.get();
        SessionType type = request.sessionType();
        List<EntryRequest> requestedEntries = request.entries() == null ? List.of() : request.entries();

        if (requiresEntries(type) && requestedEntries.isEmpty()) {
            throw new IllegalArgumentException("Este tipo de sesión necesita al menos una serie");
        }
        if (type == SessionType.WOD && (request.wodFormat() == null || isBlank(request.wodScore()))) {
            throw new IllegalArgumentException("Un WOD necesita formato y resultado");
        }
        if (type == SessionType.HYROX_SIM && isBlank(request.wodScore())) {
            throw new IllegalArgumentException("Una simulación HYROX necesita tiempo total");
        }

        List<EntryWrite> entries = new ArrayList<>();
        int fallbackSetIndex = 0;
        for (EntryRequest entry : requestedEntries) {
            fallbackSetIndex++;
            Exercise exercise = null;
            if (entry.exerciseId() != null) {
                exercise = exercises.findById(entry.exerciseId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No existe el ejercicio " + entry.exerciseId()));
            }
            String movement = exercise != null ? exercise.name() : trimToNull(entry.movement());
            if (movement == null) {
                throw new IllegalArgumentException("Cada serie necesita un ejercicio o un movimiento");
            }
            if (entry.reps() == null && entry.seconds() == null) {
                throw new IllegalArgumentException("Cada serie necesita repeticiones o segundos");
            }
            entries.add(new EntryWrite(
                    entry.exerciseId(),
                    movement,
                    entry.setIndex() == null ? fallbackSetIndex : entry.setIndex(),
                    entry.reps(),
                    entry.weightKg(),
                    entry.seconds(),
                    entry.rpe()));
        }

        SessionWrite write = new SessionWrite(
                athlete.id(),
                request.performedOn(),
                request.discipline(),
                type,
                requiredTitle(request.title()),
                trimToNull(request.notes()),
                request.durationMin(),
                request.rpe(),
                request.wodFormat(),
                trimToNull(request.wodScore()),
                request.wodDivision(),
                entries.isEmpty() ? null : entries);

        return sessions.create(write);
    }

    public void delete(UUID id) {
        get(id);
        if (!sessions.delete(id)) {
            throw new ResourceNotFoundException("No existe la sesión " + id);
        }
    }

    private static boolean requiresEntries(SessionType type) {
        return type == SessionType.STRENGTH || type == SessionType.SKILL;
    }

    private static String requiredTitle(String value) {
        String title = value == null ? "" : value.trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }
        if (title.length() > 120) {
            throw new IllegalArgumentException("El título no puede superar 120 caracteres");
        }
        return title;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
