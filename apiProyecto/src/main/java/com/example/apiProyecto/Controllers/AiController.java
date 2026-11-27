package com.example.apiProyecto.Controllers;

import com.example.apiProyecto.Services.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GET /api/ia/estado            -> {"activa": true|false}
 * GET /api/ia/juegos/{tipo}     -> lista de preguntas nuevas generadas por IA
 *     tipo = acertijos | logica | diferente | anagramas
 */
@RestController
@RequestMapping("/api/ia")
@CrossOrigin(origins = "*")
public class AiController {

    private final AiService ai;

    public AiController(AiService ai) {
        this.ai = ai;
    }

    @GetMapping("/estado")
    public Map<String, Boolean> estado() {
        return Map.of("activa", ai.activa());
    }

    @GetMapping("/juegos/{tipo}")
    public ResponseEntity<List<JsonNode>> juegos(@PathVariable String tipo) {
        if (!AiService.TIPOS.contains(tipo)) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(ai.contenido(tipo));
    }
}
