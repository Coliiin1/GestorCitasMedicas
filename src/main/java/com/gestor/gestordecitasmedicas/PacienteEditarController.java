package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Expediente;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Consumer;

public class PacienteEditarController implements PanelControlado {
    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellidoPaterno;
    @FXML private TextField txtApellidoMaterno;
    @FXML private TextField txtCurp;
    @FXML private DatePicker dpFechaNacimiento;
    @FXML private TextField txtTelefono;
    @FXML private ComboBox<String> cbSexo;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtDomicilio;
    @FXML private Button btnCancelar;
    @FXML private javafx.scene.layout.GridPane boxClinico;
    @FXML private ComboBox<String> cbTipoSangre;
    @FXML private TextField txtAlergias;

    private final BaseDatos baseDatos = new BaseDatos();
    private MainController mainController;
    private Paciente pacienteActual;
    private String panelRegreso = "pacientes-panel.fxml";
    private Consumer<Paciente> alRegistrar;
    private Runnable alCancelar;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void modoNuevo() {
        modoNuevo("pacientes-panel.fxml", null, null);
    }

    public void modoNuevo(String panelRegreso, Consumer<Paciente> alRegistrar, Runnable alCancelar) {
        this.panelRegreso = panelRegreso == null || panelRegreso.trim().isEmpty()
                ? "pacientes-panel.fxml" : panelRegreso;
        this.alRegistrar = alRegistrar;
        this.alCancelar = alCancelar;
        actualizarBotonCancelar();
        pacienteActual = null;
        lblTitulo.setText("REGISTRAR NUEVO PACIENTE");
        txtCurp.setEditable(true);
        txtNombre.clear();
        txtApellidoPaterno.clear();
        txtApellidoMaterno.clear();
        txtCurp.clear();
        dpFechaNacimiento.setValue(null);
        txtTelefono.clear();
        cbSexo.getSelectionModel().clearSelection();
        txtCorreo.clear();
        txtDomicilio.clear();
        cbTipoSangre.getSelectionModel().clearSelection();
        txtAlergias.clear();
        boxClinico.setVisible(true);
        boxClinico.setManaged(true);
        txtNombre.requestFocus();
    }

    public void modoEditar(Paciente paciente) {
        pacienteActual = paciente;
        panelRegreso = "pacientes-panel.fxml";
        alRegistrar = null;
        alCancelar = null;
        actualizarBotonCancelar();
        lblTitulo.setText("ACTUALIZAR DATOS DEL PACIENTE");
        txtCurp.setEditable(false);
        txtNombre.setText(valor(paciente.getNombre()));
        txtApellidoPaterno.setText(valor(paciente.getApellidoPaterno()));
        txtApellidoMaterno.setText(valor(paciente.getApellidoMaterno()));
        txtCurp.setText(valor(paciente.getCurp()));
        dpFechaNacimiento.setValue(aLocalDate(paciente.getFechaNacimiento()));
        txtTelefono.setText(valor(paciente.getTelefono()));
        if (paciente.getSexo() == null || paciente.getSexo().trim().isEmpty()) {
            cbSexo.getSelectionModel().clearSelection();
        } else {
            cbSexo.getSelectionModel().select(paciente.getSexo());
        }
        txtCorreo.setText(valor(paciente.getCorreo()));
        txtDomicilio.setText(valor(paciente.getDomicilio()));
        boxClinico.setVisible(false);
        boxClinico.setManaged(false);
        txtNombre.requestFocus();
    }

    @FXML
    private void onGuardar() {
        String nombre = texto(txtNombre);
        String apellidoPaterno = texto(txtApellidoPaterno);
        String apellidoMaterno = texto(txtApellidoMaterno);
        String curp = texto(txtCurp).toUpperCase();
        LocalDate fechaNacimiento = dpFechaNacimiento.getValue();
        String telefono = texto(txtTelefono);
        String sexo = cbSexo.getValue();
        String correo = texto(txtCorreo);
        String domicilio = texto(txtDomicilio);

        String error = validar(nombre, apellidoPaterno, apellidoMaterno, curp, fechaNacimiento,
                telefono, correo, domicilio);
        if (error == null && texto(txtAlergias).length() > 200) {
            error = "Las alergias no deben exceder 200 caracteres.";
        }
        if (error != null) {
            mostrarError(error);
            return;
        }

        boolean esNuevo = pacienteActual == null;
        Date fecha = aDate(fechaNacimiento);
        Paciente paciente = esNuevo
                ? new Paciente(curp, fecha, valorNulo(sexo), valorNulo(domicilio), valorNulo(telefono), valorNulo(correo))
                : pacienteActual;
        paciente.setNombre(nombre);
        paciente.setApellidoPaterno(apellidoPaterno);
        paciente.setApellidoMaterno(apellidoMaterno);
        paciente.setFechaNacimiento(fecha);
        paciente.setSexo(valorNulo(sexo));
        paciente.setDomicilio(valorNulo(domicilio));
        paciente.setTelefono(valorNulo(telefono));
        paciente.setCorreo(valorNulo(correo));

        boolean guardado = esNuevo
                ? baseDatos.insertarPaciente(paciente)
                : baseDatos.actualizarPaciente(paciente);

        if (!guardado) {
            mostrarError(esNuevo
                    ? "No se pudo registrar el paciente. Verifica que la CURP no esté registrada."
                    : "No se pudo actualizar el paciente. Inténtalo de nuevo.");
            return;
        }

        if (esNuevo) {
            boolean expedienteCreado = crearExpediente(paciente);
            if (expedienteCreado) {
                mostrarInformacion("Paciente registrado",
                        "El paciente y su expediente se registraron correctamente.");
            } else {
                mostrarInformacion("Paciente registrado",
                        "El paciente se registró, pero no se pudo crear su expediente automáticamente.");
            }
        } else {
            mostrarInformacion("Paciente actualizado", "Los datos del paciente se actualizaron correctamente.");
        }

        Consumer<Paciente> registrar = esNuevo ? alRegistrar : null;
        String destino = panelRegreso;
        alRegistrar = null;
        alCancelar = null;
        if (registrar != null) {
            registrar.accept(paciente);
        }
        mainController.ir(destino);
    }

    @FXML
    private void onCancelar() {
        Runnable cancelar = alCancelar;
        alRegistrar = null;
        alCancelar = null;
        if (cancelar != null) {
            cancelar.run();
        }
        mainController.ir(panelRegreso);
    }

    private void actualizarBotonCancelar() {
        if (btnCancelar == null) {
            return;
        }
        btnCancelar.setText("agenda-panel.fxml".equals(panelRegreso)
                ? "Cancelar / Volver a la Agenda"
                : "Cancelar / Volver a Pacientes");
    }

    private boolean crearExpediente(Paciente paciente) {
        Expediente expediente = new Expediente();
        expediente.setDatosPaciente(paciente);
        expediente.setFechaApertura(new Date());
        expediente.setAntecedentesFamiliares("");
        expediente.setAntecedentesPersonales("");
        expediente.setAlergias(texto(txtAlergias));
        expediente.setTipoSangre(cbTipoSangre.getValue());
        expediente.setObservaciones("");
        return baseDatos.insertarExpediente(expediente);
    }

    private static String validar(String nombre, String apellidoPaterno, String apellidoMaterno,
                                  String curp, LocalDate fechaNacimiento, String telefono,
                                  String correo, String domicilio) {
        if (nombre.isEmpty() || apellidoPaterno.isEmpty()) {
            return "El nombre y el apellido paterno son obligatorios.";
        }
        if (nombre.length() > 20 || apellidoPaterno.length() > 20 || apellidoMaterno.length() > 20) {
            return "El nombre y los apellidos no deben exceder 20 caracteres.";
        }
        if (!curp.matches("[A-Z0-9]{18}")) {
            return "La CURP debe tener 18 caracteres alfanuméricos.";
        }
        if (fechaNacimiento == null) {
            return "La fecha de nacimiento es obligatoria.";
        }
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            return "La fecha de nacimiento no puede ser futura.";
        }
        if (!telefono.isEmpty() && !telefono.matches("\\d{10}")) {
            return "El teléfono debe contener 10 dígitos.";
        }
        if (!correo.isEmpty() && !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "El correo electrónico no tiene un formato válido.";
        }
        if (correo.length() > 50) {
            return "El correo electrónico no debe exceder 50 caracteres.";
        }
        if (domicilio.length() > 100) {
            return "El domicilio no debe exceder 100 caracteres.";
        }
        return null;
    }

    private static String texto(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private static String valorNulo(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : texto.trim();
    }

    private static Date aDate(LocalDate fecha) {
        if (fecha == null) {
            return null;
        }
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate aLocalDate(Date fecha) {
        if (fecha == null) {
            return null;
        }
        return Instant.ofEpochMilli(fecha.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle("Datos incompletos");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
