package com.mycompany.excel;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Genera el informe deportivo en PDF a partir de un resumen ya calculado. */
public final class GeneradorPDF {

    private static final float ANCHO = PDRectangle.A4.getWidth();
    private static final float ALTO = PDRectangle.A4.getHeight();
    private static final float MARGEN = 46;
    private static final float ANCHO_CONTENIDO = ANCHO - 2 * MARGEN;
    private static final ColorPaleta COLOR = new ColorPaleta();
    private static final Locale ESPANOL = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", ESPANOL);
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ESPANOL);

    private GeneradorPDF() { }

    public static void generar(ResumenInforme informe, File destino) throws IOException {
        if (informe == null || destino == null) {
            throw new IllegalArgumentException("Faltan los datos del informe o el archivo de destino.");
        }
        File fuenteNormal = new File("src/main/resources/fonts/DejaVuSans.ttf");
        File fuenteNegrita = new File("src/main/resources/fonts/DejaVuSans-Bold.ttf");
        if (!fuenteNormal.isFile() || !fuenteNegrita.isFile()) {
            throw new IOException("No se encuentran las fuentes TTF incluidas en src/main/resources/fonts.");
        }
        File carpetaDestino = destino.getAbsoluteFile().getParentFile();
        if (carpetaDestino != null && !carpetaDestino.isDirectory() && !carpetaDestino.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de destino.");
        }

        try (PDDocument documento = new PDDocument()) {
            PDFont normal = PDType0Font.load(documento, fuenteNormal);
            PDFont negrita = PDType0Font.load(documento, fuenteNegrita);
            byte[] ilustracion = crearIlustracionAtletismo();
            byte[] graficoCarga = crearGraficoCarga(informe);
            byte[] graficoMarcas = crearGraficoMarcas(informe);

            crearPortada(documento, informe, normal, negrita, ilustracion);
            crearPaginaEntrenamientos(documento, informe, normal, negrita, graficoCarga);
            crearPaginaRendimiento(documento, informe, normal, negrita, graficoMarcas);
            crearPaginaInterpretacion(documento, informe, normal, negrita);

            documento.save(destino);
        }
    }

    private static void crearPortada(PDDocument documento, ResumenInforme informe,
            PDFont normal, PDFont negrita, byte[] ilustracion) throws IOException {
        PDPage pagina = nuevaPagina(documento);
        try (PDPageContentStream c = new PDPageContentStream(documento, pagina)) {
            dibujarCabeceraPie(c, normal, negrita, 1);
            dibujarLogo(c, normal, negrita);
            texto(c, negrita, 26, MARGEN, 690, "Informe de rendimiento", COLOR.TINTA);
            texto(c, normal, 12, MARGEN, 665, "Centro de Rendimiento Deportivo", COLOR.SECUNDARIO);

            texto(c, negrita, 19, MARGEN, 615, informe.getDeportista().getNombre(), COLOR.AZUL);
            texto(c, normal, 11, MARGEN, 592,
                    "Categoría: " + seguro(informe.getDeportista().getCategoria()), COLOR.TINTA);
            texto(c, normal, 11, MARGEN, 573,
                    "Objetivo: " + seguro(informe.getDeportista().getObjetivo()), COLOR.TINTA);
            texto(c, normal, 11, MARGEN, 554,
                    "Periodo: " + fecha(informe.getDesde()) + " – " + fecha(informe.getHasta()), COLOR.TINTA);
            texto(c, normal, 9, MARGEN, 536,
                    "Generado el " + LocalDate.now().format(FECHA_LARGA), COLOR.SECUNDARIO);

            PDImageXObject imagen = PDImageXObject.createFromByteArray(documento, ilustracion, "atletismo.png");
            c.drawImage(imagen, 350, 518, 190, 130);

            seccion(c, normal, negrita, "Resumen del periodo", MARGEN, 472);
            float cardW = (ANCHO_CONTENIDO - 18) / 2;
            tarjeta(c, normal, negrita, MARGEN, 410, cardW, 48, "Sesiones", Integer.toString(informe.getEntrenamientos().size()));
            tarjeta(c, normal, negrita, MARGEN + cardW + 18, 410, cardW, 48, "Distancia total", numero(informe.getKilometrosTotales()) + " km");
            tarjeta(c, normal, negrita, MARGEN, 348, cardW, 48, "Tiempo total", minutos(informe.getMinutosTotales()));
            tarjeta(c, normal, negrita, MARGEN + cardW + 18, 348, cardW, 48, "Carga acumulada", numero(informe.getCargaTotal()) + " UA");

            String mejor = Double.isNaN(informe.getMejorMarcaSegundos())
                    ? "Sin pruebas en este periodo" : tiempo(informe.getMejorMarcaSegundos());
            texto(c, negrita, 12, MARGEN, 306, "Mejor marca de 5 km: " + mejor, COLOR.AZUL);
            parrafo(c, normal, 9, MARGEN, 270, ANCHO_CONTENIDO,
                    "Informe individual creado a partir de los entrenamientos y pruebas registrados en la aplicación durante el periodo seleccionado.",
                    COLOR.SECUNDARIO, 14);
        }
    }

    private static void crearPaginaEntrenamientos(PDDocument documento, ResumenInforme informe,
            PDFont normal, PDFont negrita, byte[] grafico) throws IOException {
        PDPage pagina = nuevaPagina(documento);
        try (PDPageContentStream c = new PDPageContentStream(documento, pagina)) {
            dibujarCabeceraPie(c, normal, negrita, 2);
            seccion(c, normal, negrita, "Entrenamiento semanal", MARGEN, 756);
            texto(c, normal, 9, MARGEN, 735,
                    "Totales agrupados por semanas de lunes a domingo; la carga se calcula con los valores del proyecto.", COLOR.SECUNDARIO);

            List<String[]> filas = new ArrayList<>();
            for (ResumenInforme.ResumenSemanal semana : informe.getSemanas()) {
                String media = semana.getMediaMovilTresSemanas() == null
                        ? "—" : numero(semana.getMediaMovilTresSemanas());
                filas.add(new String[]{
                    semana.getInicio().format(FECHA), Integer.toString(semana.getSesiones()),
                    numero(semana.getKilometros()), Integer.toString(semana.getMinutos()),
                    numero(semana.getCarga()), media
                });
            }
            if (filas.isEmpty()) filas.add(new String[]{"Sin entrenamientos", "—", "—", "—", "—", "—"});
            tabla(c, normal, negrita, MARGEN, 704,
                    new float[]{95, 63, 83, 82, 82, 104},
                    new String[]{"Semana desde", "Sesiones", "Km", "Minutos", "Carga UA", "Media móvil"}, filas);

            parrafo(c, normal, 8, MARGEN, 424, ANCHO_CONTENIDO,
                    "La media móvil de carga aparece cuando hay tres semanas consecutivas disponibles.", COLOR.SECUNDARIO, 11);
            seccion(c, normal, negrita, "Evolución de la carga semanal", MARGEN, 392);
            PDImageXObject imagen = PDImageXObject.createFromByteArray(documento, grafico, "carga-semanal.png");
            c.drawImage(imagen, MARGEN, 162, ANCHO_CONTENIDO, 205);
            parrafo(c, normal, 8, MARGEN, 143, ANCHO_CONTENIDO,
                    "Cada barra representa la carga semanal total del deportista (duración × RPE).", COLOR.SECUNDARIO, 11);
        }
    }

    private static void crearPaginaRendimiento(PDDocument documento, ResumenInforme informe,
            PDFont normal, PDFont negrita, byte[] grafico) throws IOException {
        PDPage pagina = nuevaPagina(documento);
        try (PDPageContentStream c = new PDPageContentStream(documento, pagina)) {
            dibujarCabeceraPie(c, normal, negrita, 3);
            seccion(c, normal, negrita, "Pruebas de rendimiento de 5 km", MARGEN, 756);
            texto(c, normal, 9, MARGEN, 735,
                    "La diferencia compara cada resultado con la primera prueba del periodo.", COLOR.SECUNDARIO);

            List<String[]> filas = new ArrayList<>();
            List<PruebaRendimiento> pruebas = informe.getPruebas();
            double primeraMarca = pruebas.isEmpty() ? Double.NaN : pruebas.get(0).getMarcaSegundos();
            for (PruebaRendimiento prueba : pruebas) {
                String delta = Double.isNaN(primeraMarca) ? "—" : diferencia(prueba.getMarcaSegundos() - primeraMarca);
                filas.add(new String[]{prueba.getFecha().format(FECHA), tiempo(prueba.getMarcaSegundos()), delta,
                    Integer.toString(prueba.getFcMedia()) + " ppm"});
            }
            if (filas.isEmpty()) filas.add(new String[]{"Sin pruebas en el periodo", "—", "—", "—"});
            tabla(c, normal, negrita, MARGEN, 704,
                    new float[]{140, 110, 130, 129},
                    new String[]{"Fecha", "Marca", "Diferencia", "FC media"}, filas);

            seccion(c, normal, negrita, "Evolución de la marca", MARGEN, 532);
            PDImageXObject imagen = PDImageXObject.createFromByteArray(documento, grafico, "evolucion-5k.png");
            c.drawImage(imagen, MARGEN, 274, ANCHO_CONTENIDO, 235);
            double[] r = informe.getRegresion();
            String textoTendencia = pruebas.size() >= 2
                    ? "Pendiente estimada: " + numero(r[1]) + " s/semana; R²: " + numero(r[2]) + "."
                    : "No hay suficientes pruebas para estimar una tendencia.";
            parrafo(c, normal, 9, MARGEN, 250, ANCHO_CONTENIDO,
                    textoTendencia + " La línea resume la relación observada y no explica sus causas.", COLOR.SECUNDARIO, 13);
        }
    }

    private static void crearPaginaInterpretacion(PDDocument documento, ResumenInforme informe,
            PDFont normal, PDFont negrita) throws IOException {
        PDPage pagina = nuevaPagina(documento);
        try (PDPageContentStream c = new PDPageContentStream(documento, pagina)) {
            dibujarCabeceraPie(c, normal, negrita, 4);
            seccion(c, normal, negrita, "Lectura de los datos", MARGEN, 756);
            float y = 720;
            for (String linea : interpretacion(informe)) {
                y = parrafo(c, normal, 11, MARGEN, y, ANCHO_CONTENIDO, linea, COLOR.TINTA, 17) - 18;
            }

            seccion(c, normal, negrita, "Cómo interpretar este informe", MARGEN, y - 6);
            String limitaciones = "Los indicadores son descriptivos y dependen de la calidad y cantidad de los registros. "
                    + "Una variación de carga o de marca no demuestra por sí sola una causa ni permite emitir un diagnóstico "
                    + "médico o una recomendación individual de entrenamiento. Las semanas sin sesiones se muestran con carga cero.";
            parrafo(c, normal, 10, MARGEN, y - 32, ANCHO_CONTENIDO, limitaciones, COLOR.TINTA, 15);

            float notaY = 180;
            c.setNonStrokingColor(COLOR.FONDO_SUAVE);
            c.addRect(MARGEN, notaY - 46, ANCHO_CONTENIDO, 58);
            c.fill();
            texto(c, negrita, 9, MARGEN + 14, notaY - 8, "Fuente de datos", COLOR.AZUL);
            parrafo(c, normal, 8, MARGEN + 14, notaY - 25, ANCHO_CONTENIDO - 28,
                    "Registros de entrenamientos y pruebas del proyecto. Periodo: "
                    + fecha(informe.getDesde()) + " a " + fecha(informe.getHasta()) + ".", COLOR.SECUNDARIO, 11);
        }
    }

    private static List<String> interpretacion(ResumenInforme informe) {
        List<String> resultado = new ArrayList<>();
        List<PruebaRendimiento> pruebas = informe.getPruebas();
        if (pruebas.size() >= 2) {
            double[] reg = informe.getRegresion();
            if (reg[1] < -0.5) {
                resultado.add("En las pruebas registradas se observa una reducción del tiempo de 5 km. Esto describe los resultados del periodo, sin atribuirlos a una causa concreta.");
            } else if (reg[1] > 0.5) {
                resultado.add("En las pruebas registradas se observa un aumento del tiempo de 5 km. Conviene interpretarlo junto con el contexto y la cantidad de datos disponibles.");
            } else {
                resultado.add("Las pruebas registradas muestran tiempos relativamente estables durante el periodo analizado.");
            }
        } else if (pruebas.size() == 1) {
            resultado.add("Solo hay una prueba de 5 km en el periodo; no se puede comparar una evolución.");
        } else {
            resultado.add("No hay pruebas de 5 km en el periodo seleccionado, por lo que no se puede describir una tendencia de rendimiento.");
        }

        List<ResumenInforme.ResumenSemanal> semanas = informe.getSemanas();
        if (semanas.size() >= 2) {
            double primera = semanas.get(0).getCarga();
            double ultima = semanas.get(semanas.size() - 1).getCarga();
            if (ultima > primera) resultado.add("La carga semanal total del último bloque registrado es superior a la del primero.");
            else if (ultima < primera) resultado.add("La carga semanal total del último bloque registrado es inferior a la del primero.");
            else resultado.add("La carga semanal total del primer y del último bloque registrado es igual.");
        } else if (semanas.size() == 1) {
            resultado.add("Solo hay una semana de entrenamientos; no se compara la evolución de la carga semanal.");
        } else {
            resultado.add("No hay entrenamientos en el periodo; el resumen de carga no puede calcularse.");
        }
        return resultado;
    }

    private static PDPage nuevaPagina(PDDocument documento) {
        PDPage pagina = new PDPage(PDRectangle.A4);
        documento.addPage(pagina);
        return pagina;
    }

    private static void dibujarCabeceraPie(PDPageContentStream c, PDFont normal, PDFont negrita, int pagina) throws IOException {
        c.setNonStrokingColor(COLOR.AZUL);
        c.addRect(0, ALTO - 12, ANCHO, 12);
        c.fill();
        texto(c, negrita, 8, MARGEN, ALTO - 32, "CENTRO DE RENDIMIENTO DEPORTIVO", COLOR.AZUL);
        c.setStrokingColor(COLOR.LINEA);
        c.setLineWidth(0.6f);
        c.moveTo(MARGEN, 42);
        c.lineTo(ANCHO - MARGEN, 42);
        c.stroke();
        texto(c, normal, 8, MARGEN, 27, "Informe generado desde la aplicación deportiva", COLOR.SECUNDARIO);
        textoDerecha(c, normal, 8, ANCHO - MARGEN, 27, "Página " + pagina + " de 4", COLOR.SECUNDARIO);
    }

    private static void dibujarLogo(PDPageContentStream c, PDFont normal, PDFont negrita) throws IOException {
        c.setNonStrokingColor(COLOR.AZUL);
        c.addRect(MARGEN, 742, 38, 38);
        c.fill();
        texto(c, negrita, 13, MARGEN + 7, 756, "CR", java.awt.Color.WHITE);
        texto(c, negrita, 10, MARGEN + 48, 760, "CENTRO DE RENDIMIENTO", COLOR.TINTA);
        texto(c, normal, 8, MARGEN + 48, 746, "Atletismo · seguimiento de datos", COLOR.SECUNDARIO);
    }

    private static void seccion(PDPageContentStream c, PDFont normal, PDFont negrita,
            String titulo, float x, float y) throws IOException {
        c.setNonStrokingColor(COLOR.VERDE);
        c.addRect(x, y - 4, 4, 18);
        c.fill();
        texto(c, negrita, 15, x + 12, y, titulo, COLOR.TINTA);
    }

    private static void tarjeta(PDPageContentStream c, PDFont normal, PDFont negrita,
            float x, float y, float ancho, float alto, String etiqueta, String valor) throws IOException {
        c.setNonStrokingColor(COLOR.FONDO_SUAVE);
        c.addRect(x, y, ancho, alto);
        c.fill();
        texto(c, normal, 8, x + 12, y + alto - 16, etiqueta, COLOR.SECUNDARIO);
        texto(c, negrita, 14, x + 12, y + 13, valor, COLOR.AZUL);
    }

    private static void tabla(PDPageContentStream c, PDFont normal, PDFont negrita,
            float x, float y, float[] anchos, String[] cabeceras, List<String[]> filas) throws IOException {
        float altoFila = 23;
        float total = 0;
        for (float ancho : anchos) total += ancho;
        c.setNonStrokingColor(COLOR.AZUL);
        c.addRect(x, y - altoFila, total, altoFila);
        c.fill();
        float cx = x;
        for (int i = 0; i < cabeceras.length; i++) {
            textoAjustado(c, negrita, 8, cx + 6, y - 15, cabeceras[i], anchos[i] - 12, java.awt.Color.WHITE);
            cx += anchos[i];
        }
        float filaY = y - altoFila;
        for (int f = 0; f < filas.size(); f++) {
            filaY -= altoFila;
            c.setNonStrokingColor(f % 2 == 0 ? COLOR.FONDO_SUAVE : java.awt.Color.WHITE);
            c.addRect(x, filaY, total, altoFila);
            c.fill();
            c.setStrokingColor(COLOR.LINEA);
            c.setLineWidth(0.35f);
            c.moveTo(x, filaY);
            c.lineTo(x + total, filaY);
            c.stroke();
            cx = x;
            for (int col = 0; col < anchos.length; col++) {
                String valor = col < filas.get(f).length ? filas.get(f)[col] : "";
                textoAjustado(c, normal, 8, cx + 6, filaY + 8, valor, anchos[col] - 12, COLOR.TINTA);
                cx += anchos[col];
            }
        }
        c.setStrokingColor(COLOR.LINEA);
        c.setLineWidth(0.6f);
        c.addRect(x, filaY, total, altoFila * (filas.size() + 1));
        c.stroke();
    }

    private static void texto(PDPageContentStream c, PDFont fuente, float tam,
            float x, float y, String texto, java.awt.Color color) throws IOException {
        c.setNonStrokingColor(color);
        c.beginText();
        c.setFont(fuente, tam);
        c.newLineAtOffset(x, y);
        c.showText(seguro(texto));
        c.endText();
    }

    private static void textoDerecha(PDPageContentStream c, PDFont fuente, float tam,
            float x, float y, String texto, java.awt.Color color) throws IOException {
        float ancho = fuente.getStringWidth(seguro(texto)) / 1000 * tam;
        texto(c, fuente, tam, x - ancho, y, texto, color);
    }

    private static void textoAjustado(PDPageContentStream c, PDFont fuente, float tam,
            float x, float y, String texto, float maxAncho, java.awt.Color color) throws IOException {
        String valor = seguro(texto);
        while (!valor.isEmpty() && fuente.getStringWidth(valor) / 1000 * tam > maxAncho) {
            valor = valor.substring(0, valor.length() - 1);
        }
        texto(c, fuente, tam, x, y, valor, color);
    }

    private static float parrafo(PDPageContentStream c, PDFont fuente, float tam,
            float x, float y, float maxAncho, String texto, java.awt.Color color, float interlineado) throws IOException {
        String[] palabras = seguro(texto).split("\\s+");
        String linea = "";
        float cursorY = y;
        for (String palabra : palabras) {
            String prueba = linea.isEmpty() ? palabra : linea + " " + palabra;
            if (!linea.isEmpty() && fuente.getStringWidth(prueba) / 1000 * tam > maxAncho) {
                texto(c, fuente, tam, x, cursorY, linea, color);
                cursorY -= interlineado;
                linea = palabra;
            } else {
                linea = prueba;
            }
        }
        if (!linea.isEmpty()) {
            texto(c, fuente, tam, x, cursorY, linea, color);
            cursorY -= interlineado;
        }
        return cursorY;
    }

    private static byte[] crearGraficoCarga(ResumenInforme informe) throws IOException {
        List<Double> valores = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>();
        for (ResumenInforme.ResumenSemanal semana : informe.getSemanas()) {
            valores.add(semana.getCarga());
            etiquetas.add(semana.getInicio().format(DateTimeFormatter.ofPattern("dd/MM")));
        }
        return crearGraficoBarras("Carga semanal (UA)", etiquetas, valores);
    }

    private static byte[] crearGraficoBarras(String titulo, List<String> etiquetas, List<Double> valores) throws IOException {
        int ancho = 1100, alto = 520;
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        prepararGrafico(g, ancho, alto, titulo);
        int izquierda = 78, arriba = 78, derecha = 40, abajo = 86;
        int plotW = ancho - izquierda - derecha, plotH = alto - arriba - abajo;
        double max = 0.0;
        for (double valor : valores) max = Math.max(max, valor);
        if (max <= 0) max = 1;
        dibujarCuadricula(g, izquierda, arriba, plotW, plotH, 0, max, false);
        if (valores.isEmpty()) {
            centrar(g, "Sin datos de entrenamiento", ancho / 2, alto / 2);
        } else {
            double espacio = (double) plotW / valores.size();
            int barW = Math.max(12, (int) Math.min(72, espacio * 0.52));
            for (int i = 0; i < valores.size(); i++) {
                int x = izquierda + (int) ((i + 0.5) * espacio) - barW / 2;
                int barH = (int) (plotH * valores.get(i) / max);
                g.setColor(new java.awt.Color(39, 129, 105));
                g.fillRoundRect(x, arriba + plotH - barH, barW, barH, 8, 8);
                g.setColor(new java.awt.Color(37, 53, 71));
                String val = String.format(ESPANOL, "%.0f", valores.get(i));
                g.drawString(val, x - 2, arriba + plotH - barH - 8);
                g.drawString(etiquetas.get(i), x - 10, arriba + plotH + 24);
            }
        }
        dibujarEjes(g, izquierda, arriba, plotW, plotH);
        g.dispose();
        return png(imagen);
    }

    private static byte[] crearGraficoMarcas(ResumenInforme informe) throws IOException {
        List<PruebaRendimiento> pruebas = informe.getPruebas();
        int ancho = 1100, alto = 520;
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        prepararGrafico(g, ancho, alto, "Evolución de marca de 5 km");
        int izquierda = 90, arriba = 78, derecha = 48, abajo = 86;
        int plotW = ancho - izquierda - derecha, plotH = alto - arriba - abajo;
        if (pruebas.size() < 2) {
            dibujarEjes(g, izquierda, arriba, plotW, plotH);
            centrar(g, "Se necesitan al menos dos pruebas", ancho / 2, alto / 2);
            g.dispose();
            return png(imagen);
        }

        double minimo = Double.POSITIVE_INFINITY;
        double maximo = Double.NEGATIVE_INFINITY;
        for (PruebaRendimiento prueba : pruebas) {
            minimo = Math.min(minimo, prueba.getMarcaSegundos());
            maximo = Math.max(maximo, prueba.getMarcaSegundos());
        }
        double rango = Math.max(20, maximo - minimo);
        minimo -= rango * 0.15;
        maximo += rango * 0.15;
        dibujarCuadricula(g, izquierda, arriba, plotW, plotH, minimo, maximo, true);
        g.setColor(new java.awt.Color(37, 53, 71));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double[] reg = informe.getRegresion();
        LocalDate fechaInicial = pruebas.get(0).getFecha();
        int[] xs = new int[pruebas.size()];
        int[] ys = new int[pruebas.size()];
        for (int i = 0; i < pruebas.size(); i++) {
            double xSemanas = java.time.temporal.ChronoUnit.DAYS.between(fechaInicial, pruebas.get(i).getFecha()) / 7.0;
            double escalaX = pruebas.size() == 1 ? 0 : xSemanas / Math.max(1.0,
                    java.time.temporal.ChronoUnit.DAYS.between(fechaInicial, pruebas.get(pruebas.size() - 1).getFecha()) / 7.0);
            xs[i] = izquierda + (int) (escalaX * plotW);
            ys[i] = arriba + plotH - (int) ((pruebas.get(i).getMarcaSegundos() - minimo) / (maximo - minimo) * plotH);
            if (i > 0) g.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
            g.fillOval(xs[i] - 7, ys[i] - 7, 14, 14);
            g.setFont(g.getFont().deriveFont(16f));
            g.drawString(tiempo(pruebas.get(i).getMarcaSegundos()), xs[i] - 32, ys[i] - 15);
            g.setFont(g.getFont().deriveFont(13f));
            g.drawString(pruebas.get(i).getFecha().format(DateTimeFormatter.ofPattern("dd/MM")), xs[i] - 18, arriba + plotH + 25);
        }
        double xMax = java.time.temporal.ChronoUnit.DAYS.between(fechaInicial, pruebas.get(pruebas.size() - 1).getFecha()) / 7.0;
        double y0 = reg[0];
        double y1 = reg[0] + reg[1] * xMax;
        int yReg0 = arriba + plotH - (int) ((y0 - minimo) / (maximo - minimo) * plotH);
        int yReg1 = arriba + plotH - (int) ((y1 - minimo) / (maximo - minimo) * plotH);
        g.setColor(new java.awt.Color(227, 147, 59));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{10f, 8f}, 0f));
        g.drawLine(izquierda, yReg0, izquierda + plotW, yReg1);
        dibujarEjes(g, izquierda, arriba, plotW, plotH);
        g.dispose();
        return png(imagen);
    }

    private static byte[] crearIlustracionAtletismo() throws IOException {
        int ancho = 720, alto = 430;
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(239, 246, 244));
        g.fillRect(0, 0, ancho, alto);
        g.setColor(new java.awt.Color(217, 234, 229));
        g.fillRoundRect(26, 28, ancho - 52, alto - 56, 28, 28);
        g.setColor(new java.awt.Color(39, 129, 105));
        g.setStroke(new BasicStroke(11, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(82, 130, 530, 210, 5, 165);
        g.drawArc(125, 168, 440, 170, 5, 165);
        g.setColor(new java.awt.Color(32, 76, 106));
        g.setStroke(new BasicStroke(22, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(370, 135, 337, 238);
        g.drawLine(337, 238, 269, 306);
        g.drawLine(337, 238, 415, 289);
        g.drawLine(348, 177, 430, 214);
        g.drawLine(348, 177, 290, 226);
        g.setColor(new java.awt.Color(227, 147, 59));
        g.fillOval(348, 78, 57, 57);
        g.setColor(new java.awt.Color(32, 76, 106));
        g.setStroke(new BasicStroke(7));
        g.drawLine(269, 306, 225, 319);
        g.drawLine(415, 289, 462, 302);
        g.setColor(new java.awt.Color(39, 129, 105));
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 19));
        g.drawString("ATLETISMO · DATOS · PROGRESO", 160, 385);
        g.dispose();
        return png(imagen);
    }

    private static void prepararGrafico(Graphics2D g, int ancho, int alto, String titulo) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, ancho, alto);
        g.setColor(new java.awt.Color(37, 53, 71));
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 24));
        g.drawString(titulo, 42, 42);
    }

    private static void dibujarCuadricula(Graphics2D g, int x, int y, int ancho, int alto,
            double minimo, double maximo, boolean ejeTemporal) {
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
        for (int i = 0; i <= 4; i++) {
            int gy = y + alto - i * alto / 4;
            g.setColor(new java.awt.Color(226, 232, 235));
            g.drawLine(x, gy, x + ancho, gy);
            g.setColor(new java.awt.Color(100, 114, 124));
            double valor = minimo + (maximo - minimo) * i / 4;
            String etiqueta = ejeTemporal ? tiempo(valor) : String.format(ESPANOL, "%.0f", valor);
            g.drawString(etiqueta, 20, gy + 5);
        }
    }

    private static void dibujarEjes(Graphics2D g, int x, int y, int ancho, int alto) {
        g.setColor(new java.awt.Color(130, 145, 152));
        g.setStroke(new BasicStroke(2));
        g.drawLine(x, y, x, y + alto);
        g.drawLine(x, y + alto, x + ancho, y + alto);
    }

    private static void centrar(Graphics2D g, String texto, int x, int y) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.setColor(new java.awt.Color(100, 114, 124));
        g.drawString(texto, x - fm.stringWidth(texto) / 2, y);
    }

    private static byte[] png(BufferedImage imagen) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private static String tiempo(double segundos) {
        int total = (int) Math.round(segundos);
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private static String minutos(int minutos) {
        return String.format("%d h %02d min", minutos / 60, minutos % 60);
    }

    private static String numero(double valor) {
        if (!Double.isFinite(valor)) return "—";
        return String.format(ESPANOL, "%.1f", valor);
    }

    private static String diferencia(double segundos) {
        if (Math.abs(segundos) < 0.5) return "0 s";
        return (segundos > 0 ? "+" : "−") + String.format(ESPANOL, "%.0f", Math.abs(segundos)) + " s";
    }

    private static String fecha(LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(FECHA);
    }

    private static String seguro(String texto) {
        return texto == null ? "" : texto.replace('\u0000', ' ');
    }

    private static final class ColorPaleta {
        private final java.awt.Color AZUL = new java.awt.Color(32, 76, 106);
        private final java.awt.Color VERDE = new java.awt.Color(39, 129, 105);
        private final java.awt.Color TINTA = new java.awt.Color(37, 53, 71);
        private final java.awt.Color SECUNDARIO = new java.awt.Color(100, 114, 124);
        private final java.awt.Color FONDO_SUAVE = new java.awt.Color(239, 246, 244);
        private final java.awt.Color LINEA = new java.awt.Color(213, 223, 226);
    }
}
