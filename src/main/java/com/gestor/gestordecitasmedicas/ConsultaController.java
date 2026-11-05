package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorPDF;
import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Cita;
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
        final HBox contenedor;
        final TextField nombre;
        final TextField dosis;

        FilaMedicamento(HBox contenedor, TextField nombre, TextField dosis) {
            this.contenedor = contenedor;
            this.nombre = nombre;
            this.dosis = dosis;
        }
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
        this.paciente = paciente;
        this.signos = signos;
        this.receta = cita == null ? null : baseDatos.cargarRecetaPorCita(cita.getIdCita());

        medicamentosBox.getChildren().clear();
        filas.clear();
        txtDiagnostico.clear();
        txtEvolucion.clear();
        txtIndicaciones.clear();

        if (receta != null) {
            if (receta.getSignosVitales() != null) {
                this.signos = receta.getSignosVitales();
            }
            txtDiagnostico.setText(receta.getDiagnostico() == null ? "" : receta.getDiagnostico());
            for (Medicamento medicamento : receta.getMedicamentos()) {
                agregarFila(medicamento.getNombreMedicamento(), medicamento.getDosis());
            }
        }
        if (filas.isEmpty()) {
            agregarFila("", "");
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
        segundos = 0;
        reloj.pause();
        medicamentosBox.getChildren().clear();
        filas.clear();
        txtDiagnostico.clear();
        txtEvolucion.clear();
        txtIndicaciones.clear();
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
                + new SimpleDateFormat("dd 'de' MMMM 'de' yyyy").format(new Date()));
    }

    private void tick() {
        segundos++;
        lblTimer.setText(String.format("%02d:%02d min", segundos / 60, segundos % 60));
    }

    @FXML
    private void onAgregarMedicamento() {
        agregarFila("", "");
    }

    private void agregarFila(String nombre, String dosis) {
        HBox contenedor = new HBox(12);
        contenedor.getStyleClass().add("med-row");

        VBox columnaNombre = new VBox(4);
        HBox.setHgrow(columnaNombre, Priority.ALWAYS);
        Label etiquetaNombre = new Label("MEDICAMENTO");
        etiquetaNombre.getStyleClass().add("caption-blue");
        TextField campoNombre = new TextField(nombre == null ? "" : nombre);
        campoNombre.setPromptText("Nombre genérico / comercial / presentación...");
        campoNombre.getStyleClass().add("search-field");
        columnaNombre.getChildren().addAll(etiquetaNombre, campoNombre);

        VBox columnaDosis = new VBox(4);
        HBox.setHgrow(columnaDosis, Priority.ALWAYS);
        Label etiquetaDosis = new Label("DOSIS Y POSOLOGÍA");
        etiquetaDosis.getStyleClass().add("caption");
        TextField campoDosis = new TextField(dosis == null ? "" : dosis);
        campoDosis.setPromptText("Dosis, frecuencia y duración...");
        campoDosis.getStyleClass().add("search-field");
        columnaDosis.getChildren().addAll(etiquetaDosis, campoDosis);

        FilaMedicamento fila = new FilaMedicamento(contenedor, campoNombre, campoDosis);
        Button eliminar = new Button();
        eliminar.getStyleClass().addAll("btn-icon", "btn-icon-danger");
        eliminar.setGraphic(new FontIcon("fi-rr-trash"));
        eliminar.setOnAction(e -> {
            medicamentosBox.getChildren().remove(contenedor);
            filas.remove(fila);
            actualizarContador();
        });

        contenedor.getChildren().addAll(columnaNombre, columnaDosis, eliminar);
        filas.add(fila);
        medicamentosBox.getChildren().add(contenedor);
        actualizarContador();
    }

    private void actualizarContador() {
        int total = 0;
        for (FilaMedicamento fila : filas) {
            if (!fila.nombre.getText().trim().isEmpty() || !fila.dosis.getText().trim().isEmpty()) {
                total++;
            }
        }
        lblMedicamentos.setText(total + (total == 1 ? " fármaco añadido" : " fármacos añadidos"));
    }

    @FXML
    private void onGuardarPdf() {
        if (!guardarReceta()) {
            return;
        }
        if (controladorPDF.generarPDF(receta)) {
            File archivo = new File(new File(ControladorPDF.CARPETA_SALIDA),
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
        if (!guardarReceta()) {
            return;
        }
        cita.setEstado(Estado.ATENDIDA);
        if (!baseDatos.actualizarCita(cita)) {
            aviso(Alert.AlertType.ERROR, "No se pudo finalizar",
                    "La receta se guardó, pero la cita no se pudo marcar como atendida.");
            return;
        }
        aviso(Alert.AlertType.INFORMATION, "Consulta finalizada",
                "La cita #" + cita.getIdCita() + " se marcó como atendida.");
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

    private boolean guardarReceta() {
        if (cita == null) {
            aviso(Alert.AlertType.INFORMATION, "Sin cita",
                    "Selecciona una cita desde la agenda para capturar la receta.");
            return false;
        }

        Receta destino = receta != null ? receta : new Receta();
        destino.setIdCita(cita.getIdCita());
        destino.setCurp(cita.getCurp());
        destino.setFecha(new Date());
        destino.setDiagnostico(diagnostico());
        destino.setMedicamentos(medicamentos());
        destino.setSignosVitales(signos);

        boolean guardado = destino.getIdReceta() > 0
                ? baseDatos.actualizarReceta(destino)
                : baseDatos.insertarReceta(destino);

        if (guardado) {
            receta = destino;
            return true;
        }
        aviso(Alert.AlertType.ERROR, "No se pudo guardar",
                "La receta no se guardó en la base de datos.");
        return false;
    }

    private String diagnostico() {
        List<String> partes = new ArrayList<>();
        String principal = txtDiagnostico.getText().trim();
        String evolucion = txtEvolucion.getText().trim();
        String indicaciones = txtIndicaciones.getText().trim();
        if (!principal.isEmpty()) partes.add(principal);
        if (!evolucion.isEmpty()) partes.add("Evolución: " + evolucion);
        if (!indicaciones.isEmpty()) partes.add("Indicaciones: " + indicaciones);
        return String.join("\n", partes);
    }

    private ArrayList<Medicamento> medicamentos() {
        ArrayList<Medicamento> medicamentos = new ArrayList<>();
        for (FilaMedicamento fila : filas) {
            String nombre = fila.nombre.getText() == null ? "" : fila.nombre.getText().trim();
            String dosis = fila.dosis.getText() == null ? "" : fila.dosis.getText().trim();
            if (nombre.isEmpty() && dosis.isEmpty()) {
                continue;
            }
            Medicamento medicamento = new Medicamento();
            medicamento.setNombreMedicamento(nombre);
            medicamento.setDosis(dosis);
            medicamentos.add(medicamento);
        }
        return medicamentos;
    }

    private static int edad(Paciente paciente) {
        if (paciente == null || paciente.getFechaNacimiento() == null) {
            return 0;
        }
        LocalDate nacimiento = paciente.getFechaNacimiento().toInstant()
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
