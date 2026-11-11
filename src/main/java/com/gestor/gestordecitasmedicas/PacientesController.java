package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.TextoExpediente;
import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import static com.gestor.gestordecitasmedicas.Ui.aviso;

public class PacientesController implements PanelControlado, PanelRefrescable {
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbOrden;
    @FXML private VBox listaPacientes;
    @FXML private Label lblContador;

    private final BaseDatos baseDatos = new BaseDatos();
    private final ArrayList<Paciente> pacientes = new ArrayList<>();
    private MainController mainController;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        cmbOrden.getItems().addAll("Nombre A–Z", "Nombre Z–A");
        cmbOrden.setValue("Nombre A–Z");
        cmbOrden.valueProperty().addListener((o, a, b) -> aplicarFiltro());
        txtBuscar.textProperty().addListener((o, a, b) -> aplicarFiltro());
        refrescar();
    }

    @Override
    public void refrescar() {
        pacientes.clear();
        pacientes.addAll(baseDatos.cargarPacientes().values());
        aplicarFiltro();
    }

    @FXML
    private void onBuscar() {
        aplicarFiltro();
    }

    @FXML
    private void onNuevo() {
        PacienteEditarController editor = mainController.controlador("paciente-editar-panel.fxml");
        editor.modoNuevo();
        mainController.ir("paciente-editar-panel.fxml");
    }

    private void aplicarFiltro() {
        String filtro = normalizar(txtBuscar.getText());
        ArrayList<Paciente> filtrados = new ArrayList<>();
        for (Paciente paciente : pacientes) {
            if (filtro.isEmpty()
                    || normalizar(nombreCompleto(paciente)).contains(filtro)
                    || normalizar(paciente.getCurp()).contains(filtro)) {
                filtrados.add(paciente);
            }
        }
        Comparator<Paciente> orden = Comparator
                .comparing((Paciente p) -> normalizar(p.getApellidoPaterno()))
                .thenComparing(p -> normalizar(p.getApellidoMaterno()))
                .thenComparing(p -> normalizar(p.getNombre()));
        filtrados.sort("Nombre Z–A".equals(cmbOrden.getValue()) ? orden.reversed() : orden);
        mostrarPacientes(filtrados);
        lblContador.setText("MOSTRANDO " + filtrados.size() + " DE " + pacientes.size() + " REGISTROS");
    }

    private void mostrarPacientes(ArrayList<Paciente> filtrados) {
        listaPacientes.getChildren().clear();
        if (filtrados.isEmpty()) {
            Label vacio = new Label("No se encontraron pacientes registrados.");
            vacio.getStyleClass().add("hint");
            listaPacientes.getChildren().add(vacio);
            return;
        }
        for (Paciente paciente : filtrados) {
            listaPacientes.getChildren().add(crearTarjeta(paciente));
        }
    }

    private HBox crearTarjeta(Paciente paciente) {
        Label avatar = new Label(iniciales(paciente));
        avatar.setMinHeight(44.0);
        avatar.setMinWidth(44.0);
        avatar.getStyleClass().add("avatar-lg");

        Label nombre = new Label(nombreCompleto(paciente));
        nombre.getStyleClass().add("patient-card-name");

        HBox identificadores = new HBox(0,
                etiqueta("CURP: ", "patient-meta"),
                etiqueta(valor(paciente.getCurp(), "No registrada"), "meta-mono"),
                etiqueta("   •   Tel: ", "patient-meta"),
                etiqueta(valor(paciente.getTelefono(), "No registrado"), "meta-value"));
        identificadores.setAlignment(Pos.CENTER_LEFT);

        HBox contacto = new HBox(0,
                etiqueta("Nacimiento: ", "patient-meta"),
                etiqueta(TextoExpediente.fecha(paciente.getFechaNacimiento()), "meta-value"),
                etiqueta("   •   Sexo: ", "patient-meta"),
                etiqueta(valor(paciente.getSexo(), "No especificado"), "meta-value"),
                etiqueta("   •   Correo: ", "patient-meta"),
                etiqueta(valor(paciente.getCorreo(), "No registrado"), "meta-value"));
        contacto.setAlignment(Pos.CENTER_LEFT);

        VBox datos = new VBox(4, nombre, identificadores, contacto);
        HBox.setHgrow(datos, Priority.ALWAYS);

        Button editar = new Button("Editar");
        editar.getStyleClass().add("btn-edit");
        editar.setOnAction(e -> editar(paciente));

        Button expediente = new Button("Expediente");
        expediente.getStyleClass().add("btn-edit");
        expediente.setOnAction(e -> abrirExpediente(paciente));

        Button eliminar = new Button("Eliminar");
        eliminar.getStyleClass().add("btn-del");
        eliminar.setOnAction(e -> eliminar(paciente));

        HBox acciones = new HBox(8, editar, expediente, eliminar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        HBox tarjeta = new HBox(14, avatar, datos, acciones);
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.getStyleClass().add("patient-card");
        return tarjeta;
    }

    private void editar(Paciente paciente) {
        PacienteEditarController editor = mainController.controlador("paciente-editar-panel.fxml");
        editor.modoEditar(paciente);
        mainController.ir("paciente-editar-panel.fxml");
    }

    private void abrirExpediente(Paciente paciente) {
        ExpedienteController expediente = mainController.controlador("expediente-panel.fxml");
        expediente.mostrarPaciente(paciente);
        mainController.ir("expediente-panel.fxml");
    }

    private void eliminar(Paciente paciente) {
        ButtonType eliminar = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar paciente");
        confirmacion.setHeaderText("¿Eliminar a " + nombreCompleto(paciente) + "?");
        confirmacion.setContentText("También se eliminarán su expediente, citas, consultas y recetas asociadas. "
                + "Esta acción no se puede deshacer.");
        confirmacion.getButtonTypes().setAll(eliminar, cancelar);

        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (!respuesta.isPresent() || respuesta.get() != eliminar) {
            return;
        }

        if (baseDatos.eliminarPaciente(paciente.getCurp())) {
            refrescar();
            aviso(Alert.AlertType.INFORMATION, "Paciente eliminado",
                    "El paciente se eliminó correctamente.");
        } else {
            aviso(Alert.AlertType.ERROR, "No se pudo eliminar",
                    "Ocurrió un error al eliminar el paciente. Inténtalo de nuevo.");
        }
    }

    private static Label etiqueta(String texto, String estilo) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        return label;
    }

    private static String iniciales(Paciente paciente) {
        String nombre = valor(paciente.getNombre()).trim();
        String apellido = valor(paciente.getApellidoPaterno()).trim();
        StringBuilder iniciales = new StringBuilder();
        if (!nombre.isEmpty()) {
            iniciales.append(Character.toUpperCase(nombre.charAt(0)));
        }
        if (!apellido.isEmpty()) {
            iniciales.append(Character.toUpperCase(apellido.charAt(0)));
        }
        return iniciales.length() == 0 ? "--" : iniciales.toString();
    }

    private static String nombreCompleto(Paciente paciente) {
        StringBuilder nombre = new StringBuilder();
        agregar(nombre, paciente.getNombre());
        agregar(nombre, paciente.getApellidoPaterno());
        agregar(nombre, paciente.getApellidoMaterno());
        return nombre.toString();
    }

    private static void agregar(StringBuilder texto, String parte) {
        if (parte == null || parte.trim().isEmpty()) {
            return;
        }
        if (texto.length() > 0) {
            texto.append(' ');
        }
        texto.append(parte.trim());
    }

    private static String valor(String texto) {
        return valor(texto, "");
    }

    private static String valor(String texto, String porDefecto) {
        return texto == null || texto.trim().isEmpty() ? porDefecto : texto.trim();
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim().toLowerCase();
    }
}
