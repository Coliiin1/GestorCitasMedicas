package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.servicio.ControladorPDF;
import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.Consulta;
import com.gestor.gestordecitasmedicas.servicio.ValidacionClinica;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import com.gestor.gestordecitasmedicas.modelo.Medicamento;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import com.gestor.gestordecitasmedicas.modelo.Receta;
import com.gestor.gestordecitasmedicas.modelo.SignosVitales;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.gestor.gestordecitasmedicas.servicio.TextoExpediente;
import java.util.stream.Stream;


public class ConsultaController implements PanelControlado, PanelRefrescable {
    @FXML private Label lblEstado;
    @FXML private HBox timerBox;
    @FXML private Label lblPaciente, lblResumen, lblFechaEmision, lblMedicamentos, lblTimer;
    @FXML private TextField txtDiagnostico;
    @FXML private TextArea txtEvolucion, txtIndicaciones;
    @FXML private VBox medicamentosBox;
    @FXML private Button btnHistorial, btnVolver, btnGuardarPdf, btnFinalizar, btnAgregarMedicamento;

    @FXML private TextArea txtPadecimiento, txtExploracion, txtEstudios;
    @FXML private TextField txtNombrePaciente, txtEdadPaciente, txtSexoPaciente;
    private Consulta consulta;

    private final ControladorPDF controladorPDF = new ControladorPDF();
    private final List<FilaMedicamento> filas = new ArrayList<>();
    private final Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
    private int segundos;

    private MainController mainController;
    private Cita cita;
    private Paciente paciente;
    private SignosVitales signos;
    private Receta receta;
    private boolean modoEdicion;

    private static final class FilaMedicamento {
        final VBox contenedor = new VBox(8);
        final TextField nombre = new TextField();
        final TextField distintiva = new TextField();
        final TextField dosis = new TextField();
        final TextField duracion = new TextField();
        final TextField cantidad = new TextField();
        final ComboBox<String> grupo = new ComboBox<>();
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
        limpiar();
    }

    /** Abre una consulta ya registrada para editarla; conserva su vínculo con la cita, nota y receta. */
    public boolean cargarConsultaParaEdicion(Consulta guardada) {
        Cita citaGuardada = guardada == null ? null : Cita.buscar(guardada.getIdCita());
        if (citaGuardada == null) return false;
        cargarConsulta(citaGuardada, guardada.getSignosVitales());
        aplicarModoEdicion(true);
        return true;
    }

    private void aplicarModoEdicion(boolean activo) {
        modoEdicion = activo;
        lblEstado.setText(activo ? "En edición" : "En Atención");
        timerBox.setVisible(!activo);
        timerBox.setManaged(!activo);
        if (activo) reloj.stop();
        btnFinalizar.setText(activo ? "Actualizar consulta" : "Finalizar y preparar para firma");
        btnVolver.setText(activo ? "Volver al expediente" : "Volver a Signos Vitales");
    }

    /** El paciente se toma siempre de la base de datos según la CURP de la cita. */
    public void cargarConsulta(Cita cita, SignosVitales signos) {
        aplicarModoEdicion(false);
        this.cita = cita;
        this.paciente = cita == null ? null : Paciente.buscar(cita.getCurp());
        this.signos = signos;
        this.receta = cita == null ? null : Receta.deCita(cita.getIdCita());
        this.consulta = cita == null ? null : Consulta.deCita(cita.getIdCita());
        limpiarCampos();

        if (receta != null) {
            if (receta.getSignosVitales() != null) {
                this.signos = receta.getSignosVitales();
            }
            txtDiagnostico.setText(receta.getDiagnostico() == null ? "" : receta.getDiagnostico());
            txtIndicaciones.setText(receta.getIndicaciones());
            for (Medicamento medicamento : receta.getMedicamentos()) {
                agregarFila(medicamento);
            }
        }
        if (paciente != null) {
            Date fechaIdentidad = receta == null || receta.getFecha() == null ? new Date() : receta.getFecha();
            txtNombrePaciente.setText(receta != null && receta.getNombrePaciente() != null ? receta.getNombrePaciente() : Paciente.nombreParaMostrar(paciente, cita.getCurp()));
            txtEdadPaciente.setText(receta != null && receta.getEdadPaciente() != null ? receta.getEdadPaciente() : (paciente.getFechaNacimiento() == null ? "" : TextoExpediente.edad(paciente, fechaIdentidad)));
            txtSexoPaciente.setText(receta != null && receta.getSexoPaciente() != null ? receta.getSexoPaciente() : paciente.getSexo());
        }
        if (consulta != null) {
            txtPadecimiento.setText(consulta.getPadecimiento());
            txtEvolucion.setText(consulta.getEvolucion());
            txtExploracion.setText(consulta.getExploracionFisica());
            txtEstudios.setText(consulta.getResultadosEstudios());
            txtIndicaciones.setText(consulta.getIndicaciones());
        }
        if (filas.isEmpty()) {
            agregarFila(new Medicamento());
        }

        segundos = 0;
        tick();
        if (reloj.getStatus() != Animation.Status.RUNNING) {
            reloj.play();
        }
        actualizarCabecera();
        actualizarContador();
        habilitar(cita != null);
    }

    public boolean enCurso() {
        return cita != null;
    }

    /** Descarta la atención en curso (sin guardar). */
    public void abandonar() {
        limpiar();
    }

    @Override
    public void refrescar() {
        if (cita == null) {
            limpiar();
            return;
        }
        actualizarCabecera();
        actualizarContador();
        habilitar(true);
    }

    private void limpiar() {
        aplicarModoEdicion(false);
        cita = null;
        paciente = null;
        signos = null;
        receta = null;
        consulta = null;
        segundos = 0;
        reloj.pause();
        limpiarCampos();
        lblPaciente.setText("Sin cita seleccionada");
        lblResumen.setText("Selecciona una cita desde la agenda para iniciar la consulta");
        lblFechaEmision.setText("Fecha de emisión: --");
        lblMedicamentos.setText("0 fármacos añadidos");
        lblTimer.setText("00:00 min");
        habilitar(false);
    }

    private void limpiarCampos() {
        medicamentosBox.getChildren().clear();
        filas.clear();
        txtDiagnostico.clear();
        txtEvolucion.clear();
        txtIndicaciones.clear();
        txtPadecimiento.clear();
        txtExploracion.clear();
        txtEstudios.clear();
        txtNombrePaciente.clear();
        txtEdadPaciente.clear();
        txtSexoPaciente.clear();
    }

    private void habilitar(boolean activo) {
        if (btnHistorial != null) btnHistorial.setDisable(!activo);
        if (btnVolver != null) btnVolver.setDisable(!activo);
        if (btnGuardarPdf != null) btnGuardarPdf.setDisable(!activo);
        if (btnFinalizar != null) btnFinalizar.setDisable(!activo);
        if (btnAgregarMedicamento != null) btnAgregarMedicamento.setDisable(!activo);
    }

    private void actualizarCabecera() {
        if (cita == null) {
            return;
        }
        lblPaciente.setText(Paciente.nombreParaMostrar(paciente, cita.getCurp()));
        String edad = paciente == null ? "--" : String.valueOf(edad(paciente));
        String hora = cita.getFechaCita() == null ? "--"
                : new SimpleDateFormat("HH:mm").format(cita.getFechaCita());
        lblResumen.setText("Edad: " + edad + " años  •  Hora de inicio: " + hora
                + "  •  Cita #" + cita.getIdCita());
        lblFechaEmision.setText("Fecha de emisión: "
                + new SimpleDateFormat("dd 'de' MMMM 'de' yyyy").format(
                        receta != null && receta.getFecha() != null ? receta.getFecha() : new Date()));
    }

    private void tick() {
        segundos++;
        lblTimer.setText(String.format("%02d:%02d min", segundos / 60, segundos % 60));
    }

    @FXML
    private void onAgregarMedicamento() {
        agregarFila(new Medicamento());
    }

    private void agregarFila(Medicamento m) {
        FilaMedicamento fila = new FilaMedicamento();
        fila.contenedor.getStyleClass().add("med-row");
        GridPane campos = new GridPane();
        campos.setHgap(12);
        campos.setVgap(8);
        fila.nombre.setText(valor(m.getNombreMedicamento()));
        fila.nombre.getStyleClass().add("search-field");
        fila.nombre.setPromptText("Ej. Amoxicilina 500 mg tabletas");
        VBox col0 = new VBox(4, etiqueta("Nombre del medicamento"), fila.nombre);
        GridPane.setHgrow(col0, Priority.ALWAYS);
        campos.add(col0, 0, 0);
        // Recetas antiguas: se fusionan concentración, vía y frecuencia en el campo único para no perder datos.
        fila.dosis.setText(String.join(" ", Stream.of(m.getConcentracion(), m.getDosis(), m.getViaAdministracion(), m.getFrecuencia())
                .map(ConsultaController::valor).filter(s -> !s.isEmpty()).toArray(String[]::new)));
        fila.dosis.getStyleClass().add("search-field");
        fila.dosis.setPromptText("Ej. 1 tableta de 500 mg por vía oral cada 8 horas");
        VBox col1 = new VBox(4, etiqueta("Dosis y posología"), fila.dosis);
        GridPane.setHgrow(col1, Priority.ALWAYS);
        campos.add(col1, 1, 0);
        fila.duracion.setText(valor(m.getDuradcion()));
        fila.duracion.getStyleClass().add("search-field");
        fila.duracion.setPromptText("Ej. 7 días");
        VBox col2 = new VBox(4, etiqueta("Duración del tratamiento"), fila.duracion);
        GridPane.setHgrow(col2, Priority.ALWAYS);
        campos.add(col2, 2, 0);

        fila.distintiva.setText(valor(m.getDenominacionDistintiva()));
        fila.distintiva.getStyleClass().add("search-field");
        fila.distintiva.setPromptText("Ej. Amoxil");
        VBox colMarca = new VBox(4, etiqueta("Marca"), fila.distintiva);
        fila.cantidad.setText(valor(m.getCantidad()));
        fila.cantidad.getStyleClass().add("search-field");
        fila.cantidad.setPromptText("Ej. 1 caja con 21 tabletas");
        VBox colCantidad = new VBox(4, etiqueta("Cantidad a dispensar (con unidad)"), fila.cantidad);
        fila.grupo.getItems().addAll("I", "II", "III", "IV", "V", "VI");
        fila.grupo.setPromptText("Ej. IV (antibióticos y otros de receta ordinaria)");
        fila.grupo.getStyleClass().add("combo-field");
        fila.grupo.setMaxWidth(Double.MAX_VALUE);
        fila.grupo.setValue(m.getGrupoRegulatorio());
        Label alcance = new Label("Receta ordinaria: fracciones IV, V y VI. Para I, II o III use el procedimiento de control correspondiente fuera de este formato.");
        alcance.setWrapText(true);
        alcance.getStyleClass().add("hint");
        VBox extra = new VBox(8, colMarca, colCantidad, etiqueta("Tipo de receta (fracción, art. 226 de la Ley General de Salud)"), fila.grupo, alcance);
        boolean hayExtra = !valor(m.getDenominacionDistintiva()).isEmpty() || !valor(m.getCantidad()).isEmpty() || m.getGrupoRegulatorio() != null;
        extra.setVisible(hayExtra);
        extra.setManaged(hayExtra);
        Button toggle = new Button(hayExtra ? "Datos adicionales ▴" : "Datos adicionales ▾");
        toggle.getStyleClass().add("btn-secondary");
        toggle.setOnAction(e -> {
            boolean mostrar = !extra.isVisible();
            extra.setVisible(mostrar);
            extra.setManaged(mostrar);
            toggle.setText(mostrar ? "Datos adicionales ▴" : "Datos adicionales ▾");
        });
        Button eliminar = new Button("Eliminar medicamento");
        eliminar.getStyleClass().addAll("btn-icon", "btn-icon-danger");
        // btn-icon fija 36 px de ancho (pensado para iconos); con texto se colapsaba, así que se libera el ancho.
        eliminar.setStyle("-fx-min-width: USE_PREF_SIZE; -fx-pref-width: USE_COMPUTED_SIZE; -fx-max-width: USE_PREF_SIZE; -fx-padding: 0 12;");        eliminar.setOnAction(e -> {
            medicamentosBox.getChildren().remove(fila.contenedor);
            filas.remove(fila);
            actualizarContador();
        });
        fila.contenedor.getChildren().addAll(campos, toggle, extra, eliminar);
        fila.nombre.textProperty().addListener((o, a, b) -> actualizarContador());
        fila.dosis.textProperty().addListener((o, a, b) -> actualizarContador());
        filas.add(fila);
        medicamentosBox.getChildren().add(fila.contenedor);
        actualizarContador();
    }

    private void actualizarContador() {
        int total = 0;
        for (FilaMedicamento fila : filas) {
            if (!valor(fila.nombre.getText()).isEmpty() || !valor(fila.dosis.getText()).isEmpty()) {
                total++;
            }
        }
        lblMedicamentos.setText(total + (total == 1 ? " fármaco añadido" : " fármacos añadidos"));
    }

    @FXML
    private void onGuardarPdf() {
        if (!guardarAtencion(false, true)) {
            return;
        }
        if (controladorPDF.generarPDF(receta)) {
            File archivo = new File(new File(ControladorPDF.carpetaSalida()),
                    "receta_" + receta.getIdReceta() + ".pdf");
            ControladorPDF.abrir(archivo);
            Notificador.exito("Receta guardada",
                    "La receta se guardó y el PDF se generó en:\n" + archivo.getAbsolutePath());
        } else {
            Notificador.error("PDF no generado",
                    "La receta se guardó, pero no se pudo generar el PDF.");
        }
    }

    @FXML
    private void onFinalizar() {
        if (!guardarAtencion(true, false)) return;
        if (modoEdicion) {
            Notificador.exito("Consulta actualizada", "Los cambios se guardaron en la consulta.");
            Paciente actual = paciente;
            limpiar();
            if (mainController != null && actual != null) {
                ExpedienteController expediente = mainController.controlador("expediente-panel.fxml");
                expediente.mostrarPaciente(actual);
                mainController.ir("expediente-panel.fxml");
            }
            return;
        }
        Notificador.exito("Consulta finalizada",
                "La nota y la receta se guardaron. Imprima el expediente y la receta para que el profesional los firme de forma autógrafa.");
        limpiar();
        if (mainController != null) {
            mainController.ir("agenda-panel.fxml");
        }
    }

    @FXML
    private void onHistorial() {
        if (paciente == null || mainController == null) {
            return;
        }
        ExpedienteController controlador = mainController.controlador("expediente-panel.fxml");
        controlador.mostrarPaciente(paciente);
        mainController.ir("expediente-panel.fxml");
    }

    @FXML
    private void onVolver() {
        if (modoEdicion) {
            onHistorial();
            return;
        }
        if (mainController != null) {
            mainController.ir("cita-panel.fxml");
        }
    }

    private boolean guardarAtencion(boolean finalizar, boolean requiereReceta) {
        if (cita == null || paciente == null) return false;
        Receta destino = receta == null ? new Receta() : receta;
        destino.setIdCita(cita.getIdCita());
        destino.setCurp(cita.getCurp());
        if (destino.getFecha() == null) destino.setFecha(new Date());
        destino.setDiagnostico(valor(txtDiagnostico.getText()));
        destino.setIndicaciones(valor(txtIndicaciones.getText()));
        destino.setMedicamentos(medicamentos());
        destino.setSignosVitales(signos);
        destino.setNombrePaciente(valor(txtNombrePaciente.getText()));
        destino.setEdadPaciente(valor(txtEdadPaciente.getText()));
        destino.setSexoPaciente(valor(txtSexoPaciente.getText()));
        if (destino.getIdReceta() == 0 || valor(destino.getCedulaMedico()).isEmpty()) {
            try {
                destino.cargarDatosPrescriptor();
            } catch (IllegalStateException ex) {
                Notificador.error("Datos del prescriptor no disponibles", ex.getMessage());
                return false;
            }
        }
        Consulta nota = consulta == null ? new Consulta() : consulta;
        nota.setIdCita(cita.getIdCita());
        nota.setMotivo("Consulta externa");
        nota.setPadecimiento(valor(txtPadecimiento.getText()));
        nota.setEvolucion(valor(txtEvolucion.getText()));
        nota.setExploracionFisica(valor(txtExploracion.getText()));
        nota.setResultadosEstudios(valor(txtEstudios.getText()));
        nota.setDiagnostico(destino.getDiagnostico());
        nota.setIndicaciones(destino.getIndicaciones());
        nota.setNombreMedico(destino.getNombreMedico());
        nota.setCedulaMedico(destino.getCedulaMedico());
        nota.setNombrePaciente(destino.getNombrePaciente());
        nota.setEdadPaciente(destino.getEdadPaciente());
        nota.setSexoPaciente(destino.getSexoPaciente());
        nota.setSignosVitales(signos);
        nota.setReceta(destino);
        List<String> errores = ValidacionClinica.atencion(nota, destino, requiereReceta);
        if (!errores.isEmpty()) {
            Notificador.advertencia("Datos clínicos incompletos", String.join("\n", errores));
            return false;
        }
        if (!nota.guardarAtencion(cita, destino, finalizar)) {
            Notificador.error("No se pudo guardar", "La nota y la receta no se guardaron. Revise la conexión y vuelva a intentar.");
            return false;
        }
        receta = destino;
        consulta = nota;
        return true;
    }

    private ArrayList<Medicamento> medicamentos() {
        ArrayList<Medicamento> lista = new ArrayList<>();
        for (FilaMedicamento fila : filas) {
            if (valor(fila.nombre.getText()).isEmpty() && valor(fila.distintiva.getText()).isEmpty() && valor(fila.dosis.getText()).isEmpty() && valor(fila.duracion.getText()).isEmpty() && valor(fila.cantidad.getText()).isEmpty() && fila.grupo.getValue() == null) continue;
            Medicamento m = new Medicamento();
            m.setNombreMedicamento(valor(fila.nombre.getText()));
            m.setDenominacionDistintiva(valor(fila.distintiva.getText()));
            m.setDosis(valor(fila.dosis.getText()));
            m.setDuradcion(valor(fila.duracion.getText()));
            m.setCantidad(valor(fila.cantidad.getText()));
            m.setGrupoRegulatorio(fila.grupo.getValue());
            lista.add(m);
        }
        return lista;
    }

    private static int edad(Paciente paciente) {
        if (paciente == null || paciente.getFechaNacimiento() == null) {
            return 0;
        }
        LocalDate nacimiento = new Date(paciente.getFechaNacimiento().getTime()).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return Period.between(nacimiento, LocalDate.now()).getYears();
    }

    private static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("caption");
        return l;
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
