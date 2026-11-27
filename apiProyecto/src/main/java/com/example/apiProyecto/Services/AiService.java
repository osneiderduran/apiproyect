package com.example.apiProyecto.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Genera contenido nuevo para los juegos (acertijos, lógica, "encuentra el diferente"
 * y anagramas) con la API gratuita de Groq (https://console.groq.com).
 *
 * - La clave se lee de la variable de entorno GROQ_API_KEY (nunca va al navegador).
 * - Cada tipo se guarda en caché unos minutos para no gastar la cuota gratuita.
 * - Si no hay clave o la IA falla, devuelve una lista vacía y la app usa sus preguntas locales.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";
    public static final Set<String> TIPOS = Set.of("acertijos", "logica", "diferente", "anagramas");

    private static final String[] TEMAS = {
            "animales", "el cuerpo humano", "el espacio", "la cocina", "la casa", "la naturaleza",
            "los deportes", "la música", "la escuela", "la tecnología", "el clima", "los oficios",
            "la geografía de Colombia", "los números", "los colores", "los medios de transporte",
            "la historia", "las plantas", "el océano", "los inventos"
    };

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final Map<String, Cache> cache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${groq.cache.minutos:15}")
    private long cacheMinutos;

    private record Cache(long hasta, List<JsonNode> items) {}

    public boolean activa() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<JsonNode> contenido(String tipo) {
        if (!TIPOS.contains(tipo) || !activa()) return List.of();
        Cache c = cache.get(tipo);
        if (c != null && c.hasta() > System.currentTimeMillis()) return c.items();
        synchronized (this) {
            c = cache.get(tipo);
            if (c != null && c.hasta() > System.currentTimeMillis()) return c.items();
            List<JsonNode> nuevos = generar(tipo);
            // Si falla, se reintenta en 2 minutos; si funciona, se guarda el tiempo configurado.
            long ttl = nuevos.isEmpty() ? Duration.ofMinutes(2).toMillis() : Duration.ofMinutes(cacheMinutos).toMillis();
            List<JsonNode> guardar = nuevos.isEmpty() && c != null ? c.items() : nuevos;
            cache.put(tipo, new Cache(System.currentTimeMillis() + ttl, guardar));
            return guardar;
        }
    }

    private List<JsonNode> generar(String tipo) {
        String tema1 = TEMAS[random.nextInt(TEMAS.length)], tema2 = TEMAS[random.nextInt(TEMAS.length)];
        String prompt = instrucciones(tipo) +
                "\nTemas sugeridos para esta tanda: " + tema1 + " y " + tema2 + " (puedes mezclar otros)." +
                "\nTodo en español neutro, apto para todo público, sin repetir ideas clásicas muy conocidas." +
                "\nResponde SOLO con un objeto JSON de la forma {\"items\":[...]}.";
        try {
            ObjectNode body = json.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.95);
            body.put("max_tokens", 2500);
            body.putObject("response_format").put("type", "json_object");
            ArrayNode msgs = body.putArray("messages");
            msgs.addObject().put("role", "system").put("content",
                    "Eres un creador de juegos de ingenio para una app educativa. Respondes únicamente con JSON válido.");
            msgs.addObject().put("role", "user").put("content", prompt);

            HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                    .timeout(Duration.ofSeconds(40))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("Groq respondió {} para '{}': {}", res.statusCode(), tipo, recortar(res.body()));
                return List.of();
            }
            String texto = json.readTree(res.body()).path("choices").path(0).path("message").path("content").asText("");
            JsonNode items = json.readTree(texto).path("items");
            List<JsonNode> ok = new ArrayList<>();
            if (items.isArray()) for (JsonNode it : items) {
                JsonNode limpio = validar(tipo, it);
                if (limpio != null) ok.add(limpio);
            }
            log.info("IA generó {} elementos válidos de '{}'", ok.size(), tipo);
            return ok;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            log.warn("No se pudo generar '{}' con IA: {}", tipo, e.getMessage());
            return List.of();
        }
    }

    private String instrucciones(String tipo) {
        return switch (tipo) {
            case "acertijos" -> "Crea 10 acertijos ORIGINALES y cortos (adivinanzas). Formato de cada elemento: " +
                    "{\"q\":\"pregunta\",\"a\":\"respuesta corta (1 a 4 palabras)\",\"o\":[\"4 opciones, una es exactamente a\"],\"hint\":\"pista breve\"}";
            case "logica" -> "Crea 10 preguntas de razonamiento lógico (deducción, orden, días de la semana, cálculo mental sencillo). " +
                    "Cada una con una única respuesta correcta sin ambigüedad. Formato: " +
                    "{\"q\":\"pregunta\",\"o\":[\"3 o 4 opciones cortas\"],\"a\":\"la opción correcta, idéntica a una de o\",\"hint\":\"pista breve\"}";
            case "diferente" -> "Crea 10 retos de 'encuentra el elemento que no pertenece al grupo'. Cuatro elementos, solo uno rompe el patrón de forma clara. Formato: " +
                    "{\"q\":\"pregunta corta, p. ej. ¿Cuál no es una fruta?\",\"o\":[\"4 elementos\"],\"a\":\"el diferente, idéntico a uno de o\"}";
            case "anagramas" -> "Crea 12 palabras en español de 5 a 10 letras (sin espacios) con su pista. Formato: " +
                    "{\"w\":\"PALABRA en mayúsculas\",\"h\":\"pista corta de 2 a 6 palabras\"}";
            default -> "";
        };
    }

    /** Revisa que el elemento esté completo y bien formado; si no, se descarta. */
    private JsonNode validar(String tipo, JsonNode it) {
        if (!it.isObject()) return null;
        ObjectNode out = json.createObjectNode();
        if (tipo.equals("anagramas")) {
            String w = sinTildes(it.path("w").asText("")).toUpperCase(Locale.ROOT).replaceAll("[^A-ZÑ]", "");
            String h = it.path("h").asText("").trim();
            if (w.length() < 4 || w.length() > 12 || h.isEmpty() || h.length() > 60) return null;
            return out.put("w", w).put("h", h);
        }
        String q = it.path("q").asText("").trim(), a = it.path("a").asText("").trim();
        if (q.length() < 8 || q.length() > 220 || a.isEmpty() || a.length() > 40) return null;
        List<String> ops = new ArrayList<>();
        for (JsonNode o : it.path("o")) {
            String s = o.asText("").trim();
            if (!s.isEmpty() && s.length() <= 40 && ops.stream().noneMatch(s::equalsIgnoreCase)) ops.add(s);
        }
        if (ops.stream().noneMatch(a::equals)) return null;      // la respuesta tiene que estar en las opciones
        if (ops.size() < 3 || ops.size() > 4) return null;
        out.put("q", q).put("a", a);
        ArrayNode arr = out.putArray("o");
        ops.forEach(arr::add);
        String hint = it.path("hint").asText("").trim();
        if (!hint.isEmpty() && hint.length() <= 120) out.put("hint", hint);
        if (tipo.equals("diferente")) out.put("eq", String.join(" · ", ops));
        return out;
    }

    private static String sinTildes(String s) {
        // Conserva la Ñ y quita tildes y diéresis.
        String tmp = s.replace("ñ", "\u0001").replace("Ñ", "\u0002");
        tmp = Normalizer.normalize(tmp, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return tmp.replace("\u0001", "ñ").replace("\u0002", "Ñ");
    }

    private static String recortar(String s) {
        return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
