package com.example.apiProyecto.Models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "retos")
public class ChallengeModel {

    @Id
    private String id;
    private String titulo;
    private String categoria;
    private String descripcion;
    private String dificultad;
    private int xp;

    public ChallengeModel() {
    }

    public ChallengeModel(String titulo, String categoria, String descripcion, String dificultad, int xp) {
        this.titulo = titulo;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.dificultad = dificultad;
        this.xp = xp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getDificultad() { return dificultad; }
    public void setDificultad(String dificultad) { this.dificultad = dificultad; }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
}
