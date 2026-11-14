package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.Personal;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.io.UncheckedIOException;

public class LoginController {
    @FXML private StackPane hero;
    @FXML private StackPane carrusel;
    @FXML private ImageView logo;
    @FXML private HBox puntos;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField password;
    @FXML private TextField passwordVisible;
    @FXML private Button btnTogglePassword;

    private boolean visible;

    @FXML
    private void initialize() {
        // Recorta la foto a las esquinas redondeadas (arco = 2 × radio de 12).
        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(hero.widthProperty());
        clip.heightProperty().bind(hero.heightProperty());
        hero.setClip(clip);
        logo.setImage(new Image(getClass().getResource("/Icon/Icono.png").toExternalForm()));
        Rectangle redondeoLogo = new Rectangle(36, 36);
        redondeoLogo.setArcWidth(16);
        redondeoLogo.setArcHeight(16);
        logo.setClip(redondeoLogo);
        passwordVisible.textProperty().bindBidirectional(password.textProperty());
        iniciarCarrusel();
    }

    private static final int FOTOS = 5;

    /** Carrusel a panel completo: cada foto cubre el panel y se funde con la siguiente cada 4 s. */
    private void iniciarCarrusel() {
        ImageView[] fotos = new ImageView[FOTOS];
        for (int i = 0; i < FOTOS; i++) {
            ImageView v = new ImageView(new Image(getClass().getResource("/Imagenes/carrusel/" + (i + 1) + ".jpg").toExternalForm()));
            v.setManaged(false); // su tamaño sigue al panel, no al revés
            fotos[i] = v;
            Region punto = new Region();
            punto.getStyleClass().add("login-punto");
            puntos.getChildren().add(punto);
        }
        carrusel.getChildren().setAll(fotos);

        DoubleProperty pos = new SimpleDoubleProperty();
        Runnable colocar = () -> {
            int activa = Math.floorMod(Math.round(pos.get()), FOTOS);
            for (int i = 0; i < FOTOS; i++) {
                java.util.List<String> clases = puntos.getChildren().get(i).getStyleClass();
                // Solo toca las clases al cambiar de foto, no en cada cuadro de la animación.
                if ((i == activa) != clases.contains("login-punto-activo")) {
                    if (i == activa) clases.add("login-punto-activo"); else clases.remove("login-punto-activo");
                }
            }
            double w = hero.getWidth(), h = hero.getHeight();
            if (w <= 0 || h <= 0) return;
            for (int i = 0; i < FOTOS; i++) {
                double d = (((i - pos.get()) % FOTOS) + FOTOS) % FOTOS;
                if (d > FOTOS / 2.0) d -= FOTOS;
                // Recorte tipo "cover" a la proporción actual del panel.
                Image img = fotos[i].getImage();
                double vw = Math.min(img.getWidth(), img.getHeight() * w / h);
                double vh = vw * h / w;
                fotos[i].setViewport(new Rectangle2D((img.getWidth() - vw) / 2, (img.getHeight() - vh) / 2, vw, vh));
                fotos[i].setFitWidth(w);
                fotos[i].setFitHeight(h);
                fotos[i].setOpacity(Math.max(0, 1 - Math.abs(d)));
            }
        };
        pos.addListener((o, a, b) -> colocar.run());
        hero.widthProperty().addListener((o, a, b) -> colocar.run());
        hero.heightProperty().addListener((o, a, b) -> colocar.run());
        colocar.run();

        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(4), e ->
                new Timeline(new KeyFrame(Duration.millis(800),
                        new KeyValue(pos, Math.round(pos.get()) + 1, Interpolator.EASE_BOTH))).play()));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
        // Al pasar al panel principal el login se descarta: se detiene el reloj.
        hero.sceneProperty().addListener((o, a, s) -> { if (s == null) reloj.stop(); });
    }

    @FXML
    private void onTogglePassword() {
        visible = !visible;
        password.setVisible(!visible);
        passwordVisible.setVisible(visible);
        ((FontIcon) btnTogglePassword.getGraphic())
                .setIconLiteral(visible ? "fi-rr-eye-crossed" : "fi-rr-eye");
        TextField foco = visible ? passwordVisible : password;
        foco.requestFocus();
        foco.positionCaret(foco.getLength());
    }

    @FXML
    private void onIngresar() {
        String idUsuario = txtUsuario.getText().trim().toUpperCase(java.util.Locale.ROOT);
        String rol = Personal.autenticar(idUsuario, password.getText());
        if (rol == null) {
            password.clear();
            Ui.sacudir(password.getParent());
            Notificador.advertencia("Acceso denegado", "Usuario o contraseña incorrectos.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gestor/gestordecitasmedicas/main-view.fxml"));
            Parent raiz = loader.load();
            ((MainController) loader.getController()).configurarRol(rol, idUsuario);
            txtUsuario.getScene().setRoot(raiz);
            Ui.entrada(raiz, 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
