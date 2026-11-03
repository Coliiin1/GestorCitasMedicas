package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorPDF;
import com.itextpdf.text.DocumentException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;

public class ExpedienteController {
    @FXML private Label nombre;
    @FXML private Label folio;
    @FXML private Label curp;
    @FXML private Label telefono;
    @FXML private Label nombreFicha;
    @FXML private TabPane secciones;

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
    public void mostrarPaciente(String[] datos) {
        nombre.setText(datos[0]);
        nombreFicha.setText(datos[0]);
        folio.setText("EXPEDIENTE " + datos[1]);
        curp.setText(datos[2]);
        telefono.setText(datos[3]);
        secciones.getSelectionModel().selectFirst();
    }
}

