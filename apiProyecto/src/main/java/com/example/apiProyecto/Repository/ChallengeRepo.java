package com.example.apiProyecto.Repository;

import com.example.apiProyecto.Models.ChallengeModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeRepo extends MongoRepository<ChallengeModel, String> {

    List<ChallengeModel> findByCategoria(String categoria);
}
