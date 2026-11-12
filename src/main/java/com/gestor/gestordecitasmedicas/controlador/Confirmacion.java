package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.icons.FlaticonIcons;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Diálogo de confirmación dentro de la ventana (reemplaza los Alert CONFIRMATION). Bloquea como showAndWait()
 * con un bucle de eventos anidado, así que se usa igual: {@code if (!Confirmacion.destructiva(...)) return;}.
 */
final class Confirmacion {
    private Confirmacion() {
    }

    static boolean destructiva(String titulo, String mensaje, String accion) {
        return preguntar(titulo, mensaje, accion, true, null);
    }

    static boolean normal(String titulo, String mensaje, String accion) {
        return preguntar(titulo, mensaje, accion, false, null);
    }

    /** {@code extra} (opcional) va bajo el mensaje, p. ej. la lista de citas afectadas. */
    static boolean preguntar(String titulo, String mensaje, String accion, boolean destructiva, Node extra) {
        StackPane capa = Ui.capa();
        if (capa == null) return false;
        Scene scene = capa.getScene();
        Node focoPrevio = scene.getFocusOwner();
        Object llave = new Object();

        FontIcon icono = new FontIcon(destructiva ? FlaticonIcons.TRASH : FlaticonIcons.INFO);
        icono.setIconSize(16);
        StackPane tile = new StackPane(icono);
        tile.getStyleClass().addAll("icono-estado", destructiva ? "estado-error" : "estado-info");
        tile.setMinSize(36, 36);
        tile.setMaxSize(36, 36);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("confirm-titulo");
        lblTitulo.setWrapText(true);
        Label lblMensaje = new Label(mensaje);
        lblMensaje.getStyleClass().add("confirm-mensaje");
        lblMensaje.setWrapText(true);
        VBox textos = new VBox(4, lblTitulo, lblMensaje);
        if (extra != null) textos.getChildren().add(extra);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox arriba = new HBox(12, tile, textos);

        Button volver = new Button("Volver");
        volver.getStyleClass().add("btn-secondary");
        volver.setCancelButton(true);
        Button confirmar = new Button(accion);
        confirmar.getStyleClass().add(destructiva ? "btn-peligro" : "btn-primary");
        volver.setOnAction(e -> Platform.exitNestedEventLoop(llave, Boolean.FALSE));
        confirmar.setOnAction(e -> Platform.exitNestedEventLoop(llave, Boolean.TRUE));
        HBox acciones = new HBox(8, volver, confirmar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        VBox dialogo = new VBox(24, arriba, acciones);
        dialogo.getStyleClass().add("confirm-dialogo");
        dialogo.setPrefWidth(420);
        dialogo.setMaxSize(420, Region.USE_PREF_SIZE);
        dialogo.setAccessibleText(titulo + ". " + mensaje);

        StackPane velo = new StackPane(dialogo);
        velo.getStyleClass().add("confirm-velo");
        velo.setOnMouseClicked(e -> {
            if (e.getTarget() == velo) Platform.exitNestedEventLoop(llave, Boolean.FALSE);
        });
        velo.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                Platform.exitNestedEventLoop(llave, Boolean.FALSE);
            }
        });
        Button inicial = destructiva ? volver : confirmar;
        // Atrapa el foco: si Tab lo saca del diálogo, vuelve al botón inicial.
        ChangeListener<Node> trampa = (obs, antes, ahora) -> {
            if (ahora != null && !esDescendiente(ahora, velo)) Platform.runLater(inicial::requestFocus);
        };
        scene.focusOwnerProperty().addListener(trampa);

        capa.getChildren().add(velo);
        animarEntrada(velo, dialogo);
        inicial.requestFocus();
        Object respuesta = Platform.enterNestedEventLoop(llave);

        scene.focusOwnerProperty().removeListener(trampa);
        velo.setMouseTransparent(true);
        FadeTransition salida = new FadeTransition(Duration.millis(120), velo);
        salida.setToValue(0);
        salida.setOnFinished(e -> capa.getChildren().remove(velo));
        salida.play();
        if (focoPrevio != null) focoPrevio.requestFocus();
        return Boolean.TRUE.equals(respuesta);
    }

    private static boolean esDescendiente(Node nodo, Node ancestro) {
        for (Node n = nodo; n != null; n = n.getParent()) {
            if (n == ancestro) return true;
        }
        return false;
    }

    private static void animarEntrada(Node velo, Node dialogo) {
        velo.setOpacity(0);
        dialogo.setScaleX(0.96);
        dialogo.setScaleY(0.96);
        FadeTransition aparecer = new FadeTransition(Duration.millis(160), velo);
        aparecer.setToValue(1);
        ScaleTransition crecer = new ScaleTransition(Duration.millis(160), dialogo);
        crecer.setToX(1);
        crecer.setToY(1);
        new ParallelTransition(aparecer, crecer).play();
    }
}
