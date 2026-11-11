package com.gestor.gestordecitasmedicas;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.io.UncheckedIOException;

public class LoginController {
    @FXML private StackPane hero;
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
        passwordVisible.textProperty().bindBidirectional(password.textProperty());
        iniciarCarrusel();
    }

    private static final int FOTOS = 5;
    private static final double ANCHO = 380, ALTO = 300, HUECO = 12;

    /** Carrusel vertical: la foto central es la mayor y las vecinas se achican; avanza solo cada 3 s. */
    private void iniciarCarrusel() {
        ImageView[] fotos = new ImageView[FOTOS];
        for (int i = 0; i < FOTOS; i++) {
            Image img = new Image(getClass().getResource("/Imagenes/carrusel/" + (i + 1) + ".jpg").toExternalForm());
            ImageView v = new ImageView(img);
            // Recorte tipo "cover" al formato de la tarjeta.
            double w = Math.min(img.getWidth(), img.getHeight() * ANCHO / ALTO);
            double h = w * ALTO / ANCHO;
            v.setViewport(new Rectangle2D((img.getWidth() - w) / 2, (img.getHeight() - h) / 2, w, h));
            v.setFitWidth(ANCHO);
            v.setFitHeight(ALTO);
            Rectangle redondeo = new Rectangle(ANCHO, ALTO);
            redondeo.setArcWidth(24);
            redondeo.setArcHeight(24);
            v.setClip(redondeo);
            fotos[i] = v;
        }
        hero.getChildren().setAll(fotos);

        DoubleProperty pos = new SimpleDoubleProperty();
        Runnable colocar = () -> {
            // La foto principal ocupa la mitad del alto del panel (y hasta 90 % del ancho): crece y encoge con la ventana.
            double s = Math.min(hero.getHeight() * 0.5 / ALTO, hero.getWidth() * 0.9 / ANCHO);
            double alto = ALTO * s;
            for (int i = 0; i < FOTOS; i++) {
                double d = (((i - pos.get()) % FOTOS) + FOTOS) % FOTOS;
                if (d > FOTOS / 2.0) d -= FOTOS;
                double a1 = Math.abs(d);
                double escala = s * (1 - 0.2 * Math.min(a1, 1)); // las vecinas son 20 % más pequeñas
                fotos[i].setScaleX(escala);
                fotos[i].setScaleY(escala);
                // Solo una vecina por lado, cortada a ~la mitad por el clip del hero; la tercera se oculta.
                fotos[i].setTranslateY(Math.signum(d) * (0.9 * alto + HUECO) * Math.min(a1, 1.5));
                fotos[i].setOpacity(Math.max(0, Math.min(1, 2 - a1)));
            }
        };
        pos.addListener((o, a, b) -> colocar.run());
        hero.widthProperty().addListener((o, a, b) -> colocar.run());
        hero.heightProperty().addListener((o, a, b) -> colocar.run());
        pos.set(0.001); // fuerza la primera colocación

        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(3), e ->
                new Timeline(new KeyFrame(Duration.millis(600),
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
        String rol = null;
        int idUsuario = 0;
        try {
            String id = txtUsuario.getText().trim().replaceFirst("^#", "");
            idUsuario = Integer.parseInt(id);
            rol = Personal.autenticar(idUsuario, password.getText());
        } catch (NumberFormatException ignorada) {
            // usuario no numérico: se trata como credenciales incorrectas
        }
        if (rol == null) {
            password.clear();
            new Alert(Alert.AlertType.WARNING, "Usuario o contraseña incorrectos.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));
            Parent raiz = loader.load();
            ((MainController) loader.getController()).configurarRol(rol, idUsuario);
            txtUsuario.getScene().setRoot(raiz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
