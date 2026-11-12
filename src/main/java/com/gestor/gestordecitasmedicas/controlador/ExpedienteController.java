package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.servicio.ControladorPDF;
import com.gestor.gestordecitasmedicas.servicio.TextoExpediente;
import com.gestor.gestordecitasmedicas.modelo.*;
import com.itextpdf.text.DocumentException;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import javafx.geometry.Pos;

import static com.gestor.gestordecitasmedicas.controlador.Ui.aviso;

public class ExpedienteController implements PanelRefrescable, PanelControlado {
    @FXML private Label nombre, folio, curp, telefono, nombreFicha, datosPersonales;
    @FXML private Label tipoSangre;
    @FXML private Label alergias, antecedentes, historialSignos;
    @FXML private VBox listaConsultas;
    @FXML private TabPane secciones;
    @FXML private Button btnEditar, btnExportar;

    @FXML private Label estadoEdicion;

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
            Expediente expediente = Expediente.dePaciente(pacienteActual.getCurp());
            if (expediente == null) {
                expediente = new Expediente();
                expediente.setDatosPaciente(Paciente.buscar(pacienteActual.getCurp()));
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
            mostrarConsultas(expediente);
            historialSignos.setText(TextoExpediente.signos(expediente));
            btnEditar.setDisable(false);
            btnExportar.setDisable(false);
        } catch (IllegalStateException ex) {
            expedienteActual = null;
            folio.setText("NO SE PUDO CARGAR EL EXPEDIENTE");
            for (Label label : new Label[]{nombre, nombreFicha, curp, telefono, datosPersonales, alergias, tipoSangre,
                    antecedentes, historialSignos}) label.setText("");
            listaConsultas.getChildren().clear();
            aviso(Alert.AlertType.ERROR, "Error al cargar", ex.getMessage());
        }
    }

    private void mostrarConsultas(Expediente expediente) {
        listaConsultas.getChildren().clear();
        if (expediente.getConsultas().isEmpty()) {
            Label vacio = new Label("Sin consultas registradas.");
            vacio.getStyleClass().add("hint");
            listaConsultas.getChildren().add(vacio);
            return;
        }
        for (Consulta c : expediente.getConsultas()) {
            String dx = c.getDiagnostico() == null || c.getDiagnostico().trim().isEmpty() ? "Sin diagnóstico" : c.getDiagnostico().trim();
            String fecha = c.getFechaConsulta() == null ? "Sin fecha"
                    : c.getFechaConsulta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            Label titulo = new Label(dx + " — " + fecha);
            titulo.setWrapText(true);
            titulo.getStyleClass().add("row-name");
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);
            Button imprimir = new Button("Imprimir receta");
            imprimir.getStyleClass().add("btn-secondary");
            imprimir.setGraphic(new FontIcon("fi-rr-file-pdf"));
            imprimir.setDisable(c.getReceta() == null || c.getReceta().getMedicamentos().isEmpty());
            imprimir.setOnAction(e -> imprimirReceta(c.getReceta()));
            Button detalles = new Button("Ver detalles");
            detalles.getStyleClass().add("btn-primary");
            detalles.setOnAction(e -> verDetalles(c));
            HBox fila = new HBox(12, titulo, espacio, imprimir, detalles);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("row-card");
            listaConsultas.getChildren().add(fila);
        }
    }

    private void imprimirReceta(Receta receta) {
        if (receta == null) return;
        if (new ControladorPDF().generarPDF(receta)) {
            File archivo = new File(ControladorPDF.carpetaSalida(), "receta_" + receta.getIdReceta() + ".pdf");
            ControladorPDF.abrir(archivo);
            aviso(Alert.AlertType.INFORMATION, "Receta generada", archivo.getAbsolutePath());
        } else {
            aviso(Alert.AlertType.ERROR, "PDF no generado", "No se pudo generar la receta. Revise que los datos estén completos.");
        }
    }

    private void verDetalles(Consulta consulta) {
        if (mainController == null) return;
        ConsultaController editor = mainController.controlador("consulta-panel.fxml");
        if (!editor.cargarConsultaParaEdicion(consulta)) {
            aviso(Alert.AlertType.ERROR, "No disponible", "No se encontró la cita vinculada a esta consulta.");
            return;
        }
        mainController.ir("consulta-panel.fxml");
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
}
