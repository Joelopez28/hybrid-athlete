package com.ruky.training.web;

import com.ruky.training.domain.Discipline;
import com.ruky.training.domain.Exercise;
import com.ruky.training.postgrest.ExerciseRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseRepository repository;

    public ExerciseController(ExerciseRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Exercise> list(@RequestParam(required = false) Discipline discipline) {
        return repository.findAll(discipline);
    }
}
