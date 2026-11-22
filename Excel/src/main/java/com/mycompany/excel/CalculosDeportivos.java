package com.mycompany.excel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Parte 3: Indicadores deportivos y estadistica descriptiva
// Parte 4: Regresion lineal y correlacion de Pearson
public class CalculosDeportivos {

    public static double media(List<Double> datos) {
        if (datos == null || datos.isEmpty()) return 0.0;
        double suma = 0;
        for (double d : datos) suma += d;
        return suma / datos.size();
    }

    public static double mediana(List<Double> datos) {
        if (datos == null || datos.isEmpty()) return 0.0;
        List<Double> ordenados = new ArrayList<>(datos);
        Collections.sort(ordenados);
        int n = ordenados.size();
        if (n % 2 == 1) {
            return ordenados.get(n / 2);
        } else {
            return (ordenados.get(n / 2 - 1) + ordenados.get(n / 2)) / 2.0;
        }
    }

    public static double minimo(List<Double> datos) {
        if (datos == null || datos.isEmpty()) return 0.0;
        double min = datos.get(0);
        for (double d : datos) if (d < min) min = d;
        return min;
    }

    public static double maximo(List<Double> datos) {
        if (datos == null || datos.isEmpty()) return 0.0;
        double max = datos.get(0);
        for (double d : datos) if (d > max) max = d;
        return max;
    }

    public static double desviacionTipica(List<Double> datos) {
        if (datos == null || datos.isEmpty()) return 0.0;
        double m = media(datos);
        double suma = 0;
        for (double d : datos) {
            suma += Math.pow(d - m, 2);
        }
        return Math.sqrt(suma / datos.size());
    }

    public static double coeficienteVariacion(List<Double> datos) {
        double m = media(datos);
        if (m == 0.0) return 0.0;
        return (desviacionTipica(datos) / m) * 100.0;
    }

    public static double monotonia(List<Double> cargas7Dias) {
        double dt = desviacionTipica(cargas7Dias);
        if (dt == 0.0) return Double.NaN;
        return media(cargas7Dias) / dt;
    }

    public static double tensionSemanal(double cargaSemanal, double monotonia) {
        return cargaSemanal * monotonia;
    }

    public static double[] regresionLineal(List<Double> x, List<Double> y) {
        int n = x.size();
        if (n < 2) return new double[]{0.0, 0.0, 0.0};

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double xi = x.get(i);
            double yi = y.get(i);
            sumX += xi;
            sumY += yi;
            sumXY += xi * yi;
            sumX2 += xi * xi;
        }

        double denominador = (n * sumX2) - (sumX * sumX);
        if (denominador == 0) return new double[]{media(y), 0.0, 0.0};

        double b = ((n * sumXY) - (sumX * sumY)) / denominador;
        double a = media(y) - (b * media(x));

        double medY = media(y);
        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < n; i++) {
            double yi = y.get(i);
            double yPred = a + (b * x.get(i));
            ssTot += Math.pow(yi - medY, 2);
            ssRes += Math.pow(yi - yPred, 2);
        }
        double r2 = (ssTot == 0) ? 1.0 : (1.0 - (ssRes / ssTot));
        r2 = Math.max(0.0, Math.min(1.0, r2));

        return new double[]{a, b, r2};
    }

    public static double correlacionPearson(List<Double> x, List<Double> y) {
        int n = x.size();
        if (n < 2 || n != y.size()) return 0.0;

        double medX = media(x);
        double medY = media(y);
        double num = 0.0, denX = 0.0, denY = 0.0;

        for (int i = 0; i < n; i++) {
            double diffX = x.get(i) - medX;
            double diffY = y.get(i) - medY;
            num += diffX * diffY;
            denX += diffX * diffX;
            denY += diffY * diffY;
        }

        double den = Math.sqrt(denX * denY);
        if (den == 0.0) return 0.0;
        return num / den;
    }
}
