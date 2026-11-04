package com.gestor.gestordecitasmedicas;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;

public class AgendaController {
    @FXML private GridPane calendario, semana;
    @FXML private TextField fichaNombre;
    @FXML private Label fichaEdad, fichaFecha, fichaHora, mesLabel, rangoLabel, resumenLabel;

    private static final String[] DIAS_CAL = {"L", "M", "X", "J", "V", "S", "D"};
    private static final String[] NOMBRES_DIA = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final String[] NOMBRES_DIA_CORTO = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final String[] MESES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
    private static final String[] MESES_CORTO = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
    private static final String[] HORAS = {"09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "04:00 PM", "05:00 PM"};

    // Semana con los turnos de ejemplo (lunes). El resto de semanas se muestran vacías.
    private static final LocalDate SEMANA_DEMO = LocalDate.of(2024, 10, 21);

    private YearMonth mes = YearMonth.of(2024, 10);
    private LocalDate seleccion = LocalDate.of(2024, 10, 24);

    // TODO: reemplazar por las citas del modelo (Cita/Paciente) cuando exista el flujo de citas.
    // Una fila por hora, una columna por día. null = disponible.
    private static final class Turno {
        final String estado, paciente, motivo, edad;

        Turno(String estado, String paciente, String motivo, String edad) {
            this.estado = estado;
            this.paciente = paciente;
            this.motivo = motivo;
            this.edad = edad;
        }
    }

    private static Turno t(String estado, String paciente, String motivo, String edad) {
        return new Turno(estado, paciente, motivo, edad);
    }

    private static final Turno[][] CITAS = {
            {t("Completada", "Elena Navarro V.", "Control Hipertensión", "57"), t("Confirmada", "Roberto Gómez B.", "Primera Consulta", "34"), null, t("Confirmada", "Ana María Rios", "Lectura de Rayos X", "29"), null},
            {t("En espera", "Carlos A. Mendoza", "Triaje / Cita General", "42"), t("Confirmada", "Miriam Valenzuela", "Revisión Post-op", "61"), t("Confirmada", "Samuel Paredes L.", "Evaluación Rutina", "38"), null, t("Confirmada", "Lucía Fernández", "Entrega de Exámenes", "45")},
            {t("Confirmada", "Patricia Morales", "Dolor Articular", "50"), null, null, t("Confirmada", "Joaquín Delgado", "Consulta Preventiva", "33"), null},
            {null, t("Confirmada", "Daniela Castro E.", "Resultados Laboratorio", "27"), t("Reservado", "Comité Clínico", "Sesión Médica", "--"), null, t("Confirmada", "Guillermo Vega", "Control Rutinario", "68")},
            {t("Confirmada", "Marcos Villalba", "Revisión General", "41"), t("Confirmada", "Beatriz Sotomayor", "Consulta Alergias", "36"), null, null, null},
            {null, null, t("Confirmada", "Hernán Ortiz", "Evaluación Médica", "52"), null, t("Cierre", "Bloque Administrativo", "Cierre de Fichas", "--")},
    };

    @FXML
    private void initialize() {
        renderCalendario();
        renderSemana();
        // Turno en curso por defecto: Carlos A. Mendoza (Lun, 10:00 AM).
        mostrarCita(CITAS[1][0], fechaLarga(lunesSemana(seleccion)), HORAS[1]);
    }

    private void renderCalendario() {
        calendario.getChildren().clear();
        mesLabel.setText(MESES[mes.getMonthValue() - 1] + " " + mes.getYear());
        for (int c = 0; c < DIAS_CAL.length; c++) calendario.add(celdaCalendario(DIAS_CAL[c], "cal-head"), c, 0);

        LocalDate dia = mes.atDay(1).minusDays(mes.atDay(1).getDayOfWeek().getValue() - 1);
        for (int i = 0; i < 42; i++, dia = dia.plusDays(1)) {
            LocalDate fecha = dia;
            String estilo = fecha.equals(seleccion) ? "cal-sel"
                    : fecha.getMonthValue() != mes.getMonthValue() ? "cal-other" : "cal-day";
            Label celda = celdaCalendario(String.valueOf(fecha.getDayOfMonth()), estilo);
            celda.setCursor(Cursor.HAND);
            celda.setOnMouseClicked(e -> {
                seleccion = fecha;
                mes = YearMonth.from(fecha);
                renderCalendario();
                renderSemana();
            });
            calendario.add(celda, i % 7, 1 + i / 7);
        }
    }

    private void renderSemana() {
        LocalDate lunes = lunesSemana(seleccion);
        rangoLabel.setText("Semana del " + lunes.getDayOfMonth() + " al " + lunes.plusDays(4).getDayOfMonth()
                + " de " + MESES[lunes.getMonthValue() - 1] + ", " + lunes.getYear());

        semana.getChildren().clear();
        Label horario = new Label("HORARIO");
        horario.getStyleClass().add("caption");
        semana.add(horario, 0, 0);

        Turno[][] datos = esSemanaDemo(lunes) ? CITAS : new Turno[HORAS.length][5];

        for (int c = 0; c < 5; c++) {
            LocalDate dia = lunes.plusDays(c);
            Label l = new Label(NOMBRES_DIA[dia.getDayOfWeek().getValue() - 1] + "\n"
                    + dia.getDayOfMonth() + " " + MESES_CORTO[dia.getMonthValue() - 1]);
            l.getStyleClass().addAll("day-head", dia.equals(seleccion) ? "day-head-sel" : "day-head-off");
            l.setMaxWidth(Double.MAX_VALUE);
            semana.add(l, c + 1, 0);
        }
        for (int h = 0; h < HORAS.length; h++) {
            Label hora = new Label(HORAS[h]);
            hora.getStyleClass().add("info-value");
            semana.add(hora, 0, h + 1);
            for (int c = 0; c < 5; c++) {
                semana.add(celdaCita(datos[h][c], fechaLarga(lunes.plusDays(c)), HORAS[h]), c + 1, h + 1);
            }
        }
        resumenLabel.setText(resumen(datos, lunes));
    }

    private String resumen(Turno[][] datos, LocalDate lunes) {
        int finalizadas = 0, espera = 0, confirmadas = 0, libres = 0;
        for (Turno[] fila : datos) {
            Turno t = fila[0];
            if (t == null) libres++;
            else if ("Completada".equals(t.estado)) finalizadas++;
            else if ("En espera".equals(t.estado)) espera++;
            else confirmadas++;
        }
        return "Resumen " + NOMBRES_DIA_CORTO[lunes.getDayOfWeek().getValue() - 1] + " " + lunes.getDayOfMonth()
                + ": " + finalizadas + " finalizada · " + espera + " en espera · " + confirmadas
                + " confirmadas · " + libres + " libres";
    }

    @FXML
    private void onMesAnterior() {
        mes = mes.minusMonths(1);
        renderCalendario();
    }

    @FXML
    private void onMesSiguiente() {
        mes = mes.plusMonths(1);
        renderCalendario();
    }

    @FXML
    private void onSemanaAnterior() {
        moverSemana(-1);
    }

    @FXML
    private void onSemanaSiguiente() {
        moverSemana(1);
    }

    private void moverSemana(int semanas) {
        seleccion = seleccion.plusWeeks(semanas);
        mes = YearMonth.from(seleccion);
        renderCalendario();
        renderSemana();
    }

    private static LocalDate lunesSemana(LocalDate d) {
        return d.minusDays(d.getDayOfWeek().getValue() - 1);
    }

    private static boolean esSemanaDemo(LocalDate lunes) {
        return lunes.equals(SEMANA_DEMO);
    }

    private static String fechaLarga(LocalDate d) {
        return NOMBRES_DIA_CORTO[d.getDayOfWeek().getValue() - 1] + ", " + d.getDayOfMonth() + " "
                + MESES_CORTO[d.getMonthValue() - 1] + " " + d.getYear();
    }

    private Label celdaCalendario(String texto, String estilo) {
        Label l = new Label(texto);
        l.getStyleClass().add(estilo);
        l.setAlignment(Pos.CENTER);
        l.setMinSize(32, 32);
        return l;
    }

    private Node celdaCita(Turno turno, String fecha, String hora) {
        if (turno == null) {
            Button libre = new Button("+ Disponible");
            libre.getStyleClass().add("slot-free");
            libre.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            libre.setOnAction(e -> crearCita());
            return libre;
        }
        Label estado = new Label(turno.estado.toUpperCase());
        estado.getStyleClass().add("caption");
        Label paciente = new Label(turno.paciente);
        paciente.getStyleClass().add("info-value");
        Label motivo = new Label(turno.motivo);
        motivo.getStyleClass().add("hint");
        VBox contenido = new VBox(2, estado, paciente, motivo);
        contenido.setMaxWidth(Double.MAX_VALUE);

        Button celda = new Button();
        celda.setGraphic(contenido);
        celda.getStyleClass().addAll("slot", "slot-" + turno.estado.toLowerCase().replace(' ', '-'));
        celda.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        celda.setOnAction(e -> mostrarCita(turno, fecha, hora));
        return celda;
    }

    private void mostrarCita(Turno turno, String fecha, String hora) {
        fichaNombre.setText(turno.paciente);
        fichaEdad.setText("Edad: " + turno.edad + " años");
        fichaFecha.setText(fecha);
        fichaHora.setText(hora + " (45 min)");
    }

    // TODO: redirigir a la ventana de crear cita cuando esté implementada.
    private void crearCita() {
        new Alert(Alert.AlertType.INFORMATION, "Crear cita: falta implementación.").show();
    }
}
