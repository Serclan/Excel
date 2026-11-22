package com.mycompany.excel;

import java.time.LocalDate;

// Parte 1: Modelo de datos
public class Entrenamiento {
    private String id;
    private String idDeportista;
    private LocalDate fecha;
    private String tipoSesion;
    private int duracionMin;
    private double distanciaKm;
    private int fcMedia;
    private int rpe;

    public Entrenamiento(String id, String idDeportista, LocalDate fecha, String tipoSesion, int duracionMin, double distanciaKm, int fcMedia, int rpe) {
        this.id = idSupplier(id);
        this.idDeportista = idDeportista;
        this.fecha = fecha;
        this.tipoSesion = tipoSesion;
        this.duracionMin = duracionMin;
        this.distanciaKm = distanciaKm;
        this.fcMedia = fcMedia;
        this.rpe = rpe;
    }

    private String idSupplier(String id) { return id; }

    public double getCargaUA() {
        return duracionMin * rpe;
    }

    public String getId() { return id; }
    public String getIdDeportista() { return idDeportista; }
    public LocalDate getFecha() { return fecha; }
    public String getTipoSesion() { return tipoSesion; }
    public int getDuracionMin() { return duracionMin; }
    public double getDistanciaKm() { return distanciaKm; }
    public int getFcMedia() { return fcMedia; }
    public int getRpe() { return rpe; }
}
