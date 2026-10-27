package com.example.apiProyecto.Services;

import com.example.apiProyecto.Models.UserModel;
import com.example.apiProyecto.Repository.UserRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Acceso con Google y Facebook.
 * El navegador obtiene un token del proveedor; aquí se VERIFICA con el proveedor
 * (nunca se confía en lo que diga el navegador) y se busca o crea el usuario.
 *
 * Variables de entorno necesarias:
 *   GOOGLE_CLIENT_ID      (ID de cliente OAuth tipo "Aplicación web")
 *   FACEBOOK_APP_ID y FACEBOOK_APP_SECRET
 */
@Service
public class SocialAuthService {

    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;
    @Value("${FACEBOOK_APP_ID:}")
    private String facebookAppId;
    @Value("${FACEBOOK_APP_SECRET:}")
    private String facebookAppSecret;

    private final UserRepo userRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public SocialAuthService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public String getGoogleClientId() { return googleClientId == null ? "" : googleClientId.trim(); }
    public String getFacebookAppId() { return facebookAppId == null ? "" : facebookAppId.trim(); }
    private boolean facebookListo() { return !getFacebookAppId().isEmpty() && facebookAppSecret != null && !facebookAppSecret.isBlank(); }

    public UserModel conGoogle(String accessToken) {
        if (getGoogleClientId().isEmpty()) throw error(HttpStatus.SERVICE_UNAVAILABLE, "El acceso con Google aún no está configurado en el servidor.");
        if (accessToken == null || accessToken.isBlank()) throw error(HttpStatus.BAD_REQUEST, "Falta el token de Google.");
        JsonNode info = get("https://oauth2.googleapis.com/tokeninfo?access_token=" + enc(accessToken), null);
        String aud = info.path("aud").asText("");
        String azp = info.path("azp").asText("");
        if (!getGoogleClientId().equals(aud) && !getGoogleClientId().equals(azp)) {
            throw error(HttpStatus.UNAUTHORIZED, "El token de Google no pertenece a esta aplicación.");
        }
        String email = info.path("email").asText("");
        if (email.isEmpty() || !"true".equals(info.path("email_verified").asText(""))) {
            throw error(HttpStatus.UNAUTHORIZED, "Tu cuenta de Google no tiene un correo verificado.");
        }
        String nombre = "";
        try {
            JsonNode perfil = get("https://www.googleapis.com/oauth2/v3/userinfo", accessToken);
            nombre = perfil.path("name").asText("");
        } catch (RuntimeException ignorado) { /* el nombre es opcional */ }
        return buscarOCrear(email, nombre);
    }

    public UserModel conFacebook(String accessToken) {
        if (!facebookListo()) throw error(HttpStatus.SERVICE_UNAVAILABLE, "El acceso con Facebook aún no está configurado en el servidor.");
        if (accessToken == null || accessToken.isBlank()) throw error(HttpStatus.BAD_REQUEST, "Falta el token de Facebook.");
        String appToken = getFacebookAppId() + "|" + facebookAppSecret.trim();
        JsonNode dbg = get("https://graph.facebook.com/debug_token?input_token=" + enc(accessToken) + "&access_token=" + enc(appToken), null).path("data");
        if (!dbg.path("is_valid").asBoolean(false) || !getFacebookAppId().equals(dbg.path("app_id").asText(""))) {
            throw error(HttpStatus.UNAUTHORIZED, "El token de Facebook no es válido para esta aplicación.");
        }
        JsonNode me = get("https://graph.facebook.com/me?fields=id,name,email&access_token=" + enc(accessToken), null);
        String id = me.path("id").asText("");
        if (id.isEmpty()) throw error(HttpStatus.UNAUTHORIZED, "No se pudo leer tu perfil de Facebook.");
        String email = me.path("email").asText("");
        if (email.isEmpty()) email = "fb_" + id + "@facebook.local"; // Facebook no siempre entrega el correo
        return buscarOCrear(email, me.path("name").asText(""));
    }

    private UserModel buscarOCrear(String email, String nombre) {
        String correo = email.trim().toLowerCase();
        return userRepo.findByEmail(correo).or(() -> userRepo.findByEmail(email)).orElseGet(() -> {
            UserModel u = new UserModel();
            u.setEmail(correo);
            u.setNombre(nombre == null || nombre.isBlank() ? correo.split("@")[0] : nombre);
            u.setPassword(encoder.encode(UUID.randomUUID().toString())); // contraseña aleatoria: solo entra con el proveedor
            u.setXp(0);
            u.setNivel(1);
            return userRepo.save(u);
        });
    }

    private JsonNode get(String url, String bearer) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET();
            if (bearer != null) b.header("Authorization", "Bearer " + bearer);
            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() / 100 != 2) throw error(HttpStatus.UNAUTHORIZED, "El proveedor rechazó el token.");
            return mapper.readTree(r.body());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw error(HttpStatus.BAD_GATEWAY, "No se pudo verificar el acceso con el proveedor.");
        }
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static ResponseStatusException error(HttpStatus st, String msg) { return new ResponseStatusException(st, msg); }
}
