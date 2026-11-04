package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorPDF;
import com.gestor.gestordecitasmedicas.controlador.TextoExpediente;
import com.gestor.gestordecitasmedicas.modelo.*;
import com.itextpdf.text.DocumentException;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class ExpedienteController implements PanelRefrescable {
    @FXML private Label nombre, folio, curp, telefono, nombreFicha, datosPersonales;
    @FXML private Label alergias, antecedentes, historialConsultas, historialRecetas, historialSignos;
    @FXML private TabPane secciones;
    @FXML private Button btnEditar, btnExportar;

    private final BaseDatos baseDatos = new BaseDatos();
    private Paciente pacienteActual;
    private Expediente expedienteActual;

    public void mostrarPaciente(Paciente paciente) {
        pacienteActual = paciente;
        refrescar();
        secciones.getSelectionModel().selectFirst();
    }

    @Override
    public void refrescar() {
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
            datosPersonales.setText("Nacimiento: " + TextoExpediente.fecha(paciente.getFechaNacimiento())
                    + "\nSexo: " + TextoExpediente.valor(paciente.getSexo())
                    + "\nDomicilio: " + TextoExpediente.valor(paciente.getDomicilio()));
            alergias.setText(TextoExpediente.valor(expediente.getAlergias()));
            antecedentes.setText(TextoExpediente.antecedentes(expediente));
            historialConsultas.setText(TextoExpediente.consultas(expediente));
            historialRecetas.setText(TextoExpediente.recetas(expediente));
            historialSignos.setText(TextoExpediente.signos(expediente));
            btnEditar.setDisable(false);
            btnExportar.setDisable(false);
        } catch (IllegalStateException ex) {
            expedienteActual = null;
            folio.setText("NO SE PUDO CARGAR EL EXPEDIENTE");
            for (Label label : new Label[]{nombre, nombreFicha, curp, telefono, datosPersonales, alergias,
                    antecedentes, historialConsultas, historialRecetas, historialSignos}) label.setText("");
            aviso(Alert.AlertType.ERROR, "Error al cargar", ex.getMessage());
        }
    }

    @FXML
    private void editarExpediente() {
        if (expedienteActual == null) return;
        Dialog<ButtonType> dialogo = crearEditor();
        if (dialogo.showAndWait().orElse(ButtonType.CANCEL).getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            refrescar();
            aviso(Alert.AlertType.INFORMATION, "Expediente actualizado",
                    "Los cambios del expediente se guardaron correctamente.");
        }
    }

    Dialog<ButtonType> crearEditor() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.initOwner(nombre.getScene().getWindow());
        dialogo.setTitle("Editar expediente");
        DialogPane panel = dialogo.getDialogPane();
        panel.getStylesheets().add(ExpedienteController.class.getResource("cita.css").toExternalForm());
        panel.getStyleClass().add("expediente-editor");
        Label titulo = new Label("EDITAR EXPEDIENTE CLÍNICO");
        titulo.getStyleClass().add("field-label");
        Label paciente = new Label(nombre.getText());
        paciente.getStyleClass().add("panel-title");
        paciente.setWrapText(true);
        Label identificador = new Label("CURP: " + pacienteActual.getCurp());
        identificador.getStyleClass().add("meta-mono");
        VBox encabezado = new VBox(8, titulo, paciente, identificador);
        encabezado.getStyleClass().add("expediente-editor-header");
        panel.setHeader(encabezado);
        TextArea familiares = campo(expedienteActual.getAntecedentesFamiliares());
        TextArea personales = campo(expedienteActual.getAntecedentesPersonales());
        TextArea alergiasEditor = campo(expedienteActual.getAlergias());
        TextArea observaciones = campo(expedienteActual.getObservaciones());
        VBox contenido = new VBox(8, etiquetaCampo("ANTECEDENTES FAMILIARES", familiares), familiares,
                etiquetaCampo("ANTECEDENTES PERSONALES", personales), personales,
                etiquetaCampo("ALERGIAS Y ADVERTENCIAS", alergiasEditor), alergiasEditor,
                etiquetaCampo("OBSERVACIONES", observaciones), observaciones);
        contenido.getStyleClass().add("expediente-editor-fields");
        panel.setPrefWidth(620);
        dialogo.getDialogPane().setContent(contenido);
        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        panel.lookupButton(guardar).getStyleClass().add("btn-primary");
        panel.lookupButton(ButtonType.CANCEL).getStyleClass().add("btn-secondary");
        dialogo.getDialogPane().lookupButton(guardar).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Expediente cambios = new Expediente();
            cambios.setIdExpediente(expedienteActual.getIdExpediente());
            cambios.setDatosPaciente(pacienteActual);
            cambios.setFechaApertura(expedienteActual.getFechaApertura() == null
                    ? new Date() : expedienteActual.getFechaApertura());
            cambios.setAntecedentesFamiliares(familiares.getText().trim());
            cambios.setAntecedentesPersonales(personales.getText().trim());
            cambios.setAlergias(alergiasEditor.getText().trim());
            cambios.setObservaciones(observaciones.getText().trim());
            boolean guardado = cambios.getIdExpediente() > 0
                    ? baseDatos.actualizarExpediente(cambios) : baseDatos.insertarExpediente(cambios);
            if (!guardado) {
                event.consume();
                aviso(Alert.AlertType.ERROR, "No se pudo guardar", "Los cambios no se guardaron. Puedes volver a intentarlo.");
            }
        });
        return dialogo;
    }

    private static TextArea campo(String texto) {
        TextArea campo = new TextArea(texto == null ? "" : texto);
        campo.setWrapText(true);
        campo.setPrefRowCount(3);
        campo.getStyleClass().add("expediente-editor-input");
        return campo;
    }

    private static Label etiquetaCampo(String texto, TextArea campo) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("field-label");
        etiqueta.setLabelFor(campo);
        return etiqueta;
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
