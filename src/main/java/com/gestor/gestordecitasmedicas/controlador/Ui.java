package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.PlanificadorAgenda;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.util.List;

/** Utilidades de interfaz compartidas por los controladores. */
final class Ui {
    private Ui() {
    }

    static void aviso(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    /** ComboBox de horas con formato de la agenda; selecciona {@code seleccionada} si está en la lista. */
    static ComboBox<LocalTime> comboHora(List<LocalTime> horas, LocalTime seleccionada) {
        ComboBox<LocalTime> combo = new ComboBox<>();
        combo.getItems().setAll(horas);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add("combo-field");
        combo.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime hora) {
                return PlanificadorAgenda.formatoHora(hora);
            }

            @Override
            public LocalTime fromString(String texto) {
                return null;
            }
        });
        if (seleccionada != null && horas.contains(seleccionada)) {
            combo.getSelectionModel().select(seleccionada);
        }
        return combo;
    }
}
