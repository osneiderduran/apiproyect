package com.example.apiProyecto.Models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
public class UserModel {

    @Id
    private String id;

    private String nombre;

    @Indexed(unique = true)
    private String email;

    // WRITE_ONLY: se acepta al recibir el JSON, pero nunca se devuelve
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private int xp = 0;
    private int nivel = 1;
    private List<Completion> historial = new ArrayList<>();

    public UserModel() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }

    public List<Completion> getHistorial() { return historial; }
    public void setHistorial(List<Completion> historial) { this.historial = historial; }

    // Un reto completado (se guarda dentro del usuario)
    public static class Completion {
        private String retoId;
        private String titulo;
        private String categoria;
        private int xp;
        private String fecha; // yyyy-MM-dd

        public Completion() { }

        public String getRetoId() { return retoId; }
        public void setRetoId(String retoId) { this.retoId = retoId; }
        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public int getXp() { return xp; }
        public void setXp(int xp) { this.xp = xp; }
        public String getFecha() { return fecha; }
        public void setFecha(String fecha) { this.fecha = fecha; }
    }
}
