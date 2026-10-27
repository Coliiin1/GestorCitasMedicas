package com.gestor.gestordecitasmedicas;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class CitaController {
    @FXML private Label lblTimer, lblImc, lblPaciente, lblPacienteBanner;
    @FXML private Label lblEdad, lblNacimiento, lblCurp, lblSexo, lblSangre, lblAlergias;
    @FXML private TextField txtPresion, txtFrecuencia, txtOxigeno, txtTemperatura, txtGlucosa, txtPeso, txtTalla;
    @FXML private TextArea txtMotivo;

    private final Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
    private int segundos;

    @FXML
    private void initialize() {
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
        txtPeso.textProperty().addListener((o, a, b) -> calcularImc());
        txtTalla.textProperty().addListener((o, a, b) -> calcularImc());
        calcularImc();
    }

    private void tick() {
        segundos++;
        lblTimer.setText(String.format("%02d:%02d", segundos / 60, segundos % 60));
    }

    private void calcularImc() {
        try {
            double peso = Double.parseDouble(txtPeso.getText().trim());
            double talla = Double.parseDouble(txtTalla.getText().trim());
            if (talla <= 0) throw new NumberFormatException();
            double imc = peso / (talla * talla);
            String clase = imc < 18.5 ? "Bajo peso" : imc < 25 ? "Normal" : imc < 30 ? "Sobrepeso" : "Obesidad";
            lblImc.setText(String.format("%.1f (%s)", imc, clase));
        } catch (NumberFormatException e) {
            lblImc.setText("--");
        }
    }

    @FXML
    private void onPausar() {
        if (reloj.getStatus() == Animation.Status.RUNNING) reloj.pause(); else reloj.play();
    }

    // TODO: conectar con modelo (Cita/Paciente/SignosVitales) cuando exista el flujo de citas.
    @FXML private void onCancelar() { pendiente("Cancelar consulta"); }
    @FXML private void onVerExpediente() { pendiente("Expediente clínico"); }

    private void pendiente(String accion) {
        new Alert(Alert.AlertType.INFORMATION, accion + ": falta implementación.").show();
    }
}
