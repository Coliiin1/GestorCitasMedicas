package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.controlador.Notificador.Notificacion;
import com.gestor.gestordecitasmedicas.icons.FlaticonIcons;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Campana con contador de no leídas y el panel con el historial de avisos de la sesión. */
final class PanelNotificaciones {
    private static final double ANCHO = 360;

    private static Button campana;
    private static Pane abierto;
    private static ListChangeListener<Notificacion> alCambiar;

    private PanelNotificaciones() {
    }

    /** Convierte {@code boton} en la campana de la ventana principal. */
    static void instalar(Button boton) {
        campana = boton;
        Label contador = new Label();
        contador.getStyleClass().add("campana-contador");
        contador.textProperty().bind(Bindings.createStringBinding(
                () -> Notificador.noLeidas.get() > 9 ? "9+" : String.valueOf(Notificador.noLeidas.get()),
                Notificador.noLeidas));
        contador.visibleProperty().bind(Notificador.noLeidas.greaterThan(0));
        contador.setTranslateX(9);
        contador.setTranslateY(-9);
        StackPane.setAlignment(contador, Pos.TOP_RIGHT);
        StackPane grafico = new StackPane(new FontIcon(FlaticonIcons.BELL), contador);
        boton.setGraphic(grafico);
        boton.accessibleTextProperty().bind(Bindings.createStringBinding(
                () -> "Notificaciones, " + Notificador.noLeidas.get() + " sin leer", Notificador.noLeidas));
        boton.setOnAction(e -> {
            if (abierto != null) cerrar();
            else abrir();
        });
    }

    /** Abre el panel bajo la campana; false si no hay campana visible (p. ej. en el login). */
    static boolean abrir() {
        if (abierto != null) return true;
        if (campana == null || campana.getScene() == null) return false;
        StackPane capa = Ui.capa();
        if (capa == null || capa.getScene() != campana.getScene()) return false;

        VBox lista = new VBox();
        llenar(lista);
        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("notif-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label titulo = new Label("Notificaciones");
        titulo.getStyleClass().add("notif-titulo");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        Button marcar = accion("Marcar todo como leído", () -> {
            Notificador.marcarTodoLeido();
            llenar(lista);
        });
        HBox cabecera = new HBox(titulo, hueco, marcar);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("notif-cabecera");

        HBox pie = new HBox(accion("Limpiar historial", Notificador::limpiar));
        pie.setAlignment(Pos.CENTER_RIGHT);
        pie.getStyleClass().add("notif-pie");

        VBox panel = new VBox(cabecera, scroll, pie);
        panel.getStyleClass().add("notif-panel");
        panel.setPrefWidth(ANCHO);
        panel.setMaxWidth(ANCHO);
        panel.setMaxHeight(480);

        Bounds b = campana.localToScene(campana.getBoundsInLocal());
        double x = Math.max(16, Math.min(b.getMinX(), capa.getWidth() - ANCHO - 16));
        panel.relocate(x, b.getMaxY() + 8);

        // Fondo transparente a pantalla completa: un clic fuera del panel lo cierra.
        Pane fondo = new Pane(panel);
        fondo.setOnMouseClicked(e -> {
            if (e.getTarget() == fondo) cerrar();
        });
        fondo.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                cerrar();
                e.consume();
            }
        });
        alCambiar = c -> llenar(lista);
        Notificador.historial.addListener(alCambiar);
        capa.getChildren().add(fondo);
        abierto = fondo;
        campana.getStyleClass().add("campana-abierta");
        marcar.requestFocus();
        return true;
    }

    /** Cierra el panel; lo que se vio queda leído. */
    static void cerrar() {
        if (abierto == null) return;
        Notificador.historial.removeListener(alCambiar);
        if (abierto.getParent() instanceof Pane) ((Pane) abierto.getParent()).getChildren().remove(abierto);
        abierto = null;
        Notificador.marcarTodoLeido();
        campana.getStyleClass().remove("campana-abierta");
        campana.requestFocus();
    }

    private static void llenar(VBox lista) {
        lista.getChildren().clear();
        if (Notificador.historial.isEmpty()) {
            FontIcon icono = new FontIcon(FlaticonIcons.BELL);
            icono.getStyleClass().add("notif-vacio-icono");
            Label texto = new Label("Sin notificaciones");
            texto.getStyleClass().add("notif-mensaje");
            VBox vacio = new VBox(8, icono, texto);
            vacio.setAlignment(Pos.CENTER);
            vacio.getStyleClass().add("notif-vacio");
            lista.getChildren().add(vacio);
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        Boolean grupoActual = null;
        for (Notificacion n : Notificador.historial) {
            boolean deHoy = Notificador.esDeHoy(n, hoy);
            if (grupoActual == null || grupoActual != deHoy) {
                Label grupo = new Label(deHoy ? "HOY" : "ANTERIORES");
                grupo.getStyleClass().add("notif-grupo");
                lista.getChildren().add(grupo);
                grupoActual = deHoy;
            }
            lista.getChildren().add(fila(n, ahora));
        }
    }

    private static HBox fila(Notificacion n, LocalDateTime ahora) {
        Label titulo = new Label(n.titulo);
        titulo.getStyleClass().add("notif-fila-titulo");
        titulo.setWrapText(true);
        Label mensaje = new Label(n.mensaje);
        mensaje.getStyleClass().add("notif-mensaje");
        mensaje.setWrapText(true);
        mensaje.setVisible(!n.mensaje.isEmpty());
        mensaje.setManaged(!n.mensaje.isEmpty());
        Label hora = new Label(Notificador.horaRelativa(n.hora, ahora));
        hora.getStyleClass().add("notif-hora");
        VBox textos = new VBox(2, titulo, mensaje, hora);
        HBox.setHgrow(textos, Priority.ALWAYS);
        textos.setMinWidth(0);

        HBox fila = new HBox(12, Notificador.tile(n.tipo, 28, 14), textos);
        fila.getStyleClass().addAll("notif-fila", n.tipo.estilo);
        String estado = "";
        if (!n.leida) {
            Circle punto = new Circle(4);
            punto.getStyleClass().add("notif-punto");
            fila.getChildren().add(punto);
            fila.getStyleClass().add("notif-fila-nueva");
            estado = ", sin leer";
        }
        fila.setAccessibleText(n.titulo + ". " + n.mensaje + ". " + hora.getText() + estado);
        return fila;
    }

    private static Button accion(String texto, Runnable alPulsar) {
        Button b = new Button(texto);
        b.getStyleClass().add("notif-accion");
        b.setOnAction(e -> alPulsar.run());
        return b;
    }
}
