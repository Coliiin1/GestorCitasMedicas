package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorCorreo;
import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.Expediente;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import com.gestor.gestordecitasmedicas.modelo.Receta;
import com.gestor.gestordecitasmedicas.modelo.SignosVitales;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.gestor.gestordecitasmedicas.Ui.aviso;

public class CitaController implements PanelControlado, PanelRefrescable {
    @FXML private Label lblTimer, lblImc, lblPaciente, lblPacienteBanner;
    @FXML private Label lblEdad, lblNacimiento, lblCurp, lblSexo, lblSangre, lblAlergias;
    @FXML private TextField txtPresion, txtFrecuencia, txtFrecuenciaRespiratoria, txtOxigeno,
            txtTemperatura, txtPeso, txtTalla;
    @FXML private TextArea txtMotivo;
    @FXML private Button btnCancelar, btnSiguiente, btnExpediente;

    private final Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
    private int segundos;

    private MainController mainController;
    private Cita citaActual;
    private Paciente pacienteActual;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
        txtPeso.textProperty().addListener((o, a, b) -> calcularImc());
        txtTalla.textProperty().addListener((o, a, b) -> calcularImc());
        calcularImc();
        limpiar();
    }

    @Override
    public void refrescar() {
        if (citaActual == null) {
            limpiar();
        } else {
            cargarCita(citaActual);
        }
    }

    public void cargarCita(Cita cita) {
        if (cita == null) {
            limpiar();
            return;
        }
        citaActual = cita;
        pacienteActual = Paciente.buscar(cita.getCurp());
        segundos = 0;
        tick();
        if (reloj.getStatus() != Animation.Status.RUNNING) {
            reloj.play();
        }

        String nombre = Paciente.nombreParaMostrar(pacienteActual, cita.getCurp());
        lblPaciente.setText(nombre);
        lblPacienteBanner.setText(nombre);
        lblEdad.setText(pacienteActual == null ? "Edad: --"
                : "Edad: " + Paciente.edadTexto(pacienteActual) + " años");
        lblNacimiento.setText(pacienteActual == null ? "--" : fecha(pacienteActual.getFechaNacimiento()));
        lblCurp.setText(cita.getCurp() == null ? "--" : cita.getCurp());
        lblSexo.setText(pacienteActual == null || pacienteActual.getSexo() == null
                ? "No registrado" : pacienteActual.getSexo());
        mostrarDatosExpediente(cita.getCurp());

        Receta existente = Receta.deCita(cita.getIdCita());
        mostrarSignos(existente == null ? null : existente.getSignosVitales());
        txtMotivo.setText(existente == null || existente.getDiagnostico() == null
                ? "" : existente.getDiagnostico());

        btnCancelar.setDisable(false);
        btnSiguiente.setDisable(false);
        btnExpediente.setDisable(pacienteActual == null);
    }

    private void limpiar() {
        citaActual = null;
        pacienteActual = null;
        segundos = 0;
        lblTimer.setText("00:00");
        reloj.pause();
        lblPaciente.setText("Sin cita seleccionada");
        lblPacienteBanner.setText("Sin cita seleccionada");
        lblEdad.setText("Edad: --");
        lblNacimiento.setText("--");
        lblCurp.setText("--");
        lblSexo.setText("--");
        lblSangre.setText("--");
        lblAlergias.setText("--");
        txtMotivo.clear();
        limpiarSignos();
        if (btnCancelar != null) btnCancelar.setDisable(true);
        if (btnSiguiente != null) btnSiguiente.setDisable(true);
        if (btnExpediente != null) btnExpediente.setDisable(true);
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

    @FXML
    private void onCancelar() {
        if (citaActual == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cancelar consulta");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Cancelar la cita #" + citaActual.getIdCita() + "?");
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        if (!citaActual.cancelar()) {
            aviso(Alert.AlertType.ERROR, "No se pudo cancelar", "La cita no se actualizó en la base de datos.");
            return;
        }
        ControladorCorreo.notificarCancelaciones(List.of(citaActual), Paciente.todos(),
                HorarioConfigController::avisoNotificacion);
        limpiar();
        if (mainController != null) {
            mainController.ir("agenda-panel.fxml");
        }
    }

    @FXML
    private void onSiguiente() {
        if (citaActual == null || mainController == null) {
            aviso(Alert.AlertType.INFORMATION, "Sin cita",
                    "Selecciona una cita desde la agenda para iniciar la consulta.");
            return;
        }
        if (pacienteActual == null) {
            aviso(Alert.AlertType.ERROR, "Paciente no disponible",
                    "El paciente de esta cita ya no está registrado.");
            return;
        }
        String faltantes = signosFaltantes();
        if (!faltantes.isEmpty()) {
            aviso(Alert.AlertType.WARNING, "Signos vitales incompletos",
                    "Complete todos los signos vitales con valores válidos antes de continuar:\n" + faltantes);
            return;
        }
        ConsultaController controlador = mainController.controlador("consulta-panel.fxml");
        controlador.cargarConsulta(citaActual, leerSignos());
        reloj.pause();
        mainController.ir("consulta-panel.fxml");
    }

    @FXML
    private void onVerExpediente() {
        if (pacienteActual == null || mainController == null) {
            return;
        }
        ExpedienteController controlador = mainController.controlador("expediente-panel.fxml");
        controlador.mostrarPaciente(pacienteActual);
        mainController.ir("expediente-panel.fxml");
    }

    /** Lista los signos vitales vacíos o inválidos; cadena vacía si todos son obligatorios y válidos. */
    private String signosFaltantes() {
        StringBuilder sb = new StringBuilder();
        String presion = txtPresion.getText() == null ? "" : txtPresion.getText().trim();
        if (!presion.matches("\\d{2,3}/\\d{2,3}")) sb.append("- Presión arterial (ej. 120/80)\n");
        if (entero(txtFrecuencia.getText(), 0) <= 0) sb.append("- Frecuencia cardíaca (ej. 72)\n");
        if (entero(txtOxigeno.getText(), 0) <= 0) sb.append("- Saturación de oxígeno (ej. 98)\n");
        if (decimal(txtTemperatura.getText(), 0f) <= 0) sb.append("- Temperatura (ej. 36.6)\n");
        if (entero(txtFrecuenciaRespiratoria.getText(), 0) <= 0) sb.append("- Frecuencia respiratoria (ej. 18)\n");
        if (decimal(txtPeso.getText(), 0f) <= 0) sb.append("- Peso (ej. 74.2)\n");
        if (talla(txtTalla.getText()) <= 0) sb.append("- Talla (ej. 1.75)\n");
        return sb.toString().trim();
    }

    private SignosVitales leerSignos() {
        return new SignosVitales(
                txtPresion.getText() == null ? "" : txtPresion.getText().trim(),
                entero(txtFrecuencia.getText(), 0),
                entero(txtFrecuenciaRespiratoria.getText(), 0),
                decimal(txtTemperatura.getText(), 0f),
                entero(txtOxigeno.getText(), 0),
                decimal(txtPeso.getText(), 0f),
                talla(txtTalla.getText()));
    }

    private void limpiarSignos() {
        txtPresion.clear();
        txtFrecuencia.clear();
        txtFrecuenciaRespiratoria.clear();
        txtOxigeno.clear();
        txtTemperatura.clear();
        txtPeso.clear();
        txtTalla.clear();
        lblImc.setText("--");
    }

    private void mostrarSignos(SignosVitales signos) {
        if (signos == null) {
            limpiarSignos();
            return;
        }
        txtPresion.setText(signos.getPresionArterial() == null ? "" : signos.getPresionArterial());
        txtFrecuencia.setText(String.valueOf(signos.getFrecuenciaCardiaca()));
        txtFrecuenciaRespiratoria.setText(String.valueOf(signos.getFrecuenciaRespiratoria()));
        txtTemperatura.setText(String.valueOf(signos.getTemperatura()));
        txtOxigeno.setText(String.valueOf(signos.getSaturacionOxigeno()));
        txtPeso.setText(String.valueOf(signos.getPeso()));
        txtTalla.setText(signos.getTalla() > 3
                ? String.format("%.2f", signos.getTalla() / 100f)
                : String.valueOf(signos.getTalla()));
        calcularImc();
    }

    private static int entero(String texto, int defecto) {
        if (texto == null || texto.trim().isEmpty()) return defecto;
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private static float decimal(String texto, float defecto) {
        if (texto == null || texto.trim().isEmpty()) return defecto;
        try {
            return Float.parseFloat(texto.trim());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private static int talla(String texto) {
        float valor = decimal(texto, 0f);
        if (valor <= 0) return 0;
        return Math.round(valor > 3 ? valor : valor * 100);
    }

    /** Tipo de sangre y alergias del expediente (se carga una sola vez). */
    private void mostrarDatosExpediente(String curp) {
        Expediente expediente;
        try {
            expediente = Expediente.dePaciente(curp);
        } catch (IllegalStateException e) {
            lblSangre.setText("No disponible");
            lblAlergias.setText("No disponible");
            return;
        }
        lblSangre.setText(expediente == null || expediente.getTipoSangre() == null
                || expediente.getTipoSangre().trim().isEmpty() ? "No registrado" : expediente.getTipoSangre());
        lblAlergias.setText(expediente == null || expediente.getAlergias() == null
                || expediente.getAlergias().trim().isEmpty() ? "Ninguna reportada" : expediente.getAlergias());
    }

    private static String fecha(Date fecha) {
        return fecha == null ? "--" : new SimpleDateFormat("dd/MM/yyyy").format(fecha);
    }
}
