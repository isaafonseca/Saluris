package br.com.saluris.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público de status e verificação de saúde da aplicação para monitoramento e checagem do Render.
 */
@RestController
@RequestMapping("/api")
public class StatusController {

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "status", "UP",
            "app", "Saluris IA - Gestão de SST",
            "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
