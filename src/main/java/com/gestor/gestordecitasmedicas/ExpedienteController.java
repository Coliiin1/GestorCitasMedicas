package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorPDF;
import com.gestor.gestordecitasmedicas.controlador.TextoExpediente;
import com.gestor.gestordecitasmedicas.modelo.*;
import com.itextpdf.text.DocumentException;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class ExpedienteController implements PanelRefrescable {
    @FXML private Label nombre, folio, curp, telefono, nombreFicha, datosPersonales;
    @FXML private Label alergias, antecedentes, historialConsultas, historialRecetas, historialSignos;
    @FXML private TabPane secciones;
    @FXML private Button btnEditar, btnExportar;

    @FXML private VBox vistaExpediente, editorExpediente, camposEditor;
    @FXML private ScrollPane editorScroll;
    @FXML private Label pacienteEditor, errorEdicion, estadoEdicion;
    @FXML private Button btnGuardarEdicion;

    private final BaseDatos baseDatos = new BaseDatos();
    private final PauseTransition ocultarConfirmacion = new PauseTransition(Duration.seconds(5));
    private Paciente pacienteActual;
    private Expediente expedienteActual;

    public void mostrarPaciente(Paciente paciente) {
        pacienteActual = paciente;
        refrescar();
        secciones.getSelectionModel().selectFirst();
    }

    @Override
    public void refrescar() {
        ocultarConfirmacion.stop();
        cancelarEdicion();
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
            antecedentes.setText(TextoExpediente.historia(expediente));
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
        camposEditor.getChildren().clear();
        pacienteEditor.setText(nombre.getText() + " | CURP: " + pacienteActual.getCurp());
        Label ayuda = new Label("Complete la historia clínica con lenguaje médico, sin abreviaturas. Registre expresamente los hallazgos negativos o no aplicables. Los datos de identidad se actualizan en la ficha del paciente.");
        ayuda.setWrapText(true);
        ayuda.getStyleClass().add("card-subtitle");
        camposEditor.getChildren().add(ayuda);
        GridPane grupo0 = grupoEditor("1. Establecimiento");
        TextInputControl tipoEstablecimiento = campoEditor(grupo0, 0, "tipoEstablecimiento", "Tipo de establecimiento", expedienteActual.getTipoEstablecimiento(), false);
        TextInputControl nombreEstablecimiento = campoEditor(grupo0, 1, "nombreEstablecimiento", "Nombre del establecimiento", expedienteActual.getNombreEstablecimiento(), false);
        TextInputControl domicilioEstablecimiento = campoEditor(grupo0, 2, "domicilioEstablecimiento", "Domicilio completo del establecimiento", expedienteActual.getDomicilioEstablecimiento(), false);
        TextInputControl institucionEstablecimiento = campoEditor(grupo0, 3, "institucionEstablecimiento", "Institución de pertenencia (opcional)", expedienteActual.getInstitucionEstablecimiento(), false);
        TextInputControl propietario = campoEditor(grupo0, 4, "propietario", "Razón o denominación social (opcional)", expedienteActual.getPropietario(), false);
        GridPane grupo1 = grupoEditor("2. Interrogatorio y antecedentes");
        TextInputControl familiares = campoEditor(grupo1, 0, "familiares", "Antecedentes heredo-familiares", expedienteActual.getAntecedentesFamiliares(), true);
        TextInputControl personales = campoEditor(grupo1, 1, "personales", "Antecedentes personales patológicos", expedienteActual.getAntecedentesPersonales(), true);
        TextInputControl antecedentesNoPatologicos = campoEditor(grupo1, 2, "antecedentesNoPatologicos", "Antecedentes personales no patológicos", expedienteActual.getAntecedentesNoPatologicos(), true);
        TextInputControl consumoSustancias = campoEditor(grupo1, 3, "consumoSustancias", "Uso y dependencia de tabaco, alcohol y otras sustancias", expedienteActual.getConsumoSustancias(), true);
        TextInputControl alergiasEditor = campoEditor(grupo1, 4, "alergiasEditor", "Alergias y advertencias", expedienteActual.getAlergias(), true);
        TextInputControl grupoEtnico = campoEditor(grupo1, 5, "grupoEtnico", "Grupo étnico (opcional)", expedienteActual.getGrupoEtnico(), false);
        GridPane grupo2 = grupoEditor("3. Padecimiento y exploración");
        TextInputControl padecimientoActual = campoEditor(grupo2, 0, "padecimientoActual", "Padecimiento actual y tratamientos previos", expedienteActual.getPadecimientoActual(), true);
        TextInputControl interrogatorioSistemas = campoEditor(grupo2, 1, "interrogatorioSistemas", "Interrogatorio por aparatos y sistemas", expedienteActual.getInterrogatorioSistemas(), true);
        TextInputControl exploracionFisica = campoEditor(grupo2, 2, "exploracionFisica", "Exploración física por regiones", expedienteActual.getExploracionFisica(), true);
        TextInputControl signosHistoria = campoEditor(grupo2, 3, "signosHistoria", "Signos vitales, peso y talla (con unidades)", expedienteActual.getSignosHistoria(), true);
        GridPane grupo3 = grupoEditor("4. Estudios, diagnóstico y tratamiento");
        TextInputControl resultadosEstudios = campoEditor(grupo3, 0, "resultadosEstudios", "Resultados previos y actuales de estudios", expedienteActual.getResultadosEstudios(), true);
        TextInputControl diagnostico = campoEditor(grupo3, 1, "diagnostico", "Diagnósticos o problemas clínicos", expedienteActual.getDiagnostico(), true);
        TextInputControl pronostico = campoEditor(grupo3, 2, "pronostico", "Pronóstico", expedienteActual.getPronostico(), true);
        TextInputControl indicacionTerapeutica = campoEditor(grupo3, 3, "indicacionTerapeutica", "Indicación terapéutica", expedienteActual.getIndicacionTerapeutica(), true);
        TextInputControl observaciones = campoEditor(grupo3, 4, "observaciones", "Observaciones complementarias", expedienteActual.getObservaciones(), true);
        GridPane grupo4 = grupoEditor("5. Autor y fecha de elaboración");
        TextInputControl autorHistoria = campoEditor(grupo4, 0, "autorHistoria", "Nombre completo de quien elabora", expedienteActual.getAutorHistoria(), false);
        TextInputControl cedulaHistoria = campoEditor(grupo4, 1, "cedulaHistoria", "Cédula profesional", expedienteActual.getCedulaHistoria(), false);
        TextInputControl fechaHoraHistoria = campoEditor(grupo4, 2, "fechaHoraHistoria", "Fecha y hora de elaboración", expedienteActual.getFechaHoraHistoria(), false);
        padecimientoActual.setPromptText("Incluya tratamientos convencionales, alternativos y tradicionales previos.");
        exploracionFisica.setPromptText("Habitus exterior, cabeza, cuello, tórax, abdomen, miembros y genitales, según corresponda.");
        signosHistoria.setPromptText("Temperatura (°C), presión arterial (mmHg), frecuencias cardíaca y respiratoria, peso (kg) y talla (cm).");
        fechaHoraHistoria.setPromptText("aaaa-mm-ddThh:mm; ejemplo: 2026-11-06T10:30");
        if (fechaHoraHistoria.getText().trim().isEmpty()) fechaHoraHistoria.setText(java.time.LocalDateTime.now().withSecond(0).withNano(0).toString());
        errorEdicion.setVisible(false);
        errorEdicion.setManaged(false);
        btnGuardarEdicion.setOnAction(event -> {
            Expediente cambios = new Expediente();
            cambios.setIdExpediente(expedienteActual.getIdExpediente());
            cambios.setDatosPaciente(pacienteActual);
            cambios.setFechaApertura(expedienteActual.getFechaApertura() == null ? new Date() : expedienteActual.getFechaApertura());
            cambios.setTipoEstablecimiento(tipoEstablecimiento.getText().trim());
            cambios.setNombreEstablecimiento(nombreEstablecimiento.getText().trim());
            cambios.setDomicilioEstablecimiento(domicilioEstablecimiento.getText().trim());
            cambios.setInstitucionEstablecimiento(institucionEstablecimiento.getText().trim());
            cambios.setPropietario(propietario.getText().trim());
            cambios.setAntecedentesFamiliares(familiares.getText().trim());
            cambios.setAntecedentesPersonales(personales.getText().trim());
            cambios.setAntecedentesNoPatologicos(antecedentesNoPatologicos.getText().trim());
            cambios.setConsumoSustancias(consumoSustancias.getText().trim());
            cambios.setAlergias(alergiasEditor.getText().trim());
            cambios.setGrupoEtnico(grupoEtnico.getText().trim());
            cambios.setPadecimientoActual(padecimientoActual.getText().trim());
            cambios.setInterrogatorioSistemas(interrogatorioSistemas.getText().trim());
            cambios.setExploracionFisica(exploracionFisica.getText().trim());
            cambios.setSignosHistoria(signosHistoria.getText().trim());
            cambios.setResultadosEstudios(resultadosEstudios.getText().trim());
            cambios.setDiagnostico(diagnostico.getText().trim());
            cambios.setPronostico(pronostico.getText().trim());
            cambios.setIndicacionTerapeutica(indicacionTerapeutica.getText().trim());
            cambios.setObservaciones(observaciones.getText().trim());
            cambios.setAutorHistoria(autorHistoria.getText().trim());
            cambios.setCedulaHistoria(cedulaHistoria.getText().trim());
            cambios.setFechaHoraHistoria(fechaHoraHistoria.getText().trim());
            try {
                java.time.LocalDateTime.parse(cambios.getFechaHoraHistoria());
            } catch (java.time.format.DateTimeParseException ex) {
                mostrarErrorEdicion("La fecha y hora deben tener el formato aaaa-mm-ddThh:mm.");
                fechaHoraHistoria.requestFocus();
                return;
            }
            boolean guardado = cambios.getIdExpediente() > 0
                    ? baseDatos.actualizarExpediente(cambios) : baseDatos.insertarExpediente(cambios);
            if (!guardado) {
                mostrarErrorEdicion("No se pudieron guardar los cambios. Puede volver a intentarlo.");
                return;
            }
            refrescar();
            estadoEdicion.setText("Expediente actualizado correctamente.");
            estadoEdicion.setVisible(true);
            estadoEdicion.setManaged(true);
            ocultarConfirmacion.setOnFinished(e -> {
                estadoEdicion.setVisible(false);
                estadoEdicion.setManaged(false);
            });
            ocultarConfirmacion.playFromStart();
        });
        vistaExpediente.setVisible(false);
        vistaExpediente.setManaged(false);
        editorExpediente.setVisible(true);
        editorExpediente.setManaged(true);
        editorScroll.setVvalue(0);
    }

    @FXML
    private void cancelarEdicion() {
        editorExpediente.setVisible(false);
        editorExpediente.setManaged(false);
        vistaExpediente.setVisible(true);
        vistaExpediente.setManaged(true);
    }

    private void mostrarErrorEdicion(String mensaje) {
        errorEdicion.setText(mensaje);
        errorEdicion.setVisible(true);
        errorEdicion.setManaged(true);
    }

    private GridPane grupoEditor(String titulo) {
        Label encabezado = new Label(titulo);
        encabezado.setMaxWidth(Double.MAX_VALUE);
        encabezado.setWrapText(true);
        encabezado.getStyleClass().add("expediente-grupo-titulo");
        GridPane campos = new GridPane();
        campos.setHgap(24);
        campos.setVgap(20);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(50);
            campos.getColumnConstraints().add(columna);
        }
        VBox grupo = new VBox(18, encabezado, new Separator(), campos);
        grupo.getStyleClass().add("card");
        camposEditor.getChildren().add(grupo);
        return campos;
    }

    private static TextInputControl campoEditor(GridPane grupo, int posicion, String id, String titulo,
                                                String texto, boolean multilinea) {
        TextInputControl campo;
        if (multilinea) {
            TextArea area = new TextArea();
            area.setWrapText(true);
            area.setPrefRowCount(3);
            campo = area;
        } else campo = new TextField();
        campo.setText(texto == null ? "" : texto);
        campo.setId(id);
        campo.setMaxWidth(Double.MAX_VALUE);
        campo.getStyleClass().add("expediente-editor-input");
        Label etiqueta = new Label(titulo.toUpperCase(java.util.Locale.forLanguageTag("es-MX")));
        etiqueta.getStyleClass().add("expediente-campo-titulo");
        etiqueta.setLabelFor(campo);
        etiqueta.setWrapText(true);
        VBox contenedor = new VBox(8, etiqueta, campo);
        contenedor.setMinWidth(0);
        grupo.add(contenedor, posicion % 2, posicion / 2);
        return campo;
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
