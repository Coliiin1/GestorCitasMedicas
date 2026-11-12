package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.servicio.ControladorPDF;
import com.gestor.gestordecitasmedicas.modelo.Administrador;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Function;
import com.gestor.gestordecitasmedicas.modelo.Personal;

public class ConfiguracionController implements PanelControlado, PanelRefrescable {
    /** Un campo del formulario: etiqueta, longitud máxima (columna VARCHAR), obligatorio, lectura y escritura. */
    private static final class Campo {
        final String etiqueta;
        final int max;
        final int span; // columnas de 12 que ocupa en la rejilla
        final boolean obligatorio;
        final Function<Administrador, String> leer;
        final BiConsumer<Administrador, String> escribir;
        final TextField campo = new TextField();

        Campo(String etiqueta, String ejemplo, int max, int span, boolean obligatorio,
              Function<Administrador, String> leer, BiConsumer<Administrador, String> escribir) {
            campo.setPromptText("Ej. " + ejemplo);
            this.etiqueta = etiqueta;
            this.max = max;
            this.span = span;
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

    private MainController mainController;
    private Administrador admin;

    // Límites = tamaños de columna de la tabla Administrador (SQLite no los impone, así que se validan aquí).
    private final Campo[] campos = {
            new Campo("NOMBRE (S)", "Juan Carlos", 20, 4, true, Administrador::getNombre, Administrador::setNombre),
            new Campo("APELLIDO PATERNO", "Ramírez", 20, 4, true, Administrador::getApellidoPaterno, Administrador::setApellidoPaterno),
            new Campo("APELLIDO MATERNO", "López", 20, 4, false, Administrador::getApellidoMaterno, Administrador::setApellidoMaterno),
            new Campo("CÉDULA PROFESIONAL", "12345678", 8, 3, false, Administrador::getCedulaProfesional, Administrador::setCedulaProfesional),
            new Campo("INSTITUCIÓN", "UNAM", 20, 3, false, Administrador::getInstitucionAcademica, Administrador::setInstitucionAcademica),
            new Campo("PROFESIÓN", "Médico Cirujano", 100, 6, false, Administrador::getProfesion, Administrador::setProfesion),
            new Campo("ESPECIALIDAD", "Medicina Familiar", 100, 6, false, Administrador::getEspecialidad, Administrador::setEspecialidad),
            new Campo("REGISTRO DE ESPECIALIDAD", "87654321", 100, 6, false, Administrador::getRegistroEspecialidad, Administrador::setRegistroEspecialidad),
            new Campo("NOMBRE DEL ESTABLECIMIENTO", "Consultorio Médico Dr. Ramírez", 100, 12, false, Administrador::getNombreEstablecimiento, Administrador::setNombreEstablecimiento),
            new Campo("DOMICILIO", "Av. Juárez 123, Col. Centro, Morelia, Mich.", 100, 12, false, Administrador::getDomicilio, Administrador::setDomicilio),
            new Campo("DOMICILIO DEL ESTABLECIMIENTO", "Av. Madero 456, Col. Centro, Morelia, Mich.", 100, 12, false, Administrador::getDomicilioEstablecimiento, Administrador::setDomicilioEstablecimiento),
    };

    @FXML
    private void initialize() {
        int col = 0, fila = 0;
        for (Campo c : campos) {
            c.campo.setPrefHeight(40);
            c.campo.getStyleClass().add("form-field");
            VBox caja = new VBox(8, new Label(c.etiqueta + (c.obligatorio ? " *" : "")), c.campo);
            caja.getChildren().get(0).getStyleClass().add("field-label");
            gridDatos.add(caja, col, fila, c.span, 1);
            col += c.span;
            if (col >= 12) { col = 0; fila++; }
        }
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void refrescar() {
        admin = Administrador.buscar(mainController.idAdministrador());
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
            Notificador.error("Administrador no encontrado", "No se encontró el registro del administrador.");
            return;
        }
        for (Campo c : campos) {
            String t = c.campo.getText().trim();
            if (c.obligatorio && t.isEmpty()) {
                Notificador.advertencia("Datos incompletos", "El campo " + c.etiqueta + " es obligatorio.");
                return;
            }
            if (t.length() > c.max) {
                Notificador.advertencia("Texto demasiado largo", c.etiqueta + " no debe exceder " + c.max + " caracteres.");
                return;
            }
        }
        for (Campo c : campos) c.escribir.accept(admin, c.campo.getText().trim());
        if (!admin.actualizar()) {
            Notificador.error("Datos no guardados", "No se pudieron guardar los datos. Inténtalo de nuevo.");
            return;
        }
        mainController.aplicarConfiguracion();
        Notificador.exito("Datos guardados", "Los datos del consultorio se guardaron correctamente.");
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
            Notificador.advertencia("Carpeta sin permiso", "No hay permiso de escritura en esa carpeta.");
            return;
        }
        if (ControladorPDF.guardarCarpetaSalida(carpeta.getAbsolutePath())) {
            txtDirectorio.setText(carpeta.getAbsolutePath());
        } else {
            Notificador.error("Carpeta no guardada", "No se pudo guardar el directorio.");
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
            Notificador.advertencia("Usuario inválido", "El usuario debe ser un número entero positivo.");
        } else if (!nueva.equals(pwdConfirmar.getText())) {
            Notificador.advertencia("Contraseñas distintas", "La confirmación no coincide con la nueva contraseña.");
        } else if (nuevoId == idActual && nueva.isEmpty()) {
            Notificador.info("Sin cambios", "No hay cambios que guardar.");
        } else if (!"admin".equals(Personal.autenticar(idActual, pwdActual.getText()))) {
            Notificador.advertencia("Contraseña incorrecta", "La contraseña actual es incorrecta.");
        } else if (!Administrador.usuarioDisponible(nuevoId, idActual)) {
            Notificador.advertencia("Usuario en uso", "El usuario " + nuevoId + " ya está en uso por otra cuenta.");
        } else if (!Administrador.cambiarCredenciales(idActual, nuevoId, nueva)) {
            Notificador.error("Cambios no guardados", "No se pudieron guardar los cambios.");
        } else {
            mainController.setIdAdministrador(nuevoId);
            pwdActual.clear();
            pwdNueva.clear();
            pwdConfirmar.clear();
            Notificador.exito("Acceso actualizado", "Datos de acceso actualizados. Usa el usuario " + nuevoId
                    + " y la contraseña vigente en el próximo inicio de sesión.");
        }
    }

}
