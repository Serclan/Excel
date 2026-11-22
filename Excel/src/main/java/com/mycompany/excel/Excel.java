package com.mycompany.excel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// Parte 2: Funcionalidades de la aplicacion e interfaz Swing
public class Excel extends JFrame {

    private List<Deportista> deportistas = new ArrayList<>();
    private List<Entrenamiento> entrenamientos = new ArrayList<>();
    private List<PruebaRendimiento> pruebas = new ArrayList<>();

    private DefaultTableModel modeloDep;
    private DefaultTableModel modeloEnt;
    private DefaultTableModel modeloPru;

    public Excel() {
        setTitle("Gestión y Rendimiento Deportivo - DI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 520);
        setLocationRelativeTo(null);

        cargarDatosIniciales();
        iniciarComponentes();
    }

    private void iniciarComponentes() {
        JTabbedPane pestañas = new JTabbedPane();

        // Apartado: Deportistas
        JPanel panelDep = new JPanel(new BorderLayout(5, 5));
        modeloDep = new DefaultTableModel(new String[]{"ID", "Nombre", "Edad", "Categoría", "Objetivo", "5k (seg)"}, 0);
        JTable tablaDep = new JTable(modeloDep);
        panelDep.add(new JScrollPane(tablaDep), BorderLayout.CENTER);

        JPanel btnPanelDep = new JPanel();
        JButton btnAddDep = new JButton("Nuevo Atleta");
        btnAddDep.addActionListener(e -> agregarAtletaDialogo());
        btnPanelDep.add(btnAddDep);
        panelDep.add(btnPanelDep, BorderLayout.SOUTH);
        pestañas.addTab("Deportistas", panelDep);

        // Apartado: Entrenamientos
        JPanel panelEnt = new JPanel(new BorderLayout(5, 5));
        modeloEnt = new DefaultTableModel(new String[]{"ID", "Atleta", "Fecha", "Tipo", "Minutos", "Km", "FC", "RPE", "Carga UA"}, 0);
        JTable tablaEnt = new JTable(modeloEnt);
        panelEnt.add(new JScrollPane(tablaEnt), BorderLayout.CENTER);

        JPanel btnPanelEnt = new JPanel();
        JButton btnAddEnt = new JButton("Nuevo Entrenamiento");
        btnAddEnt.addActionListener(e -> agregarEntrenamientoDialogo());
        btnPanelEnt.add(btnAddEnt);
        panelEnt.add(btnPanelEnt, BorderLayout.SOUTH);
        pestañas.addTab("Entrenamientos", panelEnt);

        // Apartado: Pruebas de rendimiento
        JPanel panelPru = new JPanel(new BorderLayout(5, 5));
        modeloPru = new DefaultTableModel(new String[]{"ID", "Atleta", "Fecha", "Marca (seg)", "FC Media"}, 0);
        JTable tablaPru = new JTable(modeloPru);
        panelPru.add(new JScrollPane(tablaPru), BorderLayout.CENTER);
        pestañas.addTab("Pruebas 5k", panelPru);

        // Apartado: Exportacion mediante JFileChooser
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnExportar = new JButton("Exportar Informe a Excel (.xlsx)");
        btnExportar.setFont(new Font("Arial", Font.BOLD, 13));
        btnExportar.setBackground(new Color(34, 139, 34));
        btnExportar.setForeground(Color.WHITE);
        btnExportar.addActionListener(e -> exportarExcelConFileChooser());
        panelInferior.add(btnExportar);

        JButton btnExportarPdf = new JButton("Generar informe PDF");
        btnExportarPdf.setFont(new Font("Arial", Font.BOLD, 13));
        btnExportarPdf.setBackground(new Color(32, 76, 106));
        btnExportarPdf.setForeground(Color.WHITE);
        btnExportarPdf.addActionListener(e -> exportarPDFConFileChooser());
        panelInferior.add(btnExportarPdf);

        add(pestañas, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        actualizarTablas();
    }

    private void agregarAtletaDialogo() {
        JTextField txtId = new JTextField();
        JTextField txtNom = new JTextField();
        JTextField txtEdad = new JTextField();
        JTextField txtCat = new JTextField("Senior");
        JTextField txtObj = new JTextField("Sub 16m 5k");
        JTextField txtMarca = new JTextField("1020");

        Object[] campos = {
            "ID Atleta:", txtId,
            "Nombre:", txtNom,
            "Edad:", txtEdad,
            "Categoría:", txtCat,
            "Objetivo:", txtObj,
            "Marca 5k (segundos):", txtMarca
        };

        int res = JOptionPane.showConfirmDialog(this, campos, "Alta de Atleta", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String id = txtId.getText().trim();
                String nom = txtNom.getText().trim();
                int edad = Integer.parseInt(txtEdad.getText().trim());
                double marca = Double.parseDouble(txtMarca.getText().trim());

                if (id.isEmpty() || nom.isEmpty() || edad <= 0 || marca <= 0) {
                    JOptionPane.showMessageDialog(this, "Datos inválidos. Compruebe valores numéricos positivos.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                deportistas.add(new Deportista(id, nom, edad, txtCat.getText().trim(), txtObj.getText().trim(), marca));
                actualizarTablas();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de formato en número: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void agregarEntrenamientoDialogo() {
        if (deportistas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Primero debe registrar al menos un atleta.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<Deportista> comboDep = new JComboBox<>(deportistas.toArray(new Deportista[0]));
        JTextField txtMin = new JTextField("50");
        JTextField txtKm = new JTextField("10.0");
        JTextField txtFc = new JTextField("150");
        JTextField txtRpe = new JTextField("7");
        JComboBox<String> comboTipo = new JComboBox<>(new String[]{"Rodaje", "Series", "Tirada larga", "Recuperación"});

        Object[] campos = {
            "Atleta:", comboDep,
            "Tipo:", comboTipo,
            "Duración (min):", txtMin,
            "Distancia (km):", txtKm,
            "FC Media (ppm):", txtFc,
            "RPE Esfuerzo (1 a 10):", txtRpe
        };

        int res = JOptionPane.showConfirmDialog(this, campos, "Nuevo Entrenamiento", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                int min = Integer.parseInt(txtMin.getText().trim());
                double km = Double.parseDouble(txtKm.getText().trim());
                int fc = Integer.parseInt(txtFc.getText().trim());
                int rpe = Integer.parseInt(txtRpe.getText().trim());

                if (min <= 0 || km <= 0 || rpe < 1 || rpe > 10) {
                    JOptionPane.showMessageDialog(this, "Valores fuera de rango. RPE debe estar entre 1 y 10.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Deportista sel = (Deportista) comboDep.getSelectedItem();
                String idEnt = "E" + (entrenamientos.size() + 1);
                entrenamientos.add(new Entrenamiento(idEnt, sel.getId(), LocalDate.now(), (String) comboTipo.getSelectedItem(), min, km, fc, rpe));
                actualizarTablas();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error en los datos numéricos.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarExcelConFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Informe_Rendimiento_Club.xlsx"));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                GeneradorExcel.exportarExcel(deportistas, entrenamientos, pruebas, f.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "¡Informe Excel generado con éxito!\nGuardado en: " + f.getAbsolutePath(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al generar el libro Excel: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarPDFConFileChooser() {
        if (deportistas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Primero debe registrar al menos un deportista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<Deportista> comboDeportistas = new JComboBox<>(deportistas.toArray(new Deportista[0]));
        JSpinner fechaDesde = crearSelectorFecha(fechaMasAntigua());
        JSpinner fechaHasta = crearSelectorFecha(fechaMasReciente());
        JPanel campos = new JPanel(new GridLayout(0, 2, 8, 8));
        campos.add(new JLabel("Deportista:"));
        campos.add(comboDeportistas);
        campos.add(new JLabel("Fecha inicial:"));
        campos.add(fechaDesde);
        campos.add(new JLabel("Fecha final:"));
        campos.add(fechaHasta);

        int respuesta = JOptionPane.showConfirmDialog(this, campos, "Generar informe PDF", JOptionPane.OK_CANCEL_OPTION);
        if (respuesta != JOptionPane.OK_OPTION) return;

        Deportista deportista = (Deportista) comboDeportistas.getSelectedItem();
        LocalDate desde = fechaLocal(fechaDesde);
        LocalDate hasta = fechaLocal(fechaHasta);
        JFileChooser selectorArchivo = new JFileChooser();
        selectorArchivo.setSelectedFile(new File("Informe_" + deportista.getId() + ".pdf"));
        if (selectorArchivo.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File destino = selectorArchivo.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        }
        if (destino.exists()) {
            int sobrescribir = JOptionPane.showConfirmDialog(this,
                    "El archivo ya existe. ¿Quieres reemplazarlo?", "Confirmar guardado", JOptionPane.YES_NO_OPTION);
            if (sobrescribir != JOptionPane.YES_OPTION) return;
        }

        try {
            ResumenInforme informe = ResumenInforme.crear(deportista, desde, hasta, entrenamientos, pruebas);
            GeneradorPDF.generar(informe, destino);
            JOptionPane.showMessageDialog(this,
                    "Informe PDF generado correctamente:\n" + destino.getAbsolutePath(),
                    "Informe creado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Revisa los datos", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo generar el informe PDF: " + ex.getMessage(),
                    "Error al generar el informe", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JSpinner crearSelectorFecha(LocalDate fecha) {
        Date valor = Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner selector = new JSpinner(new SpinnerDateModel(valor, null, null, Calendar.DAY_OF_MONTH));
        selector.setEditor(new JSpinner.DateEditor(selector, "dd/MM/yyyy"));
        return selector;
    }

    private LocalDate fechaLocal(JSpinner selector) {
        Date valor = (Date) selector.getValue();
        return valor.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDate fechaMasAntigua() {
        LocalDate fecha = LocalDate.now();
        boolean encontrada = false;
        for (Entrenamiento entrenamiento : entrenamientos) {
            if (!encontrada || entrenamiento.getFecha().isBefore(fecha)) fecha = entrenamiento.getFecha();
            encontrada = true;
        }
        for (PruebaRendimiento prueba : pruebas) {
            if (!encontrada || prueba.getFecha().isBefore(fecha)) fecha = prueba.getFecha();
            encontrada = true;
        }
        return fecha;
    }

    private LocalDate fechaMasReciente() {
        LocalDate fecha = LocalDate.now();
        boolean encontrada = false;
        for (Entrenamiento entrenamiento : entrenamientos) {
            if (!encontrada || entrenamiento.getFecha().isAfter(fecha)) fecha = entrenamiento.getFecha();
            encontrada = true;
        }
        for (PruebaRendimiento prueba : pruebas) {
            if (!encontrada || prueba.getFecha().isAfter(fecha)) fecha = prueba.getFecha();
            encontrada = true;
        }
        return fecha;
    }

    private void actualizarTablas() {
        modeloDep.setRowCount(0);
        for (Deportista d : deportistas) {
            modeloDep.addRow(new Object[]{d.getId(), d.getNombre(), d.getEdad(), d.getCategoria(), d.getObjetivo(), d.getMarcaInicial5k()});
        }

        modeloEnt.setRowCount(0);
        for (Entrenamiento e : entrenamientos) {
            modeloEnt.addRow(new Object[]{e.getId(), e.getIdDeportista(), e.getFecha(), e.getTipoSesion(), e.getDuracionMin(), e.getDistanciaKm(), e.getFcMedia(), e.getRpe(), e.getCargaUA()});
        }

        modeloPru.setRowCount(0);
        for (PruebaRendimiento p : pruebas) {
            modeloPru.addRow(new Object[]{p.getId(), p.getIdDeportista(), p.getFecha(), p.getMarcaSegundos(), p.getFcMedia()});
        }
    }

    private void cargarDatosIniciales() {
        deportistas.add(new Deportista("DEP01", "Carlos Gómez", 24, "Senior", "Bajar de 16m en 5k", 1020));
        deportistas.add(new Deportista("DEP02", "Elena Morales", 22, "Sub-23", "Bajar de 18m en 5k", 1140));
        deportistas.add(new Deportista("DEP03", "Marcos Ruiz", 35, "Master 35", "Sub 17m30s en 5k", 1080));

        LocalDate b = LocalDate.of(2026, 10, 5);
        entrenamientos.add(new Entrenamiento("E01", "DEP01", b, "Rodaje", 45, 9.0, 142, 6));
        entrenamientos.add(new Entrenamiento("E02", "DEP01", b.plusDays(1), "Series", 50, 10.5, 168, 8));
        entrenamientos.add(new Entrenamiento("E03", "DEP01", b.plusDays(3), "Rodaje", 40, 8.0, 138, 5));
        entrenamientos.add(new Entrenamiento("E04", "DEP01", b.plusDays(4), "Series", 55, 11.0, 172, 9));
        entrenamientos.add(new Entrenamiento("E05", "DEP01", b.plusDays(6), "Tirada larga", 75, 16.0, 148, 7));

        entrenamientos.add(new Entrenamiento("E06", "DEP01", b.plusWeeks(1), "Rodaje", 50, 10.0, 145, 6));
        entrenamientos.add(new Entrenamiento("E07", "DEP01", b.plusWeeks(1).plusDays(1), "Series", 55, 11.5, 170, 8));
        entrenamientos.add(new Entrenamiento("E08", "DEP01", b.plusWeeks(1).plusDays(3), "Rodaje", 45, 9.0, 140, 6));
        entrenamientos.add(new Entrenamiento("E09", "DEP01", b.plusWeeks(1).plusDays(4), "Series", 60, 12.0, 175, 9));
        entrenamientos.add(new Entrenamiento("E10", "DEP01", b.plusWeeks(1).plusDays(6), "Tirada larga", 80, 17.5, 150, 7));

        pruebas.add(new PruebaRendimiento("T01", "DEP01", b.plusDays(6), 1015, 178));
        pruebas.add(new PruebaRendimiento("T02", "DEP01", b.plusWeeks(1).plusDays(6), 998, 179));
        pruebas.add(new PruebaRendimiento("T03", "DEP01", b.plusWeeks(2).plusDays(6), 980, 181));
        pruebas.add(new PruebaRendimiento("T04", "DEP01", b.plusWeeks(3).plusDays(6), 962, 180));

        entrenamientos.add(new Entrenamiento("E11", "DEP02", b, "Rodaje", 40, 7.5, 145, 5));
        entrenamientos.add(new Entrenamiento("E12", "DEP02", b.plusDays(2), "Series", 48, 9.0, 169, 8));
        entrenamientos.add(new Entrenamiento("E13", "DEP02", b.plusDays(5), "Tirada larga", 68, 13.5, 151, 7));
        entrenamientos.add(new Entrenamiento("E14", "DEP02", b.plusWeeks(1), "Rodaje", 44, 8.5, 147, 5));
        entrenamientos.add(new Entrenamiento("E15", "DEP02", b.plusWeeks(1).plusDays(2), "Series", 52, 10.0, 171, 8));
        entrenamientos.add(new Entrenamiento("E16", "DEP02", b.plusWeeks(1).plusDays(5), "Tirada larga", 72, 14.0, 154, 7));
        entrenamientos.add(new Entrenamiento("E17", "DEP02", b.plusWeeks(2), "Rodaje", 46, 9.0, 149, 6));
        entrenamientos.add(new Entrenamiento("E18", "DEP02", b.plusWeeks(2).plusDays(2), "Series", 54, 10.5, 173, 8));
        entrenamientos.add(new Entrenamiento("E19", "DEP02", b.plusWeeks(2).plusDays(5), "Tirada larga", 75, 15.0, 156, 7));
        entrenamientos.add(new Entrenamiento("E20", "DEP02", b.plusWeeks(3), "Rodaje", 48, 9.2, 150, 6));
        entrenamientos.add(new Entrenamiento("E21", "DEP02", b.plusWeeks(3).plusDays(2), "Series", 56, 11.0, 175, 8));
        entrenamientos.add(new Entrenamiento("E22", "DEP02", b.plusWeeks(3).plusDays(5), "Tirada larga", 78, 15.5, 158, 7));

        pruebas.add(new PruebaRendimiento("T05", "DEP02", b.plusDays(5), 1138, 176));
        pruebas.add(new PruebaRendimiento("T06", "DEP02", b.plusWeeks(1).plusDays(5), 1124, 177));
        pruebas.add(new PruebaRendimiento("T07", "DEP02", b.plusWeeks(2).plusDays(5), 1110, 179));
        pruebas.add(new PruebaRendimiento("T08", "DEP02", b.plusWeeks(3).plusDays(5), 1104, 180));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Excel().setVisible(true));
    }
}
