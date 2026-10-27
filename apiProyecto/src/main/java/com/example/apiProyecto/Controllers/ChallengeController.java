package com.example.apiProyecto.Controllers;

import com.example.apiProyecto.Models.ChallengeModel;
import com.example.apiProyecto.Services.ChallengeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retos")
@CrossOrigin(origins = "*")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping
    public List<ChallengeModel> listar(@RequestParam(required = false) String categoria) {
        return challengeService.listar(categoria);
    }

    @PostMapping
    public ResponseEntity<ChallengeModel> crear(@RequestBody ChallengeModel reto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(challengeService.guardar(reto));
    }
}
