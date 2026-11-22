package com.mycompany.excel;

import java.time.LocalDate;

// Parte 1: Modelo de datos
public class PruebaRendimiento {
    private String id;
    private String idDeportista;
    private LocalDate fecha;
    private double marcaSegundos;
    private int fcMedia;

    public PruebaRendimiento(String id, String idDeportista, LocalDate fecha, double marcaSegundos, int fcMedia) {
        this.id = id;
        this.idDeportista = idDeportista;
        this.fecha = fecha;
        this.marcaSegundos = marcaSegundos;
        this.fcMedia = fcMedia;
    }

    public String getId() { return id; }
    public String getIdDeportista() { return idDeportista; }
    public LocalDate getFecha() { return fecha; }
    public double getMarcaSegundos() { return marcaSegundos; }
    public int getFcMedia() { return fcMedia; }
}
