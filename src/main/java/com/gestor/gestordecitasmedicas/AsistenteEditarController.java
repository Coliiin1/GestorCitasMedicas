package com.gestor.gestordecitasmedicas;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

public class AsistenteEditarController {
    @FXML private PasswordField password;
    @FXML private TextField passwordVisible;
    @FXML private Button btnTogglePassword;

    private boolean visible;

    @FXML
    private void initialize() {
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
}
