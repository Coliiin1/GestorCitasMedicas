package com.gestor.gestordecitasmedicas;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;

public class AgendaController {
    @FXML private GridPane calendario, semana;

    private static final String[] DIAS = {"Lunes\n24 Oct", "Martes\n25 Oct", "Miércoles\n26 Oct", "Jueves\n27 Oct", "Viernes\n28 Oct"};
    private static final String[] HORAS = {"09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "04:00 PM", "05:00 PM"};
    // TODO: reemplazar por las citas del modelo (Cita) cuando exista el flujo de citas.
    // Una fila por hora, una columna por día. Formato "estado|paciente|motivo"; null = disponible.
    private static final String[][] CITAS = {
            {"Completada|Elena Navarro V.|Control Hipertensión", "Confirmada|Roberto Gómez B.|Primera Consulta", null, "Confirmada|Ana María Rios|Lectura de Rayos X", null},
            {"En espera|Carlos A. Mendoza|Triaje / Cita General", "Confirmada|Miriam Valenzuela|Revisión Post-op", "Confirmada|Samuel Paredes L.|Evaluación Rutina", null, "Confirmada|Lucía Fernández|Entrega de Exámenes"},
            {"Confirmada|Patricia Morales|Dolor Articular", null, null, "Confirmada|Joaquín Delgado|Consulta Preventiva", null},
            {null, "Confirmada|Daniela Castro E.|Resultados Laboratorio", "Reservado|Comité Clínico|Sesión Médica", null, "Confirmada|Guillermo Vega|Control Rutinario"},
            {"Confirmada|Marcos Villalba|Revisión General", "Confirmada|Beatriz Sotomayor|Consulta Alergias", null, null, null},
            {null, null, "Confirmada|Hernán Ortiz|Evaluación Médica", null, "Cierre|Bloque Administrativo|Cierre de Fichas"},
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
            for (int c = 0; c < DIAS.length; c++) semana.add(celdaCita(CITAS[h][c]), c + 1, h + 1);
        }
    }

    private Label celdaCalendario(String texto, String estilo) {
        Label l = new Label(texto);
        l.getStyleClass().add(estilo);
        l.setAlignment(Pos.CENTER);
        l.setMinSize(30, 26);
        return l;
    }

    private Node celdaCita(String cita) {
        if (cita == null) {
            Label libre = new Label("+ Disponible");
            libre.getStyleClass().add("slot-free");
            libre.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            return libre;
        }
        String[] p = cita.split("\\|");
        Label estado = new Label(p[0].toUpperCase());
        estado.getStyleClass().add("caption");
        Label paciente = new Label(p[1]);
        paciente.getStyleClass().add("info-value");
        Label motivo = new Label(p[2]);
        motivo.getStyleClass().add("hint");
        VBox v = new VBox(2, estado, paciente, motivo);
        v.getStyleClass().addAll("slot", "slot-" + p[0].toLowerCase().replace(' ', '-'));
        return v;
    }
}
