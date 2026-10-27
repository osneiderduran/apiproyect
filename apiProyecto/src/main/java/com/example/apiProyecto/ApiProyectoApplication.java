package com.example.apiProyecto;

import com.example.apiProyecto.Models.ChallengeModel;
import com.example.apiProyecto.Repository.ChallengeRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class ApiProyectoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiProyectoApplication.class, args);
    }

    // Si la colección "retos" está vacía, la llena con retos de ejemplo
    @Bean
    CommandLineRunner cargarRetos(ChallengeRepo repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.saveAll(List.of(
                    new ChallengeModel("20 sentadillas", "Fisicos", "Mantén la espalda recta y baja de forma controlada. Detente si sientes dolor.", "Fácil", 10),
                    new ChallengeModel("Camina 10 minutos", "Fisicos", "Camina a un ritmo cómodo y presta atención a tu entorno.", "Fácil", 10),
                    new ChallengeModel("Estira 5 minutos", "Fisicos", "Realiza estiramientos suaves sin forzar las articulaciones.", "Fácil", 10),
                    new ChallengeModel("Haz una pausa activa", "Fisicos", "Levántate, mueve los hombros y camina un poco.", "Fácil", 8),
                    new ChallengeModel("¿Qué número sigue? 2, 4, 8, 16…", "Logica", "Elige el siguiente número: 24, 28, 32 o 20. Cada término se duplica.", "Fácil", 15),
                    new ChallengeModel("Resuelve un acertijo", "Logica", "Piensa en una solución distinta y explica tu razonamiento.", "Medio", 20),
                    new ChallengeModel("Entrena tu memoria", "Logica", "Observa cinco objetos, cúbrelos e intenta recordarlos.", "Fácil", 15),
                    new ChallengeModel("Completa una secuencia", "Logica", "Encuentra el patrón de una serie de números.", "Medio", 20),
                    new ChallengeModel("Dibuja en un minuto", "Creativos", "Dibuja un animal sin borrar y dale un nombre.", "Fácil", 12),
                    new ChallengeModel("Inventa una historia", "Creativos", "Escribe una historia breve que incluya una llave y una montaña.", "Medio", 18),
                    new ChallengeModel("Toma una foto temática", "Creativos", "Fotografía algo que represente la palabra alegría.", "Fácil", 12),
                    new ChallengeModel("Aprende cinco palabras", "Aprendizaje", "Busca cinco palabras nuevas y escribe una frase con cada una.", "Fácil", 15),
                    new ChallengeModel("Lee durante 10 minutos", "Aprendizaje", "Lee un libro o artículo y resume una idea.", "Fácil", 12),
                    new ChallengeModel("Aprende un dato nuevo", "Aprendizaje", "Investiga un dato de ciencia, historia o tecnología.", "Fácil", 10),
                    new ChallengeModel("Ordena tu espacio", "Habitos", "Dedica cinco minutos a organizar tu escritorio.", "Fácil", 8)
                ));
            }
        };
    }
}
