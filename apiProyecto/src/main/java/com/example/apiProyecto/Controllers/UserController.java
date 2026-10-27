package com.example.apiProyecto.Controllers;

import com.example.apiProyecto.Models.UserModel;
import com.example.apiProyecto.Services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") // En producción, limita al dominio de tu app
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserModel> registrar(@RequestBody UserModel user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.guardarUsuario(user));
    }

    @PostMapping("/login")
    public UserModel login(@RequestBody Map<String, String> body) {
        return userService.login(body.get("email"), body.get("password"));
    }

    @GetMapping("/ranking")
    public List<Map<String, Object>> ranking() {
        return userService.ranking();
    }

    @GetMapping("/{id}")
    public UserModel obtener(@PathVariable String id) {
        return userService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public UserModel actualizar(@PathVariable String id, @RequestBody UserModel datos) {
        return userService.actualizarUsuario(id, datos);
    }

    @PostMapping("/{id}/completar/{retoId}")
    public UserModel completarReto(@PathVariable String id, @PathVariable String retoId) {
        return userService.completarReto(id, retoId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        userService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
