package com.example.apiProyecto.Services;

import com.example.apiProyecto.Models.ChallengeModel;
import com.example.apiProyecto.Models.UserModel;
import com.example.apiProyecto.Repository.ChallengeRepo;
import com.example.apiProyecto.Repository.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final UserRepo userRep;
    private final ChallengeRepo challengeRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Inyección por constructor (evita que userRep quede en null)
    public UserService(UserRepo userRep, ChallengeRepo challengeRepo) {
        this.userRep = userRep;
        this.challengeRepo = challengeRepo;
    }

    public UserModel guardarUsuario(UserModel user) {
        if (user.getEmail() == null || user.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email y contraseña son obligatorios");
        }
        if (userRep.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }
        user.setId(null);
        user.setXp(0);
        user.setNivel(1);
        user.setPassword(encoder.encode(user.getPassword()));
        return userRep.save(user);
    }

    public UserModel login(String email, String password) {
        UserModel user = userRep.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        if (!encoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        return user;
    }

    // Ranking público: solo expone nombre, nivel y XP; nunca email ni otros datos privados.
    public List<Map<String, Object>> ranking() {
        return userRep.findAll().stream()
                .sorted(Comparator.comparingInt(UserModel::getXp).reversed())
                .limit(50)
                .map(u -> Map.<String, Object>of(
                        "nombre", u.getNombre() == null ? "Explorador" : u.getNombre(),
                        "nivel", u.getNivel(),
                        "xp", u.getXp()))
                .collect(Collectors.toList());
    }

    public UserModel obtenerPorId(String id) {
        return userRep.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public UserModel actualizarUsuario(String id, UserModel datos) {
        UserModel user = obtenerPorId(id);
        if (datos.getNombre() != null) user.setNombre(datos.getNombre());
        if (datos.getEmail() != null) user.setEmail(datos.getEmail());
        if (datos.getPassword() != null) user.setPassword(encoder.encode(datos.getPassword()));
        return userRep.save(user);
    }

    public UserModel sumarXp(String id, int cantidad) {
        UserModel user = obtenerPorId(id);
        int xpTotal = user.getXp() + cantidad;
        user.setXp(xpTotal);
        user.setNivel(1 + xpTotal / 100); // sube de nivel cada 100 XP
        return userRep.save(user);
    }

    // Completa un reto: suma XP, sube de nivel y lo anota en el historial (1 vez por día)
    public UserModel completarReto(String id, String retoId) {
        UserModel user = obtenerPorId(id);
        ChallengeModel reto = challengeRepo.findById(retoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reto no encontrado"));
        String hoy = LocalDate.now(ZONA).toString();

        if (user.getHistorial() == null) {
            user.setHistorial(new ArrayList<>());
        }
        for (UserModel.Completion c : user.getHistorial()) {
            if (retoId.equals(c.getRetoId()) && hoy.equals(c.getFecha())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya completaste este reto hoy");
            }
        }

        UserModel.Completion nuevo = new UserModel.Completion();
        nuevo.setRetoId(reto.getId());
        nuevo.setTitulo(reto.getTitulo());
        nuevo.setCategoria(reto.getCategoria());
        nuevo.setXp(reto.getXp());
        nuevo.setFecha(hoy);
        user.getHistorial().add(nuevo);

        int xpTotal = user.getXp() + reto.getXp();
        user.setXp(xpTotal);
        user.setNivel(1 + xpTotal / 100);
        return userRep.save(user);
    }

    public void eliminarUsuario(String id) {
        obtenerPorId(id);
        userRep.deleteById(id);
    }
}
