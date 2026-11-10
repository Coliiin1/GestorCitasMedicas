package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorPDF;
import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.Consulta;
import com.gestor.gestordecitasmedicas.controlador.ValidacionClinica;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import com.gestor.gestordecitasmedicas.modelo.Estado;
import com.gestor.gestordecitasmedicas.modelo.Medicamento;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import com.gestor.gestordecitasmedicas.modelo.Receta;
import com.gestor.gestordecitasmedicas.modelo.SignosVitales;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConsultaController implements PanelControlado, PanelRefrescable {
    @FXML private Label lblPaciente, lblResumen, lblFechaEmision, lblMedicamentos, lblTimer;
    @FXML private TextField txtDiagnostico;
    @FXML private TextArea txtEvolucion, txtIndicaciones;
    @FXML private VBox medicamentosBox;
    @FXML private Button btnHistorial, btnVolver, btnGuardarPdf, btnFinalizar, btnAgregarMedicamento;

    @FXML private TextArea txtPadecimiento, txtExploracion, txtEstudios, txtPronostico;
    @FXML private TextField txtNombrePaciente, txtEdadPaciente, txtSexoPaciente;
    private Consulta consulta;

    private final BaseDatos baseDatos = new BaseDatos();
    private final ControladorPDF controladorPDF = new ControladorPDF();
    private final List<FilaMedicamento> filas = new ArrayList<>();
    private final Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
    private int segundos;

    private MainController mainController;
    private Cita cita;
    private Paciente paciente;
    private SignosVitales signos;
    private Receta receta;

    private static final class FilaMedicamento {
        final VBox contenedor = new VBox(8);
        final TextField nombre = new TextField();
        final TextField distintiva = new TextField();
        final TextField concentracion = new TextField();
        final TextField presentacion = new TextField();
        final TextField dosis = new TextField();
        final TextField via = new TextField();
        final TextField frecuencia = new TextField();
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

    public void cargarConsulta(Cita cita, Paciente paciente, SignosVitales signos) {
        this.cita = cita;
        this.paciente = cita == null ? null : baseDatos.cargarPacientePorCurp(cita.getCurp());
        paciente = this.paciente;
        this.signos = signos;
        this.receta = cita == null ? null : baseDatos.cargarRecetaPorCita(cita.getIdCita());
        this.consulta = cita == null ? null : baseDatos.cargarConsultaPorCita(cita.getIdCita());

        medicamentosBox.getChildren().clear();
        filas.clear();
        txtDiagnostico.clear();
        txtEvolucion.clear();
        txtIndicaciones.clear();
        txtNombrePaciente.clear();
        txtEdadPaciente.clear();
        txtSexoPaciente.clear();
        txtPadecimiento.clear();
        txtExploracion.clear();
        txtEstudios.clear();
        txtPronostico.clear();

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
            txtNombrePaciente.setText(receta != null && receta.getNombrePaciente() != null ? receta.getNombrePaciente() : nombrePaciente(paciente, cita.getCurp()));
            txtEdadPaciente.setText(receta != null && receta.getEdadPaciente() != null ? receta.getEdadPaciente() : (paciente.getFechaNacimiento() == null ? "" : com.gestor.gestordecitasmedicas.controlador.TextoExpediente.edad(paciente, fechaIdentidad)));
            txtSexoPaciente.setText(receta != null && receta.getSexoPaciente() != null ? receta.getSexoPaciente() : paciente.getSexo());
        }
        if (consulta != null) {
            txtPadecimiento.setText(consulta.getPadecimiento());
            txtEvolucion.setText(consulta.getEvolucion());
            txtExploracion.setText(consulta.getExploracionFisica());
            txtEstudios.setText(consulta.getResultadosEstudios());
            txtPronostico.setText(consulta.getPronostico());
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
        cita = null;
        paciente = null;
        signos = null;
        receta = null;
        consulta = null;
        segundos = 0;
        reloj.pause();
        medicamentosBox.getChildren().clear();
        filas.clear();
        txtDiagnostico.clear();
        txtEvolucion.clear();
        txtIndicaciones.clear();
        txtPadecimiento.clear();
        txtExploracion.clear();
        txtEstudios.clear();
        txtPronostico.clear();
        txtNombrePaciente.clear();
        txtEdadPaciente.clear();
        txtSexoPaciente.clear();
        lblPaciente.setText("Sin cita seleccionada");
        lblResumen.setText("Selecciona una cita desde la agenda para iniciar la consulta");
        lblFechaEmision.setText("Fecha de emisión: --");
        lblMedicamentos.setText("0 fármacos añadidos");
        lblTimer.setText("00:00 min");
        habilitar(false);
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
        lblPaciente.setText(nombrePaciente(paciente, cita.getCurp()));
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
        VBox col0 = new VBox(4, new Label("Denominación genérica"), fila.nombre);
        GridPane.setHgrow(col0, Priority.ALWAYS);
        campos.add(col0, 0, 0);
        fila.distintiva.setText(valor(m.getDenominacionDistintiva()));
        fila.distintiva.getStyleClass().add("search-field");
        VBox col1 = new VBox(4, new Label("Marca (opcional)"), fila.distintiva);
        GridPane.setHgrow(col1, Priority.ALWAYS);
        campos.add(col1, 1, 0);
        fila.concentracion.setText(valor(m.getConcentracion()));
        fila.concentracion.getStyleClass().add("search-field");
        VBox col2 = new VBox(4, new Label("Concentración (con unidad)"), fila.concentracion);
        GridPane.setHgrow(col2, Priority.ALWAYS);
        campos.add(col2, 2, 0);
        fila.presentacion.setText(valor(m.getPresentacion()));
        fila.presentacion.getStyleClass().add("search-field");
        VBox col3 = new VBox(4, new Label("Presentación / forma farmacéutica"), fila.presentacion);
        GridPane.setHgrow(col3, Priority.ALWAYS);
        campos.add(col3, 0, 1);
        fila.dosis.setText(valor(m.getDosis()));
        fila.dosis.getStyleClass().add("search-field");
        VBox col4 = new VBox(4, new Label("Dosis (con unidad)"), fila.dosis);
        GridPane.setHgrow(col4, Priority.ALWAYS);
        campos.add(col4, 1, 1);
        fila.via.setText(valor(m.getViaAdministracion()));
        fila.via.getStyleClass().add("search-field");
        VBox col5 = new VBox(4, new Label("Vía de administración"), fila.via);
        GridPane.setHgrow(col5, Priority.ALWAYS);
        campos.add(col5, 2, 1);
        fila.frecuencia.setText(valor(m.getFrecuencia()));
        fila.frecuencia.getStyleClass().add("search-field");
        VBox col6 = new VBox(4, new Label("Frecuencia"), fila.frecuencia);
        GridPane.setHgrow(col6, Priority.ALWAYS);
        campos.add(col6, 0, 2);
        fila.duracion.setText(valor(m.getDuradcion()));
        fila.duracion.getStyleClass().add("search-field");
        VBox col7 = new VBox(4, new Label("Duración del tratamiento"), fila.duracion);
        GridPane.setHgrow(col7, Priority.ALWAYS);
        campos.add(col7, 1, 2);
        fila.cantidad.setText(valor(m.getCantidad()));
        fila.cantidad.getStyleClass().add("search-field");
        VBox col8 = new VBox(4, new Label("Cantidad a dispensar (con unidad)"), fila.cantidad);
        GridPane.setHgrow(col8, Priority.ALWAYS);
        campos.add(col8, 2, 2);
        fila.grupo.getItems().addAll("I", "II", "III", "IV", "V", "VI");
        fila.grupo.setPromptText("Seleccione la fracción del medicamento");
        fila.grupo.setValue(m.getGrupoRegulatorio());
        Label alcance = new Label("Receta ordinaria: fracciones IV, V y VI. Para I, II o III use el procedimiento de control correspondiente fuera de este formato.");
        alcance.setWrapText(true);
        Button eliminar = new Button("Eliminar medicamento");
        eliminar.getStyleClass().addAll("btn-icon", "btn-icon-danger");
        eliminar.setOnAction(e -> {
            medicamentosBox.getChildren().remove(fila.contenedor);
            filas.remove(fila);
            actualizarContador();
        });
        fila.contenedor.getChildren().addAll(campos, new Label("Fracción del artículo 226 de la Ley General de Salud"), fila.grupo, alcance, eliminar);
        fila.nombre.textProperty().addListener((o, a, b) -> actualizarContador());
        fila.distintiva.textProperty().addListener((o, a, b) -> actualizarContador());
        fila.concentracion.textProperty().addListener((o, a, b) -> actualizarContador());
        fila.presentacion.textProperty().addListener((o, a, b) -> actualizarContador());
        fila.dosis.textProperty().addListener((o, a, b) -> actualizarContador());
        fila.via.textProperty().addListener((o, a, b) -> actualizarContador());
        fila.frecuencia.textProperty().addListener((o, a, b) -> actualizarContador());
        fila.duracion.textProperty().addListener((o, a, b) -> actualizarContador());
        fila.cantidad.textProperty().addListener((o, a, b) -> actualizarContador());
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
            aviso(Alert.AlertType.INFORMATION, "Receta guardada",
                    "La receta se guardó y el PDF se generó en:\n" + archivo.getAbsolutePath());
        } else {
            aviso(Alert.AlertType.ERROR, "PDF no generado",
                    "La receta se guardó, pero no se pudo generar el PDF.");
        }
    }

    @FXML
    private void onFinalizar() {
        if (!guardarAtencion(true, false)) return;
        aviso(Alert.AlertType.INFORMATION, "Consulta finalizada",
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
                baseDatos.cargarDatosPrescriptor(destino);
            } catch (IllegalStateException ex) {
                aviso(Alert.AlertType.ERROR, "Datos del prescriptor no disponibles", ex.getMessage());
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
        nota.setPronostico(valor(txtPronostico.getText()));
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
            aviso(Alert.AlertType.WARNING, "Datos clínicos incompletos", String.join("\n", errores));
            return false;
        }
        if (!baseDatos.guardarAtencion(cita, nota, destino, finalizar)) {
            aviso(Alert.AlertType.ERROR, "No se pudo guardar", "La nota y la receta no se guardaron. Revise la conexión y vuelva a intentar.");
            return false;
        }
        receta = destino;
        consulta = nota;
        return true;
    }

    private ArrayList<Medicamento> medicamentos() {
        ArrayList<Medicamento> lista = new ArrayList<>();
        for (FilaMedicamento fila : filas) {
            if (valor(fila.nombre.getText()).isEmpty() && valor(fila.distintiva.getText()).isEmpty() && valor(fila.concentracion.getText()).isEmpty() && valor(fila.presentacion.getText()).isEmpty() && valor(fila.dosis.getText()).isEmpty() && valor(fila.via.getText()).isEmpty() && valor(fila.frecuencia.getText()).isEmpty() && valor(fila.duracion.getText()).isEmpty() && valor(fila.cantidad.getText()).isEmpty() && fila.grupo.getValue() == null) continue;
            Medicamento m = new Medicamento();
            m.setNombreMedicamento(valor(fila.nombre.getText()));
            m.setDenominacionDistintiva(valor(fila.distintiva.getText()));
            m.setConcentracion(valor(fila.concentracion.getText()));
            m.setPresentacion(valor(fila.presentacion.getText()));
            m.setDosis(valor(fila.dosis.getText()));
            m.setViaAdministracion(valor(fila.via.getText()));
            m.setFrecuencia(valor(fila.frecuencia.getText()));
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

    private static String nombrePaciente(Paciente paciente, String curp) {
        if (paciente == null) {
            return curp == null ? "Paciente no registrado" : curp;
        }
        String nombre = (valor(paciente.getNombre()) + " " + valor(paciente.getApellidoPaterno())
                + " " + valor(paciente.getApellidoMaterno())).trim();
        return nombre.isEmpty() ? (curp == null ? "Paciente sin nombre" : curp) : nombre;
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private static void aviso(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
