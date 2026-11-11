package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.TextoExpediente;
import com.gestor.gestordecitasmedicas.modelo.Expediente;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Escena de edición del expediente clínico (antes vivía dentro de expediente-panel.fxml). */
public class ExpedienteEditarController implements PanelControlado {
    private static final Locale ES = Locale.forLanguageTag("es-MX");
    private static final Set<String> CONECTORES = new HashSet<>(Arrays.asList(
            "de", "del", "y", "o", "e", "la", "el", "los", "las", "con", "por", "en", "a", "para"));

    @FXML private VBox camposEditor;
    @FXML private ScrollPane editorScroll;
    @FXML private Label pacienteEditor, errorEdicion;

    private final Map<String, TextInputControl> campos = new LinkedHashMap<>();
    private MainController mainController;
    private Paciente paciente;
    private Expediente expediente;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /** Reconstruye el formulario con los datos actuales del expediente. */
    public void cargar(Paciente paciente, Expediente expediente) {
        this.paciente = paciente;
        this.expediente = expediente;
        campos.clear();
        camposEditor.getChildren().clear();
        pacienteEditor.setText(TextoExpediente.nombre(paciente) + " | CURP: " + paciente.getCurp());
        errorEdicion.setVisible(false);
        errorEdicion.setManaged(false);

        Label ayuda = new Label("Complete la historia clínica con lenguaje médico, sin abreviaturas. Registre expresamente los hallazgos negativos o no aplicables. Los datos de identidad se actualizan en la ficha del paciente.");
        ayuda.setWrapText(true);
        ayuda.getStyleClass().add("card-subtitle");
        camposEditor.getChildren().add(ayuda);

        GridPane g0 = grupo("1. Establecimiento", false);
        campo(g0, 0, "tipoEstablecimiento", "Tipo de establecimiento", expediente.getTipoEstablecimiento(), false, "Consultorio médico privado");
        campo(g0, 1, "nombreEstablecimiento", "Nombre del establecimiento", expediente.getNombreEstablecimiento(), false, "Consultorio Médico Dr. Ramírez");
        campo(g0, 2, "domicilioEstablecimiento", "Domicilio completo del establecimiento", expediente.getDomicilioEstablecimiento(), false, "Av. Juárez 123, Col. Centro, Morelia, Mich.");
        campo(g0, 3, "institucionEstablecimiento", "Institución de pertenencia (opcional)", expediente.getInstitucionEstablecimiento(), false, "IMSS");
        campo(g0, 4, "propietario", "Razón o denominación social (opcional)", expediente.getPropietario(), false, "Servicios Médicos Ramírez S.C.");

        GridPane g1 = grupo("2. Interrogatorio y antecedentes", true);
        campo(g1, 0, "familiares", "Antecedentes heredo-familiares", expediente.getAntecedentesFamiliares(), true, "Madre con diabetes mellitus tipo 2; padre con hipertensión arterial.");
        campo(g1, 1, "personales", "Antecedentes personales patológicos", expediente.getAntecedentesPersonales(), true, "Apendicectomía en 2015; sin enfermedades crónicas.");
        campo(g1, 2, "antecedentesNoPatologicos", "Antecedentes personales no patológicos", expediente.getAntecedentesNoPatologicos(), true, "Esquema de vacunación completo; ejercicio tres veces por semana.");
        campo(g1, 3, "consumoSustancias", "Uso y dependencia de tabaco, alcohol y otras sustancias", expediente.getConsumoSustancias(), true, "Tabaquismo negado; consumo ocasional de alcohol.");
        campo(g1, 4, "alergiasEditor", "Alergias y advertencias", expediente.getAlergias(), true, "Penicilina: urticaria; sin otras alergias conocidas.");
        campo(g1, 5, "tipoSangre", "Tipo de sangre (opcional)", expediente.getTipoSangre(), false, "O+");
        campo(g1, 6, "grupoEtnico", "Grupo étnico (opcional)", expediente.getGrupoEtnico(), false, "Náhuatl");

        GridPane g2 = grupo("3. Padecimiento y exploración", true);
        campo(g2, 0, "padecimientoActual", "Padecimiento actual y tratamientos previos", expediente.getPadecimientoActual(), true, "Tos seca de tres días de evolución; tratamiento previo con paracetamol y miel.");
        campo(g2, 1, "interrogatorioSistemas", "Interrogatorio por aparatos y sistemas", expediente.getInterrogatorioSistemas(), true, "Respiratorio: tos y rinorrea; digestivo: sin alteraciones.");
        campo(g2, 2, "exploracionFisica", "Exploración física por regiones", expediente.getExploracionFisica(), true, "Cabeza, cuello, tórax, abdomen y miembros sin alteraciones; faringe hiperémica.");
        campo(g2, 3, "signosHistoria", "Signos vitales, peso y talla (con unidades)", expediente.getSignosHistoria(), true, "Temp. 36.8 °C, PA 120/80 mmHg, FC 76 lpm, FR 18 rpm, 70 kg, 170 cm.");

        GridPane g3 = grupo("4. Estudios, diagnóstico y tratamiento", true);
        campo(g3, 0, "resultadosEstudios", "Resultados previos y actuales de estudios", expediente.getResultadosEstudios(), true, "Biometría hemática dentro de parámetros normales (05/11/2026).");
        campo(g3, 1, "diagnostico", "Diagnósticos o problemas clínicos", expediente.getDiagnostico(), true, "Faringitis aguda.");
        campo(g3, 2, "pronostico", "Pronóstico", expediente.getPronostico(), true, "Bueno para la vida y la función.");
        campo(g3, 3, "indicacionTerapeutica", "Indicación terapéutica", expediente.getIndicacionTerapeutica(), true, "Reposo relativo e hidratación abundante.");
        campo(g3, 4, "observaciones", "Observaciones complementarias", expediente.getObservaciones(), true, "Cita de control en siete días.");

        GridPane g4 = grupo("5. Autor y fecha de elaboración", true);
        campo(g4, 0, "autorHistoria", "Nombre completo de quien elabora", expediente.getAutorHistoria(), false, "Dr. Juan Pérez López");
        campo(g4, 1, "cedulaHistoria", "Cédula profesional", expediente.getCedulaHistoria(), false, "12345678");
        campo(g4, 2, "fechaHoraHistoria", "Fecha y hora de elaboración", expediente.getFechaHoraHistoria(), false, "2026-11-06T10:30");
        TextInputControl fecha = campos.get("fechaHoraHistoria");
        if (fecha.getText().trim().isEmpty()) {
            fecha.setText(LocalDateTime.now().withSecond(0).withNano(0).toString());
        }
        editorScroll.setVvalue(0);
    }

    @FXML
    private void onCancelar() {
        mainController.ir("expediente-panel.fxml");
    }

    @FXML
    private void onGuardar() {
        Expediente c = new Expediente();
        c.setIdExpediente(expediente.getIdExpediente());
        c.setDatosPaciente(paciente);
        c.setFechaApertura(expediente.getFechaApertura() == null ? new Date() : expediente.getFechaApertura());
        c.setTipoEstablecimiento(texto("tipoEstablecimiento"));
        c.setNombreEstablecimiento(texto("nombreEstablecimiento"));
        c.setDomicilioEstablecimiento(texto("domicilioEstablecimiento"));
        c.setInstitucionEstablecimiento(texto("institucionEstablecimiento"));
        c.setPropietario(texto("propietario"));
        c.setAntecedentesFamiliares(texto("familiares"));
        c.setAntecedentesPersonales(texto("personales"));
        c.setAntecedentesNoPatologicos(texto("antecedentesNoPatologicos"));
        c.setConsumoSustancias(texto("consumoSustancias"));
        c.setAlergias(texto("alergiasEditor"));
        c.setTipoSangre(texto("tipoSangre").toUpperCase(ES));
        c.setGrupoEtnico(texto("grupoEtnico"));
        c.setPadecimientoActual(texto("padecimientoActual"));
        c.setInterrogatorioSistemas(texto("interrogatorioSistemas"));
        c.setExploracionFisica(texto("exploracionFisica"));
        c.setSignosHistoria(texto("signosHistoria"));
        c.setResultadosEstudios(texto("resultadosEstudios"));
        c.setDiagnostico(texto("diagnostico"));
        c.setPronostico(texto("pronostico"));
        c.setIndicacionTerapeutica(texto("indicacionTerapeutica"));
        c.setObservaciones(texto("observaciones"));
        c.setAutorHistoria(texto("autorHistoria"));
        c.setCedulaHistoria(texto("cedulaHistoria"));
        c.setFechaHoraHistoria(texto("fechaHoraHistoria"));
        if (!c.getTipoSangre().matches("|(A|B|AB|O)[+-]")) {
            mostrarError("El tipo de sangre debe ser A, B, AB u O seguido de + o - (por ejemplo O+).");
            campos.get("tipoSangre").requestFocus();
            return;
        }
        try {
            LocalDateTime.parse(c.getFechaHoraHistoria());
        } catch (DateTimeParseException ex) {
            mostrarError("La fecha y hora deben tener el formato aaaa-mm-ddThh:mm.");
            campos.get("fechaHoraHistoria").requestFocus();
            return;
        }
        boolean guardado = c.guardar();
        if (!guardado) {
            mostrarError("No se pudieron guardar los cambios. Puede volver a intentarlo.");
            return;
        }
        mainController.ir("expediente-panel.fxml");
        ExpedienteController vista = mainController.controlador("expediente-panel.fxml");
        vista.mostrarConfirmacion("Expediente actualizado correctamente.");
    }

    private String texto(String id) {
        return campos.get(id).getText().trim();
    }

    private void mostrarError(String mensaje) {
        errorEdicion.setText(mensaje);
        errorEdicion.setVisible(true);
        errorEdicion.setManaged(true);
    }

    private GridPane grupo(String titulo, boolean conSeparador) {
        Label encabezado = new Label(titulo.toUpperCase(ES));
        encabezado.setWrapText(true);
        encabezado.getStyleClass().add("section-label");
        GridPane rejilla = new GridPane();
        rejilla.setHgap(24);
        rejilla.setVgap(20);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(50);
            rejilla.getColumnConstraints().add(columna);
        }
        VBox grupo = new VBox(16, encabezado, rejilla);
        if (conSeparador) grupo.getStyleClass().add("form-section");
        camposEditor.getChildren().add(grupo);
        return rejilla;
    }

    private void campo(GridPane grupo, int posicion, String id, String titulo, String valor,
                       boolean multilinea, String ejemplo) {
        TextInputControl campo;
        if (multilinea) {
            TextArea area = new TextArea();
            area.setWrapText(true);
            area.setPrefRowCount(3);
            area.getStyleClass().add("reason-area");
            campo = area;
        } else {
            campo = new TextField();
            campo.getStyleClass().add("form-field");
        }
        campo.setText(valor == null ? "" : valor);
        campo.setId(id);
        campo.setPromptText("Ej. " + ejemplo);
        campo.setMaxWidth(Double.MAX_VALUE);
        Label etiqueta = new Label(tituloCaso(titulo));
        etiqueta.getStyleClass().add("field-label");
        etiqueta.setLabelFor(campo);
        etiqueta.setWrapText(true);
        VBox contenedor = new VBox(8, etiqueta, campo);
        contenedor.setMinWidth(0);
        grupo.add(contenedor, posicion % 2, posicion / 2);
        campos.put(id, campo);
    }

    /** Primera letra mayúscula en cada palabra, salvo conectores; lo que va entre paréntesis queda igual. */
    static String tituloCaso(String texto) {
        int parentesis = texto.indexOf('(');
        String base = parentesis < 0 ? texto : texto.substring(0, parentesis).trim();
        String aclaracion = parentesis < 0 ? "" : " " + texto.substring(parentesis);
        StringBuilder resultado = new StringBuilder();
        String[] palabras = base.split(" ");
        for (int i = 0; i < palabras.length; i++) {
            String palabra = palabras[i];
            if (i > 0) resultado.append(' ');
            if (i > 0 && CONECTORES.contains(palabra.toLowerCase(ES))) {
                resultado.append(palabra.toLowerCase(ES));
            } else if (!palabra.isEmpty()) {
                resultado.append(palabra.substring(0, 1).toUpperCase(ES)).append(palabra.substring(1));
            }
        }
        return resultado + aclaracion;
    }
}
