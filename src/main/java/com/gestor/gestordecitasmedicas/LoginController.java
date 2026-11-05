package com.gestor.gestordecitasmedicas;

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
        // Provisional: el rol se decide por el texto del usuario ("admin" o "asis"), sin contraseña.
        String rol = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim().toLowerCase();
        if (!rol.equals("admin") && !rol.equals("asis")) {
            new Alert(Alert.AlertType.WARNING, "Usuario no válido. Usa \"admin\" o \"asis\".").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));
            Parent raiz = loader.load();
            ((MainController) loader.getController()).configurarRol(rol);
            txtUsuario.getScene().setRoot(raiz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
