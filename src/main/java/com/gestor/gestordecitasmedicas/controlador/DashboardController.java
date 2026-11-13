package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.Asistente;
import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.Consulta;
import com.gestor.gestordecitasmedicas.modelo.Estado;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import com.gestor.gestordecitasmedicas.modelo.PlanificadorAgenda;
import com.gestor.gestordecitasmedicas.modelo.Receta;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.time.temporal.ChronoUnit;
import javafx.util.StringConverter;

/** Panel de inicio del administrador: indicadores, citas del día y gráficas calculadas desde la base de datos. */
public class DashboardController implements PanelControlado, PanelRefrescable {
    @FXML private Label lblFecha;
    @FXML private VBox alertas, citasHoy, proximaCita, actividad;
    @FXML private HBox kpis;
    @FXML private StackPane graficaSemana, graficaEstado, graficaMeses;

    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie"};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("hh:mm a");

    private MainController mainController;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        refrescar();
    }

    @Override
    public void refrescar() {
        LocalDate hoy = LocalDate.now();
        lblFecha.setText("Resumen general · " + hoy.getDayOfMonth() + " de " + PlanificadorAgenda.MESES[hoy.getMonthValue() - 1]
                + " de " + hoy.getYear());

        HashMap<String, Paciente> pacientes = Paciente.todos();
        List<Cita> citas = new ArrayList<>(Cita.todas().values());
        citas.sort(Comparator.comparing(Cita::getFechaCita, Comparator.nullsLast(Comparator.naturalOrder())));
        int asistentes = Asistente.todos().size();

        LocalDate lunes = PlanificadorAgenda.lunesDe(hoy);
        YearMonth mesActual = YearMonth.from(hoy);

        List<Cita> deHoy = new ArrayList<>();
        int pendientesSemana = 0, atendidasHoy = 0, atendidasMes = 0;
        int[] porDia = new int[5];
        int[] porEstado = new int[3];
        int[] porMes = new int[6];
        List<Cita> vencidas = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();

        for (Cita c : citas) {
            LocalDateTime f = c.fechaHora();
            if (f == null) continue;
            LocalDate d = f.toLocalDate();
            Estado e = c.getEstado() == null ? Estado.AGENDADA : c.getEstado();

            if (d.equals(hoy)) {
                deHoy.add(c);
                if (e == Estado.ATENDIDA) atendidasHoy++;
            }
            if (!d.isBefore(lunes) && !d.isAfter(lunes.plusDays(6)) && e == Estado.AGENDADA) pendientesSemana++;
            int idxDia = (int) ChronoUnit.DAYS.between(lunes, d);
            if (idxDia >= 0 && idxDia < 5 && e != Estado.CANCELADA) porDia[idxDia]++;
            if (YearMonth.from(d).equals(mesActual)) {
                porEstado[e.ordinal()]++;
                if (e == Estado.ATENDIDA) atendidasMes++;
            }
            if (e == Estado.ATENDIDA) {
                int atras = (int) ChronoUnit.MONTHS.between(YearMonth.from(d), mesActual);
                if (atras >= 0 && atras < 6) porMes[5 - atras]++;
            }
            if (e == Estado.AGENDADA && f.isBefore(ahora.minusHours(1))) vencidas.add(c);
        }

        renderKpis(deHoy.size(), atendidasHoy, pendientesSemana, pacientes.size(), atendidasMes, asistentes);
        renderAlertas(pacientes, vencidas);
        renderCitasHoy(deHoy, pacientes, ahora);
        renderGraficas(porDia, porEstado, porMes, mesActual);
        renderActividad(pacientes);
    }

    // ---------------------------------------------------------------- KPIs

    private void renderKpis(int hoy, int atendidasHoy, int pendientes, int pacientes, int consultasMes, int asistentes) {
        kpis.getChildren().setAll(
                kpi("fi-rr-calendar", "CITAS DE HOY", String.valueOf(hoy), atendidasHoy + " atendidas"),
                kpi("fi-rr-clock", "PENDIENTES SEMANA", String.valueOf(pendientes), "Citas agendadas"),
                kpi("fi-rr-users", "PACIENTES", String.valueOf(pacientes), "Registrados"),
                kpi("fi-rr-notes-medical", "CONSULTAS DEL MES", String.valueOf(consultasMes), "Citas atendidas"),
                kpi("fi-rr-user-md", "ASISTENTES", String.valueOf(asistentes), "Con acceso al sistema"));
        for (int i = 0; i < kpis.getChildren().size(); i++) Ui.entrada(kpis.getChildren().get(i), i * 60);
    }

    private VBox kpi(String icono, String titulo, String valor, String detalle) {
        Label t = new Label(titulo);
        t.getStyleClass().add("caption");
        t.setGraphic(new FontIcon(icono));
        t.setGraphicTextGap(8);
        Label v = new Label(valor);
        v.getStyleClass().add("kpi-value");
        Ui.contar(v, Integer.parseInt(valor));
        Label d = new Label(detalle);
        d.getStyleClass().add("hint");
        VBox caja = new VBox(6, t, v, d);
        caja.getStyleClass().addAll("card", "kpi-card");
        HBox.setHgrow(caja, Priority.ALWAYS);
        caja.setMaxWidth(Double.MAX_VALUE);
        caja.setPrefWidth(0);
        return caja;
    }

    // ------------------------------------------------------------- Alertas

    private void renderAlertas(HashMap<String, Paciente> pacientes, List<Cita> vencidas) {
        alertas.getChildren().clear();
        long sinCorreo = pacientes.values().stream()
                .filter(p -> p.getCorreo() == null || p.getCorreo().trim().isEmpty()).count();
        if (!vencidas.isEmpty()) {
            alertas.getChildren().add(aviso("chip-amber", "fi-rr-triangle-warning",
                    vencidas.size() + " cita(s) agendada(s) con fecha pasada siguen sin atender o cancelar."));
        }
        if (sinCorreo > 0) {
            alertas.getChildren().add(aviso("chip-blood", "fi-rr-envelope",
                    sinCorreo + " paciente(s) sin correo: no recibirán la confirmación de sus citas."));
        }
        alertas.setManaged(!alertas.getChildren().isEmpty());
        alertas.setVisible(alertas.isManaged());
    }

    private HBox aviso(String estilo, String icono, String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("info-value");
        l.setWrapText(true);
        HBox caja = new HBox(12, new FontIcon(icono), l);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().addAll("notice-box", estilo);
        return caja;
    }

    // --------------------------------------------------------- Citas de hoy

    private void renderCitasHoy(List<Cita> deHoy, HashMap<String, Paciente> pacientes, LocalDateTime ahora) {
        citasHoy.getChildren().clear();
        proximaCita.getChildren().clear();
        if (deHoy.isEmpty()) {
            citasHoy.getChildren().add(vacio("No hay citas programadas para hoy."));
        }
        Cita proxima = null;
        for (Cita c : deHoy) {
            citasHoy.getChildren().add(filaCita(c, pacientes.get(c.getCurp())));
            if (proxima == null && c.getEstado() == Estado.AGENDADA && !c.fechaHora().isBefore(ahora.minusMinutes(45))) {
                proxima = c;
            }
        }
        if (proxima == null) {
            proximaCita.getChildren().add(vacio("Sin citas pendientes por atender hoy."));
            return;
        }
        Paciente p = pacientes.get(proxima.getCurp());
        Label nombre = new Label(Paciente.nombreParaMostrar(p, proxima.getCurp()));
        nombre.getStyleClass().add("row-name");
        Label hora = new Label(proxima.fechaHora().format(HORA));
        hora.getStyleClass().addAll("chip", "chip-amber");
        Label ref = new Label("Cita #" + proxima.getIdCita());
        ref.getStyleClass().add("hint");
        Button iniciar = new Button("Iniciar Cita Clínica");
        iniciar.getStyleClass().add("btn-primary");
        iniciar.setMaxWidth(Double.MAX_VALUE);
        iniciar.setGraphic(new FontIcon("fi-rr-play"));
        final Cita elegida = proxima;
        iniciar.setOnAction(e -> iniciar(elegida));
        proximaCita.getChildren().addAll(nombre, hora, ref, iniciar);
    }

    private HBox filaCita(Cita cita, Paciente paciente) {
        Label hora = new Label(cita.fechaHora().format(HORA));
        hora.getStyleClass().add("info-value");
        hora.setMinWidth(80);
        Label nombre = new Label(Paciente.nombreParaMostrar(paciente, cita.getCurp()));
        nombre.getStyleClass().add("row-name");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label estado = new Label(Estado.etiqueta(cita.getEstado()));
        estado.getStyleClass().addAll("chip", estiloEstado(cita.getEstado()));
        HBox fila = new HBox(12, hora, nombre, espacio, estado);
        if (cita.getEstado() == Estado.AGENDADA) {
            Button b = new Button("Iniciar");
            b.getStyleClass().add("btn-edit");
            b.setGraphic(new FontIcon("fi-rr-play"));
            b.setOnAction(e -> iniciar(cita));
            fila.getChildren().add(b);
        }
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("row-card");
        fila.setCursor(Cursor.HAND);
        fila.setOnMouseClicked(e -> abrirEnAgenda(cita));
        return fila;
    }

    private void abrirEnAgenda(Cita cita) {
        if (mainController == null) return;
        AgendaController agenda = mainController.controlador("agenda-panel.fxml");
        agenda.seleccionarCita(cita);
        mainController.ir("agenda-panel.fxml");
    }

    private void abrirConsulta(Receta receta) {
        if (mainController == null) return;
        ConsultaController editor = mainController.controlador("consulta-panel.fxml");
        if (!editor.cargarConsultaParaEdicion(Consulta.deCita(receta.getIdCita()))) {
            Notificador.error("No disponible", "No se encontró la cita vinculada a esta consulta.");
            return;
        }
        mainController.ir("consulta-panel.fxml");
    }

    @FXML
    private void onHorario() {
        if (mainController == null || !mainController.esAdministrador()) return;
        if (HorarioConfigController.abrir(lblFecha.getScene().getWindow())) refrescar();
    }

    private void iniciar(Cita cita) {
        if (mainController == null || !mainController.esAdministrador()) return;
        CitaController controlador = mainController.controlador("cita-panel.fxml");
        controlador.cargarCita(cita);
        mainController.ir("cita-panel.fxml");
    }

    // ------------------------------------------------------------ Gráficas

    private void renderGraficas(int[] porDia, int[] porEstado, int[] porMes, YearMonth mesActual) {
        XYChart.Series<String, Number> semana = new XYChart.Series<>();
        for (int i = 0; i < 5; i++) semana.getData().add(new XYChart.Data<>(DIAS[i], porDia[i]));
        graficaSemana.getChildren().setAll(barras(semana));

        PieChart pastel = new PieChart();
        String[] nombres = {"Agendadas", "Atendidas", "Canceladas"};
        // Orden del enum Estado: AGENDADA, ATENDIDA, CANCELADA. Se agregan las 3 siempre para que
        // cada estado conserve su color (default-color0/1/2) aunque alguno valga 0.
        int totalEstados = 0;
        for (int i = 0; i < 3; i++) {
            totalEstados += porEstado[i];
            pastel.getData().add(new PieChart.Data(nombres[i] + " (" + porEstado[i] + ")", porEstado[i]));
        }
        pastel.setLegendVisible(true);
        pastel.setLabelsVisible(false);
        pastel.setPrefHeight(240);
        pastel.setMinHeight(240);
        if (totalEstados == 0) {
            graficaEstado.getChildren().setAll(vacio("Sin citas este mes."));
        } else {
            graficaEstado.getChildren().setAll(pastel);
        }

        XYChart.Series<String, Number> meses = new XYChart.Series<>();
        for (int i = 0; i < 6; i++) {
            YearMonth m = mesActual.minusMonths(5 - i);
            meses.getData().add(new XYChart.Data<>(PlanificadorAgenda.MESES_CORTO[m.getMonthValue() - 1], porMes[i]));
        }
        graficaMeses.getChildren().setAll(barras(meses));
    }

    private BarChart<String, Number> barras(XYChart.Series<String, Number> serie) {
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        y.setForceZeroInRange(true);
        y.setTickUnit(1);
        y.setTickLabelFormatter(new StringConverter<Number>() {
            @Override public String toString(Number n) { return n.doubleValue() % 1 == 0 ? String.valueOf(n.intValue()) : ""; }
            @Override public Number fromString(String s) { return null; }
        });
        BarChart<String, Number> g = new BarChart<>(new CategoryAxis(), y);
        g.getData().add(serie);
        g.setLegendVisible(false);
        g.setAnimated(false);
        g.setPrefHeight(240);
        g.setMinHeight(240);
        return g;
    }

    // ----------------------------------------------------------- Actividad

    private void renderActividad(HashMap<String, Paciente> pacientes) {
        actividad.getChildren().clear();
        List<Receta> recetas = new ArrayList<>(Receta.todas().values());
        recetas.sort(Comparator.comparing(Receta::getFecha, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparingInt(Receta::getIdReceta).reversed());
        if (recetas.isEmpty()) {
            actividad.getChildren().add(vacio("Aún no se han emitido recetas."));
            return;
        }
        for (Receta r : recetas.subList(0, Math.min(5, recetas.size()))) {
            Label fecha = new Label(r.getFecha() == null ? "--" : r.getFecha().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            fecha.getStyleClass().add("info-value");
            fecha.setMinWidth(90);
            Label nombre = new Label(Paciente.nombreParaMostrar(pacientes.get(r.getCurp()), r.getCurp()));
            nombre.getStyleClass().add("row-name");
            nombre.setMinWidth(220);
            String dx = r.getDiagnostico() == null || r.getDiagnostico().trim().isEmpty()
                    ? "Sin diagnóstico" : r.getDiagnostico().trim();
            Label diag = new Label(dx);
            diag.getStyleClass().add("hint");
            HBox fila = new HBox(12, fecha, nombre, diag);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("row-card");
            fila.setCursor(Cursor.HAND);
            fila.setOnMouseClicked(e -> abrirConsulta(r));
            actividad.getChildren().add(fila);
        }
    }

    // ------------------------------------------------------------- Helpers

    private static Label vacio(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("hint");
        l.setWrapText(true);
        return l;
    }

    private static String estiloEstado(Estado e) {
        if (e == Estado.ATENDIDA) return "chip-blue";
        if (e == Estado.CANCELADA) return "chip-blood";
        return "chip-amber";
    }
}
