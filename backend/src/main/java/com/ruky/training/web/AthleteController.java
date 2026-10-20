package com.ruky.training.web;

import com.ruky.training.postgrest.AthleteRepository;
import com.ruky.training.service.CurrentAthleteService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AthleteController {

    private final CurrentAthleteService currentAthlete;
    private final AthleteRepository athletes;

    public AthleteController(CurrentAthleteService currentAthlete, AthleteRepository athletes) {
        this.currentAthlete = currentAthlete;
        this.athletes = athletes;
    }

    @GetMapping("/me")
    public AthleteResponse me() {
        return AthleteResponse.from(currentAthlete.get());
    }

    @GetMapping("/athletes")
    public List<AthleteResponse> community() {
        return athletes.findAll().stream().map(AthleteResponse::from).toList();
    }
}
