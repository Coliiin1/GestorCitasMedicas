package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.icons.FlaticonIcons;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Avisos al usuario: un toast que entra por la izquierda, suena y se borra solo, y queda en el historial que muestra
 * {@link PanelNotificaciones}. Reemplaza a los Alert que no piden decisión (para decidir: {@link Confirmacion}).
 */
final class Notificador {

    enum Tipo {
        EXITO("estado-exito", FlaticonIcons.CHECK, 4),
        INFO("estado-info", FlaticonIcons.INFO, 4),
        ADVERTENCIA("estado-advertencia", FlaticonIcons.TRIANGLE_WARNING, 6),
        ERROR("estado-error", FlaticonIcons.CROSS, 8);

        final String estilo;
        final FlaticonIcons icono;
        final int segundos;

        Tipo(String estilo, FlaticonIcons icono, int segundos) {
            this.estilo = estilo;
            this.icono = icono;
            this.segundos = segundos;
        }
    }

    static final class Notificacion {
        final Tipo tipo;
        final String titulo;
        final String mensaje;
        final LocalDateTime hora;
        boolean leida;

        Notificacion(Tipo tipo, String titulo, String mensaje, LocalDateTime hora) {
            this.tipo = tipo;
            this.titulo = titulo;
            this.mensaje = mensaje;
            this.hora = hora;
        }
    }

    static final int MAX_HISTORIAL = 50;
    private static final int MAX_VISIBLES = 3;
    private static final double ANCHO = 340;
    private static final String ID_PILA = "pila-toasts";

    /** Más reciente primero. */
    static final ObservableList<Notificacion> historial = FXCollections.observableArrayList();
    static final IntegerProperty noLeidas = new SimpleIntegerProperty();

    private Notificador() {
    }

    static void exito(String titulo, String mensaje) {
        mostrar(Tipo.EXITO, titulo, mensaje);
    }

    static void info(String titulo, String mensaje) {
        mostrar(Tipo.INFO, titulo, mensaje);
    }

    static void advertencia(String titulo, String mensaje) {
        mostrar(Tipo.ADVERTENCIA, titulo, mensaje);
    }

    static void error(String titulo, String mensaje) {
        mostrar(Tipo.ERROR, titulo, mensaje);
    }

    static void mostrar(Tipo tipo, String titulo, String mensaje) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> mostrar(tipo, titulo, mensaje));
            return;
        }
        Notificacion n = registrar(tipo, titulo, mensaje, LocalDateTime.now());
        StackPane capa = Ui.capa();
        if (capa == null) return;
        VBox pila = pila(capa);
        StackPane toast = toast(n);
        pila.getChildren().add(toast);
        while (pila.getChildren().size() > MAX_VISIBLES) cerrar(pila.getChildren().get(0), pila);
        animarEntrada(toast);
        Sonido.reproducir(tipo == Tipo.ERROR ? 2 : 1);
    }

    /** Agrega al historial (sin UI); separado para poder probarlo. */
    static Notificacion registrar(Tipo tipo, String titulo, String mensaje, LocalDateTime hora) {
        Notificacion n = new Notificacion(tipo, titulo, mensaje == null ? "" : mensaje, hora);
        historial.add(0, n);
        if (historial.size() > MAX_HISTORIAL) historial.remove(MAX_HISTORIAL, historial.size());
        recontar();
        return n;
    }

    static void marcarTodoLeido() {
        for (Notificacion n : historial) n.leida = true;
        recontar();
    }

    static void limpiar() {
        historial.clear();
        recontar();
    }

    private static void recontar() {
        int c = 0;
        for (Notificacion n : historial) if (!n.leida) c++;
        noLeidas.set(c);
    }

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEE d MMM", new Locale("es", "MX"));

    /** "Ahora", "Hace N min" la primera hora, luego la hora del día y en días anteriores la fecha corta. */
    static String horaRelativa(LocalDateTime hora, LocalDateTime ahora) {
        long minutos = java.time.Duration.between(hora, ahora).toMinutes();
        if (minutos < 1) return "Ahora";
        if (minutos < 60) return "Hace " + minutos + " min";
        if (hora.toLocalDate().equals(ahora.toLocalDate())) return HORA.format(hora);
        return DIA.format(hora);
    }

    static boolean esDeHoy(Notificacion n, LocalDate hoy) {
        return n.hora.toLocalDate().equals(hoy);
    }

    /** Tile cuadrado con el icono del tipo; lo comparten el toast y las filas del panel. */
    static StackPane tile(Tipo tipo, double lado, int tamIcono) {
        FontIcon icono = new FontIcon(tipo.icono);
        icono.setIconSize(tamIcono);
        StackPane tile = new StackPane(icono);
        tile.getStyleClass().add("icono-estado");
        tile.setMinSize(lado, lado);
        tile.setMaxSize(lado, lado);
        return tile;
    }

    private static VBox pila(StackPane capa) {
        for (Node n : capa.getChildren()) {
            if (ID_PILA.equals(n.getId())) return (VBox) n;
        }
        VBox pila = new VBox(8);
        pila.setId(ID_PILA);
        pila.setAlignment(Pos.BOTTOM_LEFT);
        pila.setPickOnBounds(false); // los clics fuera de los toasts llegan a la ventana
        pila.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(pila, Pos.BOTTOM_LEFT);
        StackPane.setMargin(pila, new Insets(16));
        capa.getChildren().add(pila);
        return pila;
    }

    private static StackPane toast(Notificacion n) {
        Label titulo = new Label(n.titulo);
        titulo.getStyleClass().add("toast-titulo");
        titulo.setWrapText(true);
        Label mensaje = new Label(n.mensaje);
        mensaje.getStyleClass().add("toast-mensaje");
        mensaje.setWrapText(true);
        mensaje.setVisible(!n.mensaje.isEmpty());
        mensaje.setManaged(!n.mensaje.isEmpty());
        VBox textos = new VBox(4, titulo, mensaje);
        HBox cuerpo = new HBox(12, tile(n.tipo, 32, 16), textos);
        cuerpo.getStyleClass().add("toast-cuerpo");

        FontIcon x = new FontIcon(FlaticonIcons.CROSS_SMALL);
        Button cerrar = new Button(null, x);
        cerrar.getStyleClass().add("toast-cerrar");
        cerrar.setAccessibleText("Cerrar aviso");
        StackPane.setAlignment(cerrar, Pos.TOP_RIGHT);
        StackPane.setMargin(cerrar, new Insets(8));

        Region progreso = new Region();
        progreso.getStyleClass().add("toast-progreso");
        progreso.setMaxWidth(ANCHO);
        StackPane.setAlignment(progreso, Pos.BOTTOM_LEFT);

        // El recorte redondeado va en un nodo interior: en el exterior también recortaría la sombra.
        StackPane interior = new StackPane(cuerpo, cerrar, progreso);
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(24);
        recorte.setArcHeight(24);
        recorte.widthProperty().bind(interior.widthProperty());
        recorte.heightProperty().bind(interior.heightProperty());
        interior.setClip(recorte);

        StackPane toast = new StackPane(interior);
        toast.getStyleClass().addAll("toast", n.tipo.estilo);
        toast.setPrefWidth(ANCHO);
        toast.setMaxWidth(ANCHO);
        toast.setAccessibleText(n.titulo + ". " + n.mensaje);

        Timeline tiempo = new Timeline(new KeyFrame(Duration.seconds(n.tipo.segundos),
                new KeyValue(progreso.maxWidthProperty(), 0, Interpolator.LINEAR)));
        tiempo.setOnFinished(e -> cerrar(toast, (VBox) toast.getParent()));
        toast.setOnMouseEntered(e -> tiempo.pause());
        toast.setOnMouseExited(e -> tiempo.play());
        toast.getProperties().put("tiempo", tiempo);
        cerrar.setOnAction(e -> cerrar(toast, (VBox) toast.getParent()));
        toast.setOnMouseClicked(e -> {
            if (e.getTarget() != cerrar && PanelNotificaciones.abrir()) cerrar(toast, (VBox) toast.getParent());
        });
        tiempo.play();
        return toast;
    }

    private static void animarEntrada(Node toast) {
        toast.setTranslateX(-ANCHO * 1.1 - 16);
        toast.setOpacity(0);
        TranslateTransition mover = new TranslateTransition(Duration.millis(260), toast);
        mover.setToX(0);
        mover.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition aparecer = new FadeTransition(Duration.millis(260), toast);
        aparecer.setToValue(1);
        new ParallelTransition(mover, aparecer).play();
    }

    private static void cerrar(Node toast, VBox pila) {
        if (pila == null || toast.getProperties().containsKey("cerrando")) return;
        toast.getProperties().put("cerrando", true);
        Object tiempo = toast.getProperties().get("tiempo");
        if (tiempo instanceof Timeline) ((Timeline) tiempo).stop();
        toast.setMouseTransparent(true);
        TranslateTransition mover = new TranslateTransition(Duration.millis(200), toast);
        mover.setToX(-ANCHO * 1.1 - 16);
        mover.setInterpolator(Interpolator.EASE_IN);
        FadeTransition desvanecer = new FadeTransition(Duration.millis(200), toast);
        desvanecer.setToValue(0);
        ParallelTransition salida = new ParallelTransition(mover, desvanecer);
        salida.setOnFinished(e -> pila.getChildren().remove(toast));
        salida.play();
    }

    /** Tono corto generado en memoria (sin archivo ni dependencia de javafx-media). */
    static final class Sonido {
        private static final float MUESTREO = 44100f;
        private static final ExecutorService AUDIO = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "sonido-notificacion");
            t.setDaemon(true);
            return t;
        });
        private static long ultimo;

        private Sonido() {
        }

        static void reproducir(int pulsos) {
            long ahora = System.currentTimeMillis();
            if (ahora - ultimo < 1000) return; // ráfaga de avisos: suena solo el primero
            ultimo = ahora;
            AUDIO.execute(() -> {
                byte[] pcm = tono(pulsos);
                AudioFormat formato = new AudioFormat(MUESTREO, 16, 1, true, false);
                try (SourceDataLine linea = AudioSystem.getSourceDataLine(formato)) {
                    linea.open(formato);
                    linea.start();
                    linea.write(pcm, 0, pcm.length);
                    linea.drain();
                } catch (Exception sinAudio) {
                    // Sin dispositivo de audio: el aviso visual basta.
                }
            });
        }

        /** PCM 16 bits mono: cada pulso 150 ms (880 Hz + armónico, ataque 10 ms y caída exponencial) y 30 ms de silencio. */
        static byte[] tono(int pulsos) {
            int porPulso = (int) (MUESTREO * 0.18);
            int sonando = (int) (MUESTREO * 0.15);
            int ataque = (int) (MUESTREO * 0.01);
            byte[] pcm = new byte[porPulso * pulsos * 2];
            for (int p = 0; p < pulsos; p++) {
                for (int i = 0; i < sonando; i++) {
                    double t = i / MUESTREO;
                    double envolvente = i < ataque ? (double) i / ataque : Math.exp(-(i - ataque) / (MUESTREO * 0.035));
                    double onda = Math.sin(2 * Math.PI * 880 * t) + 0.3 * Math.sin(2 * Math.PI * 1760 * t);
                    short muestra = (short) (onda / 1.3 * envolvente * 0.35 * Short.MAX_VALUE);
                    int k = (p * porPulso + i) * 2;
                    pcm[k] = (byte) muestra;
                    pcm[k + 1] = (byte) (muestra >> 8);
                }
            }
            return pcm;
        }
    }
}
