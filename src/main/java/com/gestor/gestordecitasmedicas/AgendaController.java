package com.gestor.gestordecitasmedicas;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
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
    @FXML private Label fichaEdad, fichaFecha, fichaHora;

    private static final String[] DIAS = {"Lunes\n24 Oct", "Martes\n25 Oct", "Miércoles\n26 Oct", "Jueves\n27 Oct", "Viernes\n28 Oct"};
    private static final String[] FECHAS = {"Lun, 24 Oct 2024", "Mar, 25 Oct 2024", "Mié, 26 Oct 2024", "Jue, 27 Oct 2024", "Vie, 28 Oct 2024"};
    private static final String[] HORAS = {"09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "04:00 PM", "05:00 PM"};

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
        LocalDate dia = YearMonth.of(2024, 10).atDay(1);
        dia = dia.minusDays(dia.getDayOfWeek().getValue() - 1);
        for (String d : "L M X J V S D".split(" ")) calendario.add(celdaCalendario(d, "cal-head"), calendario.getChildren().size() % 7, 0);
        for (int i = 0; i < 42; i++, dia = dia.plusDays(1)) {
            String estilo = dia.getMonthValue() != 10 ? "cal-other" : dia.getDayOfMonth() == 24 ? "cal-sel" : "cal-day";
            calendario.add(celdaCalendario(String.valueOf(dia.getDayOfMonth()), estilo), i % 7, 1 + i / 7);
        }

        Label horario = new Label("HORARIO");
        horario.getStyleClass().add("caption");
        semana.add(horario, 0, 0);
        for (int c = 0; c < DIAS.length; c++) {
            Label l = new Label(DIAS[c]);
            l.getStyleClass().addAll("day-head", c == 0 ? "day-head-sel" : "day-head-off");
            l.setMaxWidth(Double.MAX_VALUE);
            semana.add(l, c + 1, 0);
        }
        for (int h = 0; h < HORAS.length; h++) {
            Label hora = new Label(HORAS[h]);
            hora.getStyleClass().add("info-value");
            semana.add(hora, 0, h + 1);
            for (int c = 0; c < DIAS.length; c++) semana.add(celdaCita(CITAS[h][c], FECHAS[c], HORAS[h]), c + 1, h + 1);
        }

        // Turno en curso por defecto: Carlos A. Mendoza (Lun 24, 10:00 AM).
        mostrarCita(CITAS[1][0], FECHAS[0], HORAS[1]);
    }

    private Label celdaCalendario(String texto, String estilo) {
        Label l = new Label(texto);
        l.getStyleClass().add(estilo);
        l.setAlignment(Pos.CENTER);
        l.setMinSize(28, 26);
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
