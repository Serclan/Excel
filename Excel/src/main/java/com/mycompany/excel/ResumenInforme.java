package com.mycompany.excel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Datos ya filtrados y agregados para generar un informe deportivo. */
public final class ResumenInforme {

    public static final class ResumenSemanal {
        private final LocalDate inicio;
        private double kilometros;
        private int minutos;
        private double carga;
        private int sesiones;
        private Double mediaMovilTresSemanas;

        private ResumenSemanal(LocalDate inicio) {
            this.inicio = inicio;
        }

        private void agregar(Entrenamiento entrenamiento) {
            kilometros += entrenamiento.getDistanciaKm();
            minutos += entrenamiento.getDuracionMin();
            carga += entrenamiento.getCargaUA();
            sesiones++;
        }

        public LocalDate getInicio() { return inicio; }
        public double getKilometros() { return kilometros; }
        public int getMinutos() { return minutos; }
        public double getCarga() { return carga; }
        public int getSesiones() { return sesiones; }
        public Double getMediaMovilTresSemanas() { return mediaMovilTresSemanas; }
    }

    private final Deportista deportista;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final List<Entrenamiento> entrenamientos;
    private final List<PruebaRendimiento> pruebas;
    private final List<ResumenSemanal> semanas;
    private final double kilometrosTotales;
    private final int minutosTotales;
    private final double cargaTotal;
    private final double mejorMarcaSegundos;
    private final double[] regresion;

    private ResumenInforme(Deportista deportista, LocalDate desde, LocalDate hasta,
            List<Entrenamiento> entrenamientos, List<PruebaRendimiento> pruebas,
            List<ResumenSemanal> semanas, double kilometrosTotales, int minutosTotales,
            double cargaTotal, double mejorMarcaSegundos, double[] regresion) {
        this.deportista = deportista;
        this.desde = desde;
        this.hasta = hasta;
        this.entrenamientos = List.copyOf(entrenamientos);
        this.pruebas = List.copyOf(pruebas);
        this.semanas = List.copyOf(semanas);
        this.kilometrosTotales = kilometrosTotales;
        this.minutosTotales = minutosTotales;
        this.cargaTotal = cargaTotal;
        this.mejorMarcaSegundos = mejorMarcaSegundos;
        this.regresion = regresion.clone();
    }

    public static ResumenInforme crear(Deportista deportista, LocalDate desde, LocalDate hasta,
            List<Entrenamiento> todosLosEntrenamientos, List<PruebaRendimiento> todasLasPruebas) {
        if (deportista == null || desde == null || hasta == null) {
            throw new IllegalArgumentException("Selecciona el deportista y las dos fechas.");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        if (todosLosEntrenamientos == null || todasLasPruebas == null) {
            throw new IllegalArgumentException("No hay datos disponibles para crear el informe.");
        }

        List<Entrenamiento> sesiones = new ArrayList<>();
        for (Entrenamiento entrenamiento : todosLosEntrenamientos) {
            if (entrenamiento.getIdDeportista().equalsIgnoreCase(deportista.getId())
                    && !entrenamiento.getFecha().isBefore(desde)
                    && !entrenamiento.getFecha().isAfter(hasta)) {
                sesiones.add(entrenamiento);
            }
        }
        sesiones.sort((a, b) -> a.getFecha().compareTo(b.getFecha()));

        List<PruebaRendimiento> tests = new ArrayList<>();
        for (PruebaRendimiento prueba : todasLasPruebas) {
            if (prueba.getIdDeportista().equalsIgnoreCase(deportista.getId())
                    && !prueba.getFecha().isBefore(desde)
                    && !prueba.getFecha().isAfter(hasta)) {
                tests.add(prueba);
            }
        }
        tests.sort((a, b) -> a.getFecha().compareTo(b.getFecha()));

        if (sesiones.isEmpty() && tests.isEmpty()) {
            throw new IllegalArgumentException("No hay entrenamientos ni pruebas de 5 km de este deportista en ese periodo.");
        }

        double kilometros = 0.0;
        int minutos = 0;
        double carga = 0.0;
        TreeMap<LocalDate, ResumenSemanal> porSemana = new TreeMap<>();
        LocalDate primeraSemana = inicioSemana(desde);
        LocalDate ultimaSemana = inicioSemana(hasta);
        for (LocalDate semana = primeraSemana; !semana.isAfter(ultimaSemana); semana = semana.plusWeeks(1)) {
            porSemana.put(semana, new ResumenSemanal(semana));
        }
        for (Entrenamiento entrenamiento : sesiones) {
            kilometros += entrenamiento.getDistanciaKm();
            minutos += entrenamiento.getDuracionMin();
            carga += entrenamiento.getCargaUA();
            porSemana.get(inicioSemana(entrenamiento.getFecha())).agregar(entrenamiento);
        }

        List<ResumenSemanal> semanas = new ArrayList<>(porSemana.values());
        for (int i = 2; i < semanas.size(); i++) {
            double sumaTresSemanas = semanas.get(i).getCarga()
                    + semanas.get(i - 1).getCarga() + semanas.get(i - 2).getCarga();
            semanas.get(i).mediaMovilTresSemanas = sumaTresSemanas / 3.0;
        }

        double mejorMarca = Double.NaN;
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        if (!tests.isEmpty()) {
            mejorMarca = tests.get(0).getMarcaSegundos();
            LocalDate fechaInicial = tests.get(0).getFecha();
            for (PruebaRendimiento prueba : tests) {
                mejorMarca = Math.min(mejorMarca, prueba.getMarcaSegundos());
                x.add(ChronoUnit.DAYS.between(fechaInicial, prueba.getFecha()) / 7.0);
                y.add(prueba.getMarcaSegundos());
            }
        }

        double[] regresion = tests.size() >= 2
                ? CalculosDeportivos.regresionLineal(x, y)
                : new double[]{0.0, 0.0, 0.0};

        return new ResumenInforme(deportista, desde, hasta, sesiones, tests, semanas,
                kilometros, minutos, carga, mejorMarca, regresion);
    }

    private static LocalDate inicioSemana(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public Deportista getDeportista() { return deportista; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public List<Entrenamiento> getEntrenamientos() { return Collections.unmodifiableList(entrenamientos); }
    public List<PruebaRendimiento> getPruebas() { return Collections.unmodifiableList(pruebas); }
    public List<ResumenSemanal> getSemanas() { return Collections.unmodifiableList(semanas); }
    public double getKilometrosTotales() { return kilometrosTotales; }
    public int getMinutosTotales() { return minutosTotales; }
    public double getCargaTotal() { return cargaTotal; }
    public double getMejorMarcaSegundos() { return mejorMarcaSegundos; }
    public double[] getRegresion() { return regresion.clone(); }
}
