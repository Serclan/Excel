package com.mycompany.excel;

// Parte 1: Modelo de datos
public class Deportista {
    private String id;
    private String nombre;
    private int edad;
    private String categoria;
    private String objetivo;
    private double marcaInicial5k;

    public Deportista(String id, String nombre, int edad, String categoria, String objetivo, double marcaInicial5k) {
        this.id = id;
        this.nombre = nombre;
        this.edad = edad;
        this.categoria = categoria;
        this.objetivo = objetivo;
        this.marcaInicial5k = marcaInicial5k;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public double getMarcaInicial5k() { return marcaInicial5k; }
    public void setMarcaInicial5k(double marcaInicial5k) { this.marcaInicial5k = marcaInicial5k; }

    @Override
    public String toString() {
        return nombre + " (" + id + ")";
    }
}
