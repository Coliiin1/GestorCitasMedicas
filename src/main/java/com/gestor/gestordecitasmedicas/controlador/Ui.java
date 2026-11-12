package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.PlanificadorAgenda;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.Duration;
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

    /** Entrada suave: el nodo aparece y sube 10 px. {@code retrasoMs} permite escalonar varios nodos. */
    static void entrada(Node n, int retrasoMs) {
        n.setOpacity(0);
        FadeTransition aparecer = new FadeTransition(Duration.millis(240), n);
        aparecer.setToValue(1);
        TranslateTransition subir = new TranslateTransition(Duration.millis(240), n);
        subir.setFromY(10);
        subir.setToY(0);
        subir.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition p = new ParallelTransition(aparecer, subir);
        p.setDelay(Duration.millis(retrasoMs));
        p.play();
    }

    /** Cuenta de 0 a {@code valor} en la etiqueta (indicadores del dashboard). */
    static void contar(Label l, int valor) {
        IntegerProperty n = new SimpleIntegerProperty();
        l.setText("0");
        n.addListener((o, a, b) -> l.setText(String.valueOf(b.intValue())));
        new Timeline(new KeyFrame(Duration.millis(600), new KeyValue(n, valor, Interpolator.EASE_OUT))).play();
    }

    /** Sacudida horizontal breve para señalar un dato rechazado. */
    static void sacudir(Node n) {
        TranslateTransition t = new TranslateTransition(Duration.millis(50), n);
        t.setFromX(-6);
        t.setToX(6);
        t.setCycleCount(6);
        t.setAutoReverse(true);
        t.setOnFinished(e -> n.setTranslateX(0));
        t.play();
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
