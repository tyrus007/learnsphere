package com.ashique.identity_service.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/actuator/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "identity-service",
                "timestamp", Instant.now().toString()
        );
    }
}

