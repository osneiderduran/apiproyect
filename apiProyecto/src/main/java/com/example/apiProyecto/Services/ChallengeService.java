package com.example.apiProyecto.Services;

import com.example.apiProyecto.Models.ChallengeModel;
import com.example.apiProyecto.Repository.ChallengeRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChallengeService {

    private final ChallengeRepo challengeRepo;

    public ChallengeService(ChallengeRepo challengeRepo) {
        this.challengeRepo = challengeRepo;
    }

    public List<ChallengeModel> listar(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return challengeRepo.findAll();
        }
        return challengeRepo.findByCategoria(categoria);
    }

    public ChallengeModel guardar(ChallengeModel reto) {
        reto.setId(null);
        return challengeRepo.save(reto);
    }
}
