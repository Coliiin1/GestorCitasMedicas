package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.modelo.Asistente;
import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

public class AsistenteEditarController implements PanelControlado {
    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellidoPaterno;
    @FXML private TextField txtApellidoMaterno;
    @FXML private TextField txtId;
    @FXML private PasswordField password;
    @FXML private TextField passwordVisible;
    @FXML private Button btnTogglePassword;

    private final BaseDatos baseDatos = new BaseDatos();
    private MainController mainController;
    private Asistente asistenteActual;
    private String contrasenaOriginal;
    private boolean visible;

    @FXML
    private void initialize() {
        passwordVisible.textProperty().bindBidirectional(password.textProperty());
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void modoNuevo() {
        asistenteActual = null;
        contrasenaOriginal = null;
        lblTitulo.setText("NUEVO ASISTENTE");
        txtId.setText("Sin asignar");
        txtNombre.clear();
        txtApellidoPaterno.clear();
        txtApellidoMaterno.clear();
        password.clear();
        passwordVisible.clear();
        txtNombre.requestFocus();
    }

    public void modoEditar(Asistente asistente) {
        asistenteActual = asistente;
        contrasenaOriginal = asistente.getContrasena();
        lblTitulo.setText("EDITAR ASISTENTE");
        txtId.setText(String.valueOf(asistente.getIdAsistente()));
        txtNombre.setText(valor(asistente.getNombre()));
        txtApellidoPaterno.setText(valor(asistente.getApellidoPaterno()));
        txtApellidoMaterno.setText(valor(asistente.getApellidoMaterno()));
        password.clear();
        passwordVisible.clear();
        txtNombre.requestFocus();
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
    private void onGuardar() {
        String nombre = texto(txtNombre);
        String apellidoPaterno = texto(txtApellidoPaterno);
        String apellidoMaterno = texto(txtApellidoMaterno);
        String contrasena = password.getText();

        if (nombre.isEmpty() || apellidoPaterno.isEmpty()) {
            mostrarError("El nombre y el apellido paterno son obligatorios.");
            return;
        }
        if (nombre.length() > 20 || apellidoPaterno.length() > 20 || apellidoMaterno.length() > 20) {
            mostrarError("El nombre y los apellidos no deben exceder 20 caracteres.");
            return;
        }
        if (contrasena.isEmpty()) {
            if (asistenteActual == null) {
                mostrarError("La contraseña es obligatoria para registrar un asistente.");
                return;
            }
            contrasena = contrasenaOriginal == null ? "" : contrasenaOriginal;
        }

        boolean esNuevo = asistenteActual == null;
        Asistente asistente = esNuevo ? new Asistente() : asistenteActual;
        asistente.setNombre(nombre);
        asistente.setApellidoPaterno(apellidoPaterno);
        asistente.setApellidoMaterno(apellidoMaterno);
        asistente.setContrasena(contrasena);

        boolean guardado = esNuevo
                ? baseDatos.insertarAsistente(asistente)
                : baseDatos.actualizarAsistente(asistente);

        if (!guardado) {
            mostrarError("No se pudo guardar el asistente. Inténtalo de nuevo.");
            return;
        }

        mostrarInformacion(esNuevo ? "Asistente registrado" : "Asistente actualizado",
                esNuevo ? "El asistente se registró correctamente." : "Los datos del asistente se actualizaron.");
        mainController.ir("asistentes-panel.fxml");
    }

    private static String texto(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private void mostrarError(String mensaje) {
        Ui.aviso(Alert.AlertType.WARNING, "Datos incompletos", mensaje);
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Ui.aviso(Alert.AlertType.INFORMATION, titulo, mensaje);
    }
}
