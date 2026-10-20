package com.ruky.training.web;

import com.ruky.training.service.EliteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elite")
public class EliteController {

    private final EliteService service;

    public EliteController(EliteService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public EliteStatusResponse status() {
        return service.status();
    }

    @PostMapping("/apply")
    public EliteStatusResponse apply() {
        return service.apply();
    }
}
