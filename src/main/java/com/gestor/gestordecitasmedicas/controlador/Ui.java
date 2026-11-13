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
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Transform;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/** Utilidades de interfaz compartidas por los controladores. */
public final class Ui {
    private static final String ID_CAPA = "capa-superior";
    private static final String CSS = Ui.class.getResource("/com/gestor/gestordecitasmedicas/cita.css").toExternalForm();
    private static final Preferences PREFS = Preferences.userNodeForPackage(Ui.class);

    private Ui() {
    }

    /** Preferencia de modo oscuro (se guarda por usuario de Windows). */
    public static boolean oscuro() {
        return PREFS.getBoolean("modoOscuro", false);
    }

    static void setOscuro(boolean oscuro) {
        PREFS.putBoolean("modoOscuro", oscuro);
    }

    /** Guarda la preferencia y cambia de tema con un fundido: una foto del tema anterior se desvanece sobre el nuevo. */
    static void cambiarTema(boolean oscuro) {
        StackPane capa = capa(); // antes de guardar: crear la capa aplica el tema guardado y la foto saldría ya cambiada
        setOscuro(oscuro);
        if (capa == null) return;
        SnapshotParameters p = new SnapshotParameters();
        double escala = capa.getScene().getWindow().getOutputScaleX(); // sin esto la foto sale borrosa en pantallas al 125-150 %
        p.setTransform(Transform.scale(escala, escala));
        ImageView antes = new ImageView(capa.snapshot(p, null));
        antes.setFitWidth(capa.getWidth());
        antes.setFitHeight(capa.getHeight());
        antes.setMouseTransparent(true);
        tema(capa);
        capa.getChildren().add(antes);
        FadeTransition f = new FadeTransition(Duration.millis(280), antes);
        f.setToValue(0);
        f.setOnFinished(e -> capa.getChildren().remove(antes));
        f.play();
    }

    /**
     * Aplica el tema actual al nodo y a cada .root anidado: las vistas FXML llevan styleClass="root" propio y,
     * sin la clase, volverían a declarar los colores claros debajo de la capa superior.
     */
    public static void tema(Node raiz) {
        if (raiz == null) return;
        boolean oscuro = oscuro();
        List<Node> nodos = new ArrayList<>(raiz.lookupAll(".root"));
        nodos.add(raiz);
        for (Node n : nodos) {
            n.getStyleClass().remove("oscuro");
            if (oscuro) n.getStyleClass().add("oscuro");
        }
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
        tema(capa); // el listener de raíz ya corrió, pero sin la vista colgada todavía
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
