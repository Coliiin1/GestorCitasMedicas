package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorPDF;
import com.gestor.gestordecitasmedicas.controlador.TextoExpediente;
import com.gestor.gestordecitasmedicas.modelo.*;
import com.itextpdf.text.DocumentException;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class ExpedienteController implements PanelRefrescable, PanelControlado {
    @FXML private Label nombre, folio, curp, telefono, nombreFicha, datosPersonales;
    @FXML private Label tipoSangre;
    @FXML private Label alergias, antecedentes, historialConsultas, historialRecetas, historialSignos;
    @FXML private TabPane secciones;
    @FXML private Button btnEditar, btnExportar;

    @FXML private Label estadoEdicion;

    private final BaseDatos baseDatos = new BaseDatos();
    private final PauseTransition ocultarConfirmacion = new PauseTransition(Duration.seconds(5));
    private Paciente pacienteActual;
    private Expediente expedienteActual;
    private MainController mainController;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void mostrarPaciente(Paciente paciente) {
        pacienteActual = paciente;
        refrescar();
        secciones.getSelectionModel().selectFirst();
    }

    @Override
    public void refrescar() {
        ocultarConfirmacion.stop();
        estadoEdicion.setVisible(false);
        estadoEdicion.setManaged(false);
        if (pacienteActual == null) return;
        btnEditar.setDisable(true);
        btnExportar.setDisable(true);
        expedienteActual = null;
        try {
            Expediente expediente = baseDatos.cargarExpedientePorCurp(pacienteActual.getCurp());
            if (expediente == null) {
                expediente = new Expediente();
                expediente.setDatosPaciente(baseDatos.cargarPacientePorCurp(pacienteActual.getCurp()));
            }
            expedienteActual = expediente;
            Paciente paciente = expediente.getDatosPaciente();
            if (paciente == null) throw new IllegalStateException("El paciente ya no está registrado.");
            pacienteActual = paciente;
            nombre.setText(TextoExpediente.nombre(paciente));
            nombreFicha.setText(nombre.getText());
            folio.setText(expediente.getIdExpediente() > 0 ? "EXPEDIENTE #" + expediente.getIdExpediente()
                    + " - Apertura: " + TextoExpediente.fecha(expediente.getFechaApertura()) : "SIN EXPEDIENTE REGISTRADO");
            curp.setText(paciente.getCurp());
            telefono.setText(TextoExpediente.valor(paciente.getTelefono()));
            datosPersonales.setText("Edad actual: " + TextoExpediente.edad(paciente, new Date()) + "\nNacimiento: " + TextoExpediente.fecha(paciente.getFechaNacimiento())
                    + "\nSexo: " + TextoExpediente.valor(paciente.getSexo())
                    + "\nDomicilio: " + TextoExpediente.valor(paciente.getDomicilio()));
            alergias.setText(TextoExpediente.valor(expediente.getAlergias()));
            tipoSangre.setText(TextoExpediente.valor(expediente.getTipoSangre()));
            antecedentes.setText(TextoExpediente.historia(expediente));
            historialConsultas.setText(TextoExpediente.consultas(expediente));
            historialRecetas.setText(TextoExpediente.recetas(expediente));
            historialSignos.setText(TextoExpediente.signos(expediente));
            btnEditar.setDisable(false);
            btnExportar.setDisable(false);
        } catch (IllegalStateException ex) {
            expedienteActual = null;
            folio.setText("NO SE PUDO CARGAR EL EXPEDIENTE");
            for (Label label : new Label[]{nombre, nombreFicha, curp, telefono, datosPersonales, alergias, tipoSangre,
                    antecedentes, historialConsultas, historialRecetas, historialSignos}) label.setText("");
            aviso(Alert.AlertType.ERROR, "Error al cargar", ex.getMessage());
        }
    }

    @FXML
    private void editarExpediente() {
        if (expedienteActual == null || mainController == null) return;
        ExpedienteEditarController editor = mainController.controlador("expediente-editar-panel.fxml");
        editor.cargar(pacienteActual, expedienteActual);
        mainController.ir("expediente-editar-panel.fxml");
    }

    /** Muestra el aviso verde de éxito durante unos segundos (se llama después de ir() porque refrescar() lo oculta). */
    public void mostrarConfirmacion(String mensaje) {
        estadoEdicion.setText(mensaje);
        estadoEdicion.setVisible(true);
        estadoEdicion.setManaged(true);
        ocultarConfirmacion.setOnFinished(e -> {
            estadoEdicion.setVisible(false);
            estadoEdicion.setManaged(false);
        });
        ocultarConfirmacion.playFromStart();
    }

    @FXML
    private void exportarPdf() {
        if (expedienteActual == null) return;
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar expediente en PDF");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        selector.setInitialFileName("expediente_" + pacienteActual.getCurp() + ".pdf");
        File archivo = selector.showSaveDialog(nombre.getScene().getWindow());
        if (archivo == null) return;
        try {
            new ControladorPDF().generarExpediente(archivo, expedienteActual);
            aviso(Alert.AlertType.INFORMATION, "Expediente exportado", archivo.getAbsolutePath());
        } catch (IOException | DocumentException ex) {
            aviso(Alert.AlertType.ERROR, "Error al exportar", ex.getMessage());
        }
    }

    private static void aviso(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
