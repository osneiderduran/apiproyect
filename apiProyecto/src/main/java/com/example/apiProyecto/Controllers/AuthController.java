package com.example.apiProyecto.Controllers;

import com.example.apiProyecto.Models.UserModel;
import com.example.apiProyecto.Services.SocialAuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final SocialAuthService social;

    public AuthController(SocialAuthService social) {
        this.social = social;
    }

    /** Datos públicos que el navegador necesita para mostrar los botones. */
    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of("google", social.getGoogleClientId(), "facebook", social.getFacebookAppId());
    }

    @PostMapping("/google")
    public UserModel google(@RequestBody Map<String, String> body) {
        return social.conGoogle(body.get("token"));
    }

    @PostMapping("/facebook")
    public UserModel facebook(@RequestBody Map<String, String> body) {
        return social.conFacebook(body.get("token"));
    }
}
