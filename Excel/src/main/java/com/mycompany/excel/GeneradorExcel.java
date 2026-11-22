package com.mycompany.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.*;

// Parte 5: Libro Excel y requisitos tecnicos
public class GeneradorExcel {

    private static class ResumenSemanal {
        private double cargaUA;
        private double distanciaKm;
        private int duracionMin;
        private final double[] cargaDiaria = new double[7];

        private void agregar(Entrenamiento entrenamiento) {
            cargaUA += entrenamiento.getCargaUA();
            distanciaKm += entrenamiento.getDistanciaKm();
            duracionMin += entrenamiento.getDuracionMin();
            int dia = entrenamiento.getFecha().getDayOfWeek().getValue() - 1;
            cargaDiaria[dia] += entrenamiento.getCargaUA();
        }
    }

    private static LocalDate inicioSemana(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static String etiquetaSemana(LocalDate inicio) {
        return String.format("%d-W%02d", inicio.get(IsoFields.WEEK_BASED_YEAR), inicio.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    private static Map<String, NavigableMap<LocalDate, ResumenSemanal>> calcularResumenSemanal(
            List<Deportista> deportistas, List<Entrenamiento> entrenamientos) {
        Map<String, NavigableMap<LocalDate, ResumenSemanal>> resultado = new LinkedHashMap<>();

        for (Deportista deportista : deportistas) {
            List<Entrenamiento> sesiones = new ArrayList<>();
            for (Entrenamiento entrenamiento : entrenamientos) {
                if (entrenamiento.getIdDeportista().equalsIgnoreCase(deportista.getId())) {
                    sesiones.add(entrenamiento);
                }
            }

            NavigableMap<LocalDate, ResumenSemanal> semanas = new TreeMap<>();
            if (sesiones.isEmpty()) {
                resultado.put(deportista.getId(), semanas);
                continue;
            }

            LocalDate primeraSemana = inicioSemana(sesiones.stream()
                    .map(Entrenamiento::getFecha).min(LocalDate::compareTo).orElseThrow());
            LocalDate ultimaSemana = inicioSemana(sesiones.stream()
                    .map(Entrenamiento::getFecha).max(LocalDate::compareTo).orElseThrow());

            for (LocalDate semana = primeraSemana; !semana.isAfter(ultimaSemana); semana = semana.plusWeeks(1)) {
                semanas.put(semana, new ResumenSemanal());
            }

            for (Entrenamiento entrenamiento : sesiones) {
                semanas.get(inicioSemana(entrenamiento.getFecha())).agregar(entrenamiento);
            }
            resultado.put(deportista.getId(), semanas);
        }
        return resultado;
    }

    public static void exportarExcel(List<Deportista> deportistas, List<Entrenamiento> entrenamientos, List<PruebaRendimiento> pruebas, String rutaArchivo) throws IOException {
        Map<String, NavigableMap<LocalDate, ResumenSemanal>> resumenSemanal = calcularResumenSemanal(deportistas, entrenamientos);
        try (XSSFWorkbook libro = new XSSFWorkbook()) {

            CellStyle estiloCabecera = libro.createCellStyle();
            Font fuenteCabecera = libro.createFont();
            fuenteCabecera.setBold(true);
            fuenteCabecera.setColor(IndexedColors.WHITE.getIndex());
            estiloCabecera.setFont(fuenteCabecera);
            estiloCabecera.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estiloCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloCabecera.setAlignment(HorizontalAlignment.CENTER);

            CellStyle estiloBorde = libro.createCellStyle();
            estiloBorde.setBorderBottom(BorderStyle.THIN);
            estiloBorde.setBorderTop(BorderStyle.THIN);
            estiloBorde.setBorderLeft(BorderStyle.THIN);
            estiloBorde.setBorderRight(BorderStyle.THIN);

            CellStyle estiloDecimal = libro.createCellStyle();
            estiloDecimal.cloneStyleFrom(estiloBorde);
            estiloDecimal.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            // Hoja 1: Resumen
            XSSFSheet hojaResumen = libro.createSheet("Resumen");
            hojaResumen.createFreezePane(0, 1);

            Row r0Res = hojaResumen.createRow(0);
            String[] cabResumen = {"ID", "Nombre", "Categoría", "Sesiones", "Km Totales", "Carga Total (UA)", "Km Semanales"};
            for (int i = 0; i < cabResumen.length; i++) {
                Cell c = r0Res.createCell(i);
                c.setCellValue(cabResumen[i]);
                c.setCellStyle(estiloCabecera);
            }

            int fRes = 1;
            for (Deportista d : deportistas) {
                double kmTot = 0, cargaTot = 0;
                int count = 0;
                for (Entrenamiento e : entrenamientos) {
                    if (e.getIdDeportista().equalsIgnoreCase(d.getId())) {
                        kmTot += e.getDistanciaKm();
                        cargaTot += e.getCargaUA();
                        count++;
                    }
                }
                Row row = hojaResumen.createRow(fRes++);
                row.createCell(0).setCellValue(d.getId());
                row.createCell(1).setCellValue(d.getNombre());
                row.createCell(2).setCellValue(d.getCategoria());
                row.createCell(3).setCellValue(count);
                Cell cKm = row.createCell(4);
                cKm.setCellValue(kmTot);
                cKm.setCellStyle(estiloDecimal);
                row.createCell(5).setCellValue(cargaTot);
                Cell cKmSem = row.createCell(6);
                NavigableMap<LocalDate, ResumenSemanal> semanas = resumenSemanal.get(d.getId());
                if (semanas == null || semanas.isEmpty()) {
                    cKmSem.setCellValue("Sin entrenamientos");
                } else {
                    cKmSem.setCellValue(kmTot / semanas.size());
                    cKmSem.setCellStyle(estiloDecimal);
                }
            }

            Row rTot = hojaResumen.createRow(fRes);
            rTot.createCell(0).setCellValue("TOTALES");
            Cell cFormKm = rTot.createCell(4);
            cFormKm.setCellFormula("SUM(E2:E" + fRes + ")");
            Cell cFormCarga = rTot.createCell(5);
            cFormCarga.setCellFormula("SUM(F2:F" + fRes + ")");
            Cell cFormMed = rTot.createCell(6);
            cFormMed.setCellFormula("AVERAGE(G2:G" + fRes + ")");

            for (int i = 0; i < cabResumen.length; i++) hojaResumen.autoSizeColumn(i);

            crearGraficoBarras(hojaResumen, 1, 1, fRes - 1, 5, "Comparativa Carga Total Atletas", 8, 1, 16, 12);

            // Hoja 2: Deportistas
            XSSFSheet hojaDep = libro.createSheet("Deportistas");
            hojaDep.createFreezePane(0, 1);
            Row r0Dep = hojaDep.createRow(0);
            String[] cabDep = {"ID", "Nombre", "Edad", "Categoría", "Objetivo", "Marca Inicial 5k (seg)", "Marca Inicial (min)"};
            for (int i = 0; i < cabDep.length; i++) {
                Cell c = r0Dep.createCell(i);
                c.setCellValue(cabDep[i]);
                c.setCellStyle(estiloCabecera);
            }
            int fDep = 1;
            for (Deportista d : deportistas) {
                Row r = hojaDep.createRow(fDep++);
                r.createCell(0).setCellValue(d.getId());
                r.createCell(1).setCellValue(d.getNombre());
                r.createCell(2).setCellValue(d.getEdad());
                r.createCell(3).setCellValue(d.getCategoria());
                r.createCell(4).setCellValue(d.getObjetivo());
                r.createCell(5).setCellValue(d.getMarcaInicial5k());
                int min = (int) d.getMarcaInicial5k() / 60;
                int seg = (int) d.getMarcaInicial5k() % 60;
                r.createCell(6).setCellValue(String.format("%02d:%02d", min, seg));
            }
            for (int i = 0; i < cabDep.length; i++) hojaDep.autoSizeColumn(i);

            // Hoja 3: Entrenamientos
            XSSFSheet hojaEnt = libro.createSheet("Entrenamientos");
            hojaEnt.createFreezePane(0, 1);
            Row r0Ent = hojaEnt.createRow(0);
            String[] cabEnt = {"ID Sesión", "Atleta", "Fecha", "Tipo Sesión", "Duración (min)", "Distancia (km)", "FC Media", "RPE (1-10)", "Carga UA"};
            for (int i = 0; i < cabEnt.length; i++) {
                Cell c = r0Ent.createCell(i);
                c.setCellValue(cabEnt[i]);
                c.setCellStyle(estiloCabecera);
            }
            int fEnt = 1;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (Entrenamiento e : entrenamientos) {
                Row r = hojaEnt.createRow(fEnt++);
                r.createCell(0).setCellValue(e.getId());
                r.createCell(1).setCellValue(e.getIdDeportista());
                r.createCell(2).setCellValue(e.getFecha().format(dtf));
                r.createCell(3).setCellValue(e.getTipoSesion());
                r.createCell(4).setCellValue(e.getDuracionMin());
                r.createCell(5).setCellValue(e.getDistanciaKm());
                r.createCell(6).setCellValue(e.getFcMedia());
                r.createCell(7).setCellValue(e.getRpe());
                r.createCell(8).setCellValue(e.getCargaUA());
            }

            hojaEnt.setAutoFilter(new CellRangeAddress(0, fEnt - 1, 0, cabEnt.length - 1));

            SheetConditionalFormatting sheetCF = hojaEnt.getSheetConditionalFormatting();
            ConditionalFormattingRule reglaRPE = sheetCF.createConditionalFormattingRule(ComparisonOperator.GE, "8");
            PatternFormatting patron = reglaRPE.createPatternFormatting();
            patron.setFillBackgroundColor(IndexedColors.ROSE.getIndex());
            patron.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
            sheetCF.addConditionalFormatting(new CellRangeAddress[]{ new CellRangeAddress(1, fEnt - 1, 7, 7) }, reglaRPE);

            for (int i = 0; i < cabEnt.length; i++) hojaEnt.autoSizeColumn(i);

            // Hoja 4: Analisis
            XSSFSheet hojaAna = libro.createSheet("Análisis");
            hojaAna.createFreezePane(0, 1);
            Row r0Ana = hojaAna.createRow(0);
            String[] cabAna = {"Atleta", "Semana ISO / atleta", "Carga Semanal (UA)", "Distancia (km)", "Tiempo (min)", "Media Móvil (3 sem)", "Monotonía", "Tensión Semanal"};
            for (int i = 0; i < cabAna.length; i++) {
                Cell c = r0Ana.createCell(i);
                c.setCellValue(cabAna[i]);
                c.setCellStyle(estiloCabecera);
            }

            List<Double> listaCargas = new ArrayList<>();
            int fAna = 1;
            for (Deportista deportista : deportistas) {
                NavigableMap<LocalDate, ResumenSemanal> semanas = resumenSemanal.get(deportista.getId());
                if (semanas == null || semanas.isEmpty()) continue;

                List<Map.Entry<LocalDate, ResumenSemanal>> semanasLista = new ArrayList<>(semanas.entrySet());
                for (int i = 0; i < semanasLista.size(); i++) {
                    Map.Entry<LocalDate, ResumenSemanal> entrada = semanasLista.get(i);
                    LocalDate inicio = entrada.getKey();
                    ResumenSemanal resumen = entrada.getValue();
                    listaCargas.add(resumen.cargaUA);

                    Row r = hojaAna.createRow(fAna++);
                    r.createCell(0).setCellValue(deportista.getNombre() + " (" + deportista.getId() + ")");
                    r.createCell(1).setCellValue(etiquetaSemana(inicio) + " - " + deportista.getId());
                    r.createCell(2).setCellValue(resumen.cargaUA);
                    r.createCell(3).setCellValue(resumen.distanciaKm);
                    r.createCell(4).setCellValue(resumen.duracionMin);

                    if (i >= 2) {
                        double mm = (semanasLista.get(i).getValue().cargaUA
                                + semanasLista.get(i - 1).getValue().cargaUA
                                + semanasLista.get(i - 2).getValue().cargaUA) / 3.0;
                        Cell cMM = r.createCell(5);
                        cMM.setCellValue(mm);
                        cMM.setCellStyle(estiloDecimal);
                    } else {
                        r.createCell(5).setCellValue("Datos insuficientes");
                    }

                    List<Double> dias = new ArrayList<>();
                    for (double cargaDiaria : resumen.cargaDiaria) dias.add(cargaDiaria);
                    double monotonia = CalculosDeportivos.monotonia(dias);
                    if (Double.isFinite(monotonia)) {
                        Cell cMon = r.createCell(6);
                        cMon.setCellValue(monotonia);
                        cMon.setCellStyle(estiloDecimal);
                        Cell cTen = r.createCell(7);
                        cTen.setCellValue(CalculosDeportivos.tensionSemanal(resumen.cargaUA, monotonia));
                        cTen.setCellStyle(estiloDecimal);
                    } else {
                        r.createCell(6).setCellValue("No calculable (DT=0)");
                        r.createCell(7).setCellValue("No calculable");
                    }
                }
            }

            if (fAna > 1) {
                hojaAna.setAutoFilter(new CellRangeAddress(0, fAna - 1, 0, cabAna.length - 1));
            }
            int ultimaFilaSemanal = fAna - 1;

            fAna += 2;
            Row rD0 = hojaAna.createRow(fAna++);
            rD0.createCell(0).setCellValue("ESTADÍSTICA DESCRIPTIVA (Java)");
            rD0.createCell(1).setCellValue("VALOR");

            hojaAna.createRow(fAna++).createCell(0).setCellValue("Media Carga:");
            hojaAna.getRow(fAna - 1).createCell(1).setCellValue(CalculosDeportivos.media(listaCargas));

            hojaAna.createRow(fAna++).createCell(0).setCellValue("Mediana Carga:");
            hojaAna.getRow(fAna - 1).createCell(1).setCellValue(CalculosDeportivos.mediana(listaCargas));

            hojaAna.createRow(fAna++).createCell(0).setCellValue("Mínimo Carga:");
            hojaAna.getRow(fAna - 1).createCell(1).setCellValue(CalculosDeportivos.minimo(listaCargas));

            hojaAna.createRow(fAna++).createCell(0).setCellValue("Máximo Carga:");
            hojaAna.getRow(fAna - 1).createCell(1).setCellValue(CalculosDeportivos.maximo(listaCargas));

            hojaAna.createRow(fAna++).createCell(0).setCellValue("Desviación Típica:");
            hojaAna.getRow(fAna - 1).createCell(1).setCellValue(CalculosDeportivos.desviacionTipica(listaCargas));

            hojaAna.createRow(fAna++).createCell(0).setCellValue("Coeficiente Variación (CV %):");
            hojaAna.getRow(fAna - 1).createCell(1).setCellValue(CalculosDeportivos.coeficienteVariacion(listaCargas));

            for (int i = 0; i < cabAna.length; i++) hojaAna.autoSizeColumn(i);

            if (ultimaFilaSemanal >= 1) {
                crearGraficoLineas(hojaAna, 1, 1, ultimaFilaSemanal, 2, "Evolución Carga Semanal por Atleta (UA)", 9, 1, 17, 10);
            }

            // Hoja 5: Rendimiento
            XSSFSheet hojaRen = libro.createSheet("Rendimiento");
            hojaRen.createFreezePane(0, 1);
            Row r0Ren = hojaRen.createRow(0);
            String[] cabRen = {"Semana (X)", "Marca Real (seg) (Y)", "Tiempo (min:seg)", "Recta Ajustada (seg)"};
            for (int i = 0; i < cabRen.length; i++) {
                Cell c = r0Ren.createCell(i);
                c.setCellValue(cabRen[i]);
                c.setCellStyle(estiloCabecera);
            }

            List<Double> xVal = new ArrayList<>();
            List<Double> yVal = new ArrayList<>();
            for (int i = 0; i < pruebas.size(); i++) {
                if (pruebas.get(i).getIdDeportista().equalsIgnoreCase("DEP01")) {
                    xVal.add((double) (xVal.size() + 1));
                    yVal.add(pruebas.get(i).getMarcaSegundos());
                }
            }

            double[] reg = CalculosDeportivos.regresionLineal(xVal, yVal);
            double a = reg[0];
            double b = reg[1];
            double r2 = reg[2];

            int fRen = 1;
            for (int i = 0; i < xVal.size(); i++) {
                Row r = hojaRen.createRow(fRen++);
                r.createCell(0).setCellValue(xVal.get(i));
                r.createCell(1).setCellValue(yVal.get(i));
                int m = (int) (double) yVal.get(i) / 60;
                int s = (int) (double) yVal.get(i) % 60;
                r.createCell(2).setCellValue(String.format("%02d:%02d", m, s));
                double yAjustado = a + (b * xVal.get(i));
                Cell cAj = r.createCell(3);
                cAj.setCellValue(yAjustado);
                cAj.setCellStyle(estiloDecimal);
            }

            Row rPred = hojaRen.createRow(fRen++);
            rPred.createCell(0).setCellValue("Semana 5 (Predicción)");
            rPred.createCell(1).setCellValue("-");
            double yPred5 = a + (b * 5.0);
            int mp = (int) yPred5 / 60;
            int sp = (int) yPred5 % 60;
            rPred.createCell(2).setCellValue(String.format("%02d:%02d", mp, sp));
            Cell cAj5 = rPred.createCell(3);
            cAj5.setCellValue(yPred5);
            cAj5.setCellStyle(estiloDecimal);

            fRen += 2;
            hojaRen.createRow(fRen++).createCell(0).setCellValue("PARÁMETRO REGRESIÓN");
            hojaRen.getRow(fRen - 1).createCell(1).setCellValue("VALOR");

            hojaRen.createRow(fRen++).createCell(0).setCellValue("Pendiente b (seg/sem):");
            hojaRen.getRow(fRen - 1).createCell(1).setCellValue(b);

            hojaRen.createRow(fRen++).createCell(0).setCellValue("Término indep a (seg):");
            hojaRen.getRow(fRen - 1).createCell(1).setCellValue(a);

            hojaRen.createRow(fRen++).createCell(0).setCellValue("Coeficiente R²:");
            hojaRen.getRow(fRen - 1).createCell(1).setCellValue(r2);

            for (int i = 0; i < cabRen.length; i++) hojaRen.autoSizeColumn(i);

            crearGraficoLineas(hojaRen, 0, 1, xVal.size(), 1, "Evolución de Marcas 5k", 5, 1, 13, 10);

            try (FileOutputStream fos = new FileOutputStream(rutaArchivo)) {
                libro.write(fos);
            }
        }
    }

    private static void crearGraficoBarras(XSSFSheet sheet, int colCat, int rIni, int rFin, int colVal, String titulo, int c1, int r1, int c2, int r2) {
        try {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, c1, r1, c2, r2);
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText(titulo);

            XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);

            XDDFDataSource<String> xs = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(rIni, rFin, colCat, colCat));
            XDDFNumericalDataSource<Double> ys = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(rIni, rFin, colVal, colVal));

            XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
            data.setBarDirection(BarDirection.COL);
            data.addSeries(xs, ys);
            chart.plot(data);
        } catch (Exception ex) {}
    }

    private static void crearGraficoLineas(XSSFSheet sheet, int colCat, int rIni, int rFin, int colVal, String titulo, int c1, int r1, int c2, int r2) {
        try {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, c1, r1, c2, r2);
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText(titulo);

            XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);

            XDDFDataSource<String> xs = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(rIni, rFin, colCat, colCat));
            XDDFNumericalDataSource<Double> ys = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(rIni, rFin, colVal, colVal));

            XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
            data.addSeries(xs, ys);
            chart.plot(data);
        } catch (Exception ex) {}
    }
}
