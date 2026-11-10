package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorPDF;
import com.gestor.gestordecitasmedicas.modelo.Administrador;
import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ConfiguracionController implements PanelControlado, PanelRefrescable {
    /** Un campo del formulario: etiqueta, longitud máxima (columna VARCHAR), obligatorio, lectura y escritura. */
    private static final class Campo {
        final String etiqueta;
        final int max;
        final boolean obligatorio;
        final Function<Administrador, String> leer;
        final BiConsumer<Administrador, String> escribir;
        final TextField campo = new TextField();

        Campo(String etiqueta, String ejemplo, int max, boolean obligatorio,
              Function<Administrador, String> leer, BiConsumer<Administrador, String> escribir) {
            campo.setPromptText("Ej. " + ejemplo);
            this.etiqueta = etiqueta;
            this.max = max;
            this.obligatorio = obligatorio;
            this.leer = leer;
            this.escribir = escribir;
        }
    }

    @FXML private GridPane gridDatos;
    @FXML private TextField txtDirectorio;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField pwdActual;
    @FXML private PasswordField pwdNueva;
    @FXML private PasswordField pwdConfirmar;

    private final BaseDatos baseDatos = new BaseDatos();
    private MainController mainController;
    private Administrador admin;

    // Límites = tamaños de columna de la tabla Administrador (SQLite no los impone, así que se validan aquí).
    private final Campo[] campos = {
            new Campo("NOMBRE (S)", "Juan Carlos", 20, true, Administrador::getNombre, Administrador::setNombre),
            new Campo("APELLIDO PATERNO", "Ramírez", 20, true, Administrador::getApellidoPaterno, Administrador::setApellidoPaterno),
            new Campo("APELLIDO MATERNO", "López", 20, false, Administrador::getApellidoMaterno, Administrador::setApellidoMaterno),
            new Campo("CÉDULA PROFESIONAL", "12345678", 8, false, Administrador::getCedulaProfesional, Administrador::setCedulaProfesional),
            new Campo("INSTITUCIÓN", "UNAM", 20, false, Administrador::getInstitucionAcademica, Administrador::setInstitucionAcademica),
            new Campo("DOMICILIO", "Av. Juárez 123, Col. Centro, Morelia, Mich.", 100, false, Administrador::getDomicilio, Administrador::setDomicilio),
            new Campo("PROFESIÓN", "Médico Cirujano", 100, false, Administrador::getProfesion, Administrador::setProfesion),
            new Campo("ESPECIALIDAD", "Medicina Familiar", 100, false, Administrador::getEspecialidad, Administrador::setEspecialidad),
            new Campo("REGISTRO DE ESPECIALIDAD", "87654321", 100, false, Administrador::getRegistroEspecialidad, Administrador::setRegistroEspecialidad),
            new Campo("NOMBRE DEL ESTABLECIMIENTO", "Consultorio Médico Dr. Ramírez", 100, false, Administrador::getNombreEstablecimiento, Administrador::setNombreEstablecimiento),
            new Campo("DOMICILIO DEL ESTABLECIMIENTO", "Av. Madero 456, Col. Centro, Morelia, Mich.", 100, false, Administrador::getDomicilioEstablecimiento, Administrador::setDomicilioEstablecimiento),
    };

    @FXML
    private void initialize() {
        for (int i = 0; i < campos.length; i++) {
            Campo c = campos[i];
            c.campo.setPrefHeight(40);
            c.campo.getStyleClass().add("form-field");
            VBox caja = new VBox(8, new Label(c.etiqueta + (c.obligatorio ? " *" : "")), c.campo);
            caja.getChildren().get(0).getStyleClass().add("field-label");
            gridDatos.add(caja, i % 2, i / 2);
        }
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void refrescar() {
        admin = baseDatos.obtenerAdministrador(mainController.idAdministrador());
        for (Campo c : campos) {
            c.campo.setText(admin == null || c.leer.apply(admin) == null ? "" : c.leer.apply(admin));
        }
        txtDirectorio.setText(ControladorPDF.carpetaSalida());
        txtUsuario.setText(String.valueOf(mainController.idAdministrador()));
        pwdActual.clear();
        pwdNueva.clear();
        pwdConfirmar.clear();
    }

    @FXML
    private void onGuardarDatos() {
        if (admin == null) {
            aviso(Alert.AlertType.ERROR, "No se encontró el registro del administrador.");
            return;
        }
        for (Campo c : campos) {
            String t = c.campo.getText().trim();
            if (c.obligatorio && t.isEmpty()) {
                aviso(Alert.AlertType.WARNING, "El campo " + c.etiqueta + " es obligatorio.");
                return;
            }
            if (t.length() > c.max) {
                aviso(Alert.AlertType.WARNING, c.etiqueta + " no debe exceder " + c.max + " caracteres.");
                return;
            }
        }
        for (Campo c : campos) c.escribir.accept(admin, c.campo.getText().trim());
        if (!baseDatos.actualizarAdministrador(admin)) {
            aviso(Alert.AlertType.ERROR, "No se pudieron guardar los datos. Inténtalo de nuevo.");
            return;
        }
        mainController.aplicarConfiguracion();
        aviso(Alert.AlertType.INFORMATION, "Los datos del consultorio se guardaron correctamente.");
    }

    @FXML
    private void onExaminar() {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle("Carpeta para los PDF");
        File actual = new File(txtDirectorio.getText());
        if (actual.isDirectory()) selector.setInitialDirectory(actual);
        File carpeta = selector.showDialog(txtDirectorio.getScene().getWindow());
        if (carpeta == null) return;
        if (!carpeta.canWrite()) {
            aviso(Alert.AlertType.WARNING, "No hay permiso de escritura en esa carpeta.");
            return;
        }
        if (baseDatos.guardarConfig(ControladorPDF.CLAVE_CARPETA, carpeta.getAbsolutePath())) {
            txtDirectorio.setText(carpeta.getAbsolutePath());
        } else {
            aviso(Alert.AlertType.ERROR, "No se pudo guardar el directorio.");
        }
    }

    @FXML
    private void onGuardarCredenciales() {
        int idActual = mainController.idAdministrador();
        int nuevoId;
        try {
            nuevoId = Integer.parseInt(txtUsuario.getText().trim().replaceFirst("^#", ""));
        } catch (NumberFormatException e) {
            nuevoId = -1;
        }
        String nueva = pwdNueva.getText();
        if (nuevoId <= 0) {
            aviso(Alert.AlertType.WARNING, "El usuario debe ser un número entero positivo.");
        } else if (!nueva.equals(pwdConfirmar.getText())) {
            aviso(Alert.AlertType.WARNING, "La confirmación no coincide con la nueva contraseña.");
        } else if (nuevoId == idActual && nueva.isEmpty()) {
            aviso(Alert.AlertType.INFORMATION, "No hay cambios que guardar.");
        } else if (!"admin".equals(baseDatos.autenticar(idActual, pwdActual.getText()))) {
            aviso(Alert.AlertType.WARNING, "La contraseña actual es incorrecta.");
        } else if (!baseDatos.idUsuarioLibre(nuevoId, idActual)) {
            aviso(Alert.AlertType.WARNING, "El usuario " + nuevoId + " ya está en uso por otra cuenta.");
        } else if (!baseDatos.actualizarCredencialesAdmin(idActual, nuevoId, nueva)) {
            aviso(Alert.AlertType.ERROR, "No se pudieron guardar los cambios.");
        } else {
            mainController.setIdAdministrador(nuevoId);
            pwdActual.clear();
            pwdNueva.clear();
            pwdConfirmar.clear();
            aviso(Alert.AlertType.INFORMATION, "Datos de acceso actualizados. Usa el usuario " + nuevoId
                    + " y la contraseña vigente en el próximo inicio de sesión.");
        }
    }

    private static void aviso(Alert.AlertType tipo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
