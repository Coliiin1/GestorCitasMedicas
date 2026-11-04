package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorPDF;
import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Expediente;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import com.itextpdf.text.DocumentException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExpedienteController {
    @FXML private Label nombre;
    @FXML private Label folio;
    @FXML private Label curp;
    @FXML private Label telefono;
    @FXML private Label nombreFicha;
    @FXML private Label datosPersonales;
    @FXML private Label alergias;
    @FXML private Label antecedentes;
    @FXML private TabPane secciones;

    private final BaseDatos baseDatos = new BaseDatos();

    @FXML
    private void exportarPdf() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar expediente en PDF");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        selector.setInitialFileName("expediente_" + curp.getText() + ".pdf");
        File archivo = selector.showSaveDialog(nombre.getScene().getWindow());
        if (archivo == null) return;
        try {
            new ControladorPDF().generarExpediente(archivo, nombre.getText(), folio.getText(), curp.getText(), telefono.getText());
            Alert aviso = new Alert(Alert.AlertType.INFORMATION);
            aviso.setTitle("Expediente exportado");
            aviso.setHeaderText("El expediente se guardó correctamente.");
            aviso.setContentText(archivo.getAbsolutePath());
            aviso.showAndWait();
        } catch (IOException | DocumentException ex) {
            Alert aviso = new Alert(Alert.AlertType.ERROR);
            aviso.setTitle("Error al exportar");
            aviso.setHeaderText("No se pudo guardar el expediente.");
            aviso.setContentText(ex.getMessage());
            aviso.showAndWait();
        }
    }

    public void mostrarPaciente(Paciente paciente) {
        Expediente expediente = baseDatos.cargarExpedientePorCurp(paciente.getCurp());

        String nombreCompleto = nombreCompleto(paciente);
        nombre.setText(nombreCompleto);
        nombreFicha.setText(nombreCompleto);
        folio.setText(expediente == null ? "SIN EXPEDIENTE REGISTRADO" : "EXPEDIENTE #" + expediente.getIdExpediente());
        curp.setText(valor(paciente.getCurp(), "No registrada"));
        telefono.setText(valor(paciente.getTelefono(), "No registrado"));
        datosPersonales.setText(datosPersonales(paciente));
        alergias.setText(expediente == null
                ? "Sin información disponible."
                : valor(expediente.getAlergias(), "Sin alergias registradas."));
        antecedentes.setText(antecedentes(expediente));
        secciones.getSelectionModel().selectFirst();
    }

    private static String datosPersonales(Paciente paciente) {
        StringBuilder datos = new StringBuilder();
        agregar(datos, "Nacimiento: " + fecha(paciente.getFechaNacimiento()));
        agregar(datos, "Sexo: " + valor(paciente.getSexo(), "No especificado"));
        agregar(datos, "Domicilio: " + valor(paciente.getDomicilio(), "No registrado"));
        return datos.length() == 0 ? "No disponibles" : datos.toString();
    }

    private static String antecedentes(Expediente expediente) {
        if (expediente == null) {
            return "No hay antecedentes registrados para este paciente.";
        }
        StringBuilder datos = new StringBuilder();
        agregar(datos, "Familiares: " + valor(expediente.getAntecedentesFamiliares(), "No registrados"));
        agregar(datos, "Personales: " + valor(expediente.getAntecedentesPersonales(), "No registrados"));
        agregar(datos, "Observaciones: " + valor(expediente.getObservaciones(), "Sin observaciones"));
        return datos.length() == 0 ? "No hay antecedentes registrados para este paciente." : datos.toString();
    }

    private static String nombreCompleto(Paciente paciente) {
        StringBuilder nombre = new StringBuilder();
        agregarEspacio(nombre, paciente.getNombre());
        agregarEspacio(nombre, paciente.getApellidoPaterno());
        agregarEspacio(nombre, paciente.getApellidoMaterno());
        return nombre.toString();
    }

    private static void agregarEspacio(StringBuilder texto, String parte) {
        if (parte == null || parte.trim().isEmpty()) {
            return;
        }
        if (texto.length() > 0) {
            texto.append(' ');
        }
        texto.append(parte.trim());
    }

    private static void agregar(StringBuilder texto, String parte) {
        if (parte == null || parte.trim().isEmpty()) {
            return;
        }
        if (texto.length() > 0) {
            texto.append("  •  ");
        }
        texto.append(parte.trim());
    }

    private static String fecha(Date fecha) {
        return fecha == null ? "No registrada" : new SimpleDateFormat("dd/MM/yyyy").format(fecha);
    }

    private static String valor(String texto, String porDefecto) {
        return texto == null || texto.trim().isEmpty() ? porDefecto : texto.trim();
    }
}
