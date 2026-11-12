package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.PlanificadorAgenda;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.util.List;

/** Utilidades de interfaz compartidas por los controladores. */
final class Ui {
    private static final String ID_CAPA = "capa-superior";
    private static final String CSS = Ui.class.getResource("/com/gestor/gestordecitasmedicas/cita.css").toExternalForm();

    private Ui() {
    }

    /**
     * Capa superior de la ventana visible (toasts, panel de notificaciones, confirmaciones). La primera vez envuelve
     * la raíz de la escena en un StackPane; cambiar de raíz (login ↔ principal) la descarta y se vuelve a crear.
     */
    static StackPane capa() {
        Stage stage = ventana();
        if (stage == null || stage.getScene() == null) return null;
        Scene scene = stage.getScene();
        Parent raiz = scene.getRoot();
        if (raiz instanceof StackPane && ID_CAPA.equals(raiz.getId())) return (StackPane) raiz;
        Node foco = scene.getFocusOwner();
        StackPane capa = new StackPane();
        capa.setId(ID_CAPA);
        capa.getStylesheets().add(CSS);
        scene.setRoot(capa); // primero se suelta la raíz anterior: un nodo no puede ser raíz y a la vez hijo
        capa.getChildren().add(raiz);
        if (foco != null) foco.requestFocus(); // al re-colgar la raíz se pierde el foco del campo activo
        return capa;
    }

    /** La ventana enfocada o, si ninguna lo está, la primera visible. */
    private static Stage ventana() {
        Stage visible = null;
        for (Window w : Window.getWindows()) {
            if (!(w instanceof Stage) || !w.isShowing()) continue;
            if (w.isFocused()) return (Stage) w;
            if (visible == null) visible = (Stage) w;
        }
        return visible;
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
