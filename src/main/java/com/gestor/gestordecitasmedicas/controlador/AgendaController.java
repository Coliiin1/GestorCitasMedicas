package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.servicio.ControladorCorreo;
import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.DiaSemana;
import com.gestor.gestordecitasmedicas.modelo.Estado;
import com.gestor.gestordecitasmedicas.modelo.Horario;
import com.gestor.gestordecitasmedicas.modelo.HorarioBloqueado;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import com.gestor.gestordecitasmedicas.modelo.PlanificadorAgenda;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javafx.event.ActionEvent;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import static com.gestor.gestordecitasmedicas.controlador.Ui.comboHora;

public class AgendaController implements PanelControlado, PanelRefrescable {
    @FXML private GridPane calendario, semana;
    @FXML private TextField fichaNombre;
    @FXML private Label fichaEdad, fichaFecha, fichaHora, mesLabel, rangoLabel, resumenLabel;
    @FXML private Button btnIniciar, btnReprogramar, btnCancelar;
    private static final String TEXTO_INICIAR = "Iniciar Cita Clínica";
    @FXML private Button btnConfigHorario;

    private static final String[] DIAS_CAL = {"L", "M", "X", "J", "V", "S", "D"};
    private static final String[] NOMBRES_DIA_CORTO = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private final ArrayList<Horario> horarios = new ArrayList<>();
    private final ArrayList<HorarioBloqueado> bloqueos = new ArrayList<>();

    private MainController mainController;
    private YearMonth mes = YearMonth.now();
    private LocalDate seleccion = LocalDate.now();
    private Cita citaSeleccionada;
    private Paciente pacienteSeleccionado;
    private LocalDate fechaPendienteAlta;
    private LocalTime horaPendienteAlta;
    private String curpPendienteAlta;
    private boolean altaPendiente;

    private static final class Turno {
        final Cita cita;
        final Paciente paciente;

        Turno(Cita cita, Paciente paciente) {
            this.cita = cita;
            this.paciente = paciente;
        }
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        recargarHorarios();
        renderCalendario();
        renderSemana();
        limpiarFicha();
        mostrarBotonesAdmin(false);
    }

    @Override
    public void refrescar() {
        boolean admin = esAdmin();
        btnIniciar.setVisible(admin);
        btnIniciar.setManaged(admin);
        mostrarBotonesAdmin(admin);
        recargarHorarios();
        renderCalendario();
        renderSemana();
        if (citaSeleccionada != null) {
            Cita actualizada = Cita.buscar(citaSeleccionada.getIdCita());
            if (actualizada == null || actualizada.getFechaCita() == null) {
                limpiarFicha();
            } else {
                pacienteSeleccionado = Paciente.buscar(actualizada.getCurp());
                LocalDateTime fecha = actualizada.fechaHora();
                mostrarCita(new Turno(actualizada, pacienteSeleccionado),
                        fechaLarga(fecha.toLocalDate()), fecha.toLocalTime());
            }
        }
        if (altaPendiente) {
            altaPendiente = false;
            String curp = curpPendienteAlta;
            LocalDate fecha = fechaPendienteAlta;
            LocalTime hora = horaPendienteAlta;
            curpPendienteAlta = null;
            fechaPendienteAlta = null;
            horaPendienteAlta = null;
            if (curp != null) {
                LocalDate dia = fecha == null ? seleccion : fecha;
                List<LocalTime> disponibles = PlanificadorAgenda.iniciosDisponibles(dia, horarios, bloqueos);
                LocalTime inicio = hora != null ? hora : (disponibles.isEmpty() ? LocalTime.of(9, 0) : disponibles.get(0));
                dialogoCita(null, dia, inicio, curp);
            }
        }
    }

    /** Selecciona la cita en el calendario; ir("agenda-panel.fxml") la muestra en la ficha vía refrescar(). */
    public void seleccionarCita(Cita cita) {
        citaSeleccionada = cita;
        seleccion = cita.fechaHora().toLocalDate();
        mes = YearMonth.from(seleccion);
    }

    private void mostrarBotonesAdmin(boolean visible) {
        if (btnConfigHorario != null) {
            btnConfigHorario.setVisible(visible);
            btnConfigHorario.setManaged(visible);
        }
        // Bloquear día/semana: disponible para administrador y asistente.
    }

    private boolean esAdmin() {
        return mainController != null && mainController.esAdministrador();
    }

    private void recargarHorarios() {
        horarios.clear();
        horarios.addAll(Horario.activos());
        bloqueos.clear();
        bloqueos.addAll(HorarioBloqueado.todos());
    }

    private void renderCalendario() {
        calendario.getChildren().clear();
        mesLabel.setText(PlanificadorAgenda.MESES[mes.getMonthValue() - 1] + " " + mes.getYear());
        for (int c = 0; c < DIAS_CAL.length; c++) calendario.add(celdaCalendario(DIAS_CAL[c], "cal-head"), c, 0);

        LocalDate dia = mes.atDay(1).minusDays(mes.atDay(1).getDayOfWeek().getValue() - 1);
        for (int i = 0; i < 42; i++, dia = dia.plusDays(1)) {
            LocalDate fecha = dia;
            String estilo;
            if (fecha.equals(seleccion)) {
                estilo = "cal-sel";
            } else if (fecha.getMonthValue() != mes.getMonthValue()) {
                estilo = "cal-other";
            } else if (PlanificadorAgenda.diaBloqueado(fecha, horarios, bloqueos)) {
                estilo = "cal-blocked";
            } else if (!PlanificadorAgenda.esDiaHabil(fecha, horarios)) {
                estilo = "cal-off";
            } else {
                estilo = "cal-day";
            }
            Label celda = celdaCalendario(String.valueOf(fecha.getDayOfMonth()), estilo);
            celda.setCursor(Cursor.HAND);
            celda.setOnMouseClicked(e -> {
                seleccion = fecha;
                mes = YearMonth.from(fecha);
                limpiarFicha();
                renderCalendario();
                renderSemana();
            });
            calendario.add(celda, i % 7, 1 + i / 7);
        }
    }

    private void renderSemana() {
        LocalDate lunes = PlanificadorAgenda.lunesDe(seleccion);
        rangoLabel.setText("Semana del " + lunes.getDayOfMonth() + " al " + lunes.plusDays(6).getDayOfMonth()
                + " de " + PlanificadorAgenda.MESES[lunes.getMonthValue() - 1] + ", " + lunes.getYear());

        ArrayList<Cita> citasSemana = Cita.entre(lunes, lunes.plusDays(6));
        List<LocalDate> fechas = fechasSemana(lunes, citasSemana);
        List<LocalTime> horas = horasVista(citasSemana);

        semana.getChildren().clear();
        semana.getColumnConstraints().clear();
        ColumnConstraints columnaHora = new ColumnConstraints();
        columnaHora.setMinWidth(64);
        columnaHora.setPrefWidth(64);
        columnaHora.setMaxWidth(64);
        semana.getColumnConstraints().add(columnaHora);
        for (int i = 0; i < fechas.size(); i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setHgrow(Priority.ALWAYS);
            columna.setMinWidth(0);
            semana.getColumnConstraints().add(columna);
        }

        Label horario = new Label("HORARIO");
        horario.getStyleClass().add("caption");
        semana.add(horario, 0, 0);

        for (int c = 0; c < fechas.size(); c++) {
            LocalDate fecha = fechas.get(c);
            DiaSemana dia = DiaSemana.desdeDayOfWeek(fecha.getDayOfWeek());
            Label etiqueta = new Label((dia == null ? "" : dia.getEtiquetaCorta()) + "\n"
                    + fecha.getDayOfMonth() + " " + PlanificadorAgenda.MESES_CORTO[fecha.getMonthValue() - 1]);
            etiqueta.getStyleClass().addAll("day-head", fecha.equals(seleccion) ? "day-head-sel" : "day-head-off");
            etiqueta.setMaxWidth(Double.MAX_VALUE);
            semana.add(etiqueta, c + 1, 0);
        }

        if (horas.isEmpty()) {
            Label vacio = new Label("No hay horario configurado. " +
                    (esAdmin() ? "Usa \"Horario de trabajo\" para definir tus horas." : "Contacta al administrador."));
            vacio.getStyleClass().add("hint");
            vacio.setWrapText(true);
            GridPane.setColumnSpan(vacio, Math.max(1, fechas.size()));
            semana.add(vacio, 1, 1);
            resumenLabel.setText("Sin horario de trabajo configurado.");
            return;
        }

        Turno[][] datos = cargarTurnos(fechas, horas, citasSemana);
        for (int h = 0; h < horas.size(); h++) {
            Label hora = new Label(PlanificadorAgenda.formatoHora(horas.get(h)));
            hora.getStyleClass().add("info-value");
            semana.add(hora, 0, h + 1);
            for (int c = 0; c < fechas.size(); c++) {
                semana.add(celdaCita(datos[h][c], fechas.get(c), horas.get(h)), c + 1, h + 1);
            }
        }
        resumenLabel.setText(resumen(datos, fechas, horas));
    }

    private List<LocalDate> fechasSemana(LocalDate lunes, List<Cita> citasSemana) {
        Set<Integer> dias = new HashSet<>();
        for (Horario horario : horarios) {
            if (horario.isEstado() && horario.getDia() != null) {
                dias.add(horario.getDia().getNumero());
            }
        }
        for (Cita cita : citasSemana) {
            LocalDateTime fecha = cita.fechaHora();
            if (fecha != null) {
                dias.add(fecha.getDayOfWeek().getValue());
            }
        }
        if (dias.isEmpty()) {
            for (int i = 1; i <= 5; i++) {
                dias.add(i);
            }
        }

        List<LocalDate> fechas = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            if (dias.contains(d)) {
                fechas.add(lunes.plusDays(d - 1));
            }
        }
        return fechas;
    }

    private List<LocalTime> horasVista(List<Cita> citasSemana) {
        TreeSet<LocalTime> horas = new TreeSet<>(PlanificadorAgenda.horasDeTrabajo(horarios));
        for (Cita cita : citasSemana) {
            LocalDateTime fecha = cita.fechaHora();
            if (fecha != null) {
                horas.add(fecha.toLocalTime().withSecond(0).withNano(0));
            }
        }
        return new ArrayList<>(horas);
    }

    private Turno[][] cargarTurnos(List<LocalDate> fechas, List<LocalTime> horas, List<Cita> citasSemana) {
        Turno[][] turnos = new Turno[horas.size()][fechas.size()];
        HashMap<String, Paciente> pacientes = Paciente.todos();
        for (Cita cita : citasSemana) {
            LocalDateTime fecha = cita.fechaHora();
            if (fecha == null) {
                continue;
            }
            int dia = fechas.indexOf(fecha.toLocalDate());
            int hora = horas.indexOf(fecha.toLocalTime().withSecond(0).withNano(0));
            if (dia < 0 || hora < 0 || turnos[hora][dia] != null) {
                continue;
            }
            turnos[hora][dia] = new Turno(cita, pacientes.get(cita.getCurp()));
        }
        return turnos;
    }

    private String resumen(Turno[][] datos, List<LocalDate> fechas, List<LocalTime> horas) {
        int atendidas = 0, agendadas = 0, canceladas = 0, libres = 0;
        for (int h = 0; h < datos.length; h++) {
            for (int c = 0; c < datos[h].length; c++) {
                Turno turno = datos[h][c];
                if (turno == null) {
                    if (PlanificadorAgenda.esInicioDisponible(fechas.get(c), horas.get(h), horarios, bloqueos)) {
                        libres++;
                    }
                } else if (turno.cita.getEstado() == Estado.ATENDIDA) {
                    atendidas++;
                } else if (turno.cita.getEstado() == Estado.CANCELADA) {
                    canceladas++;
                } else {
                    agendadas++;
                }
            }
        }
        return "Resumen semanal: " + atendidas + " atendidas · " + agendadas + " agendadas · "
                + canceladas + " canceladas · " + libres + " libres";
    }

    @FXML
    private void onMesAnterior() {
        mes = mes.minusMonths(1);
        renderCalendario();
    }

    @FXML
    private void onMesSiguiente() {
        mes = mes.plusMonths(1);
        renderCalendario();
    }

    @FXML
    private void onSemanaAnterior() {
        moverSemana(-1);
    }

    @FXML
    private void onSemanaSiguiente() {
        moverSemana(1);
    }

    private void moverSemana(int semanas) {
        seleccion = seleccion.plusWeeks(semanas);
        mes = YearMonth.from(seleccion);
        limpiarFicha();
        renderCalendario();
        renderSemana();
    }

    private static String fechaLarga(LocalDate d) {
        return NOMBRES_DIA_CORTO[d.getDayOfWeek().getValue() - 1] + ", " + d.getDayOfMonth() + " "
                + PlanificadorAgenda.MESES_CORTO[d.getMonthValue() - 1] + " " + d.getYear();
    }

    private Label celdaCalendario(String texto, String estilo) {
        Label l = new Label(texto);
        l.getStyleClass().add(estilo);
        l.setAlignment(Pos.CENTER);
        l.setMinSize(0, 32);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Node celdaCita(Turno turno, LocalDate fecha, LocalTime hora) {
        if (turno == null) {
            boolean disponible = PlanificadorAgenda.esInicioDisponible(fecha, hora, horarios, bloqueos);
            if (!disponible) {
                LocalDateTime inicioSlot = LocalDateTime.of(fecha, hora);
                boolean slotBloqueado = PlanificadorAgenda.estaBloqueado(inicioSlot,
                        inicioSlot.plusMinutes(PlanificadorAgenda.DURACION_CITA_MINUTOS), bloqueos);
                Label cerrado = new Label(slotBloqueado
                        ? "Bloqueado" : "No laborable");
                cerrado.getStyleClass().add("slot-off");
                cerrado.setAlignment(Pos.CENTER);
                cerrado.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                cerrado.setMinWidth(0);
                return cerrado;
            }
            Button libre = new Button("+ Disponible");
            libre.getStyleClass().add("slot-free");
            libre.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            libre.setMinWidth(0);
            libre.setPrefWidth(0);
            libre.setOnAction(e -> dialogoCita(null, fecha, hora));
            return libre;
        }
        Label estado = new Label(Estado.etiqueta(turno.cita.getEstado()));
        estado.getStyleClass().add("caption");
        Label paciente = new Label(Paciente.nombreParaMostrar(turno.paciente, turno.cita.getCurp()));
        paciente.getStyleClass().add("info-value");
        Label referencia = new Label("Cita #" + turno.cita.getIdCita());
        referencia.getStyleClass().add("hint");
        VBox contenido = new VBox(2, estado, paciente, referencia);
        contenido.setMinWidth(0);

        Button celda = new Button();
        // Sin wrapText: dentro de un Button la etiqueta calculaba su alto con un ancho casi nulo y la fila se disparaba.
        // Se limita el ancho al de la celda para que el texto largo se recorte con "…".
        contenido.maxWidthProperty().bind(celda.widthProperty().subtract(24));
        celda.setMinWidth(0);
        celda.setPrefWidth(0);
        celda.setGraphic(contenido);
        celda.getStyleClass().addAll("slot", "slot-" + turno.cita.getEstado().name().toLowerCase());
        celda.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        celda.setOnAction(e -> mostrarCita(turno, fechaLarga(fecha), hora));
        return celda;
    }

    private void mostrarCita(Turno turno, String fecha, LocalTime hora) {
        citaSeleccionada = turno.cita;
        pacienteSeleccionado = turno.paciente;
        fichaNombre.setText(Paciente.nombreParaMostrar(turno.paciente, turno.cita.getCurp()));
        fichaEdad.setText("Edad: " + Paciente.edadTexto(turno.paciente) + " · " + Estado.etiqueta(turno.cita.getEstado()));
        fichaFecha.setText(fecha);
        fichaHora.setText(PlanificadorAgenda.formatoHora(hora) + " ("
                + PlanificadorAgenda.DURACION_CITA_MINUTOS + " min)");
        boolean cancelada = turno.cita.getEstado() == Estado.CANCELADA;
        boolean atendida = turno.cita.getEstado() == Estado.ATENDIDA;
        btnIniciar.setDisable(cancelada || atendida);
        btnIniciar.setText(atendida ? "Ya atendida" : TEXTO_INICIAR);
        btnReprogramar.setDisable(atendida);
        btnCancelar.setDisable(cancelada || atendida);
    }

    private void limpiarFicha() {
        citaSeleccionada = null;
        pacienteSeleccionado = null;
        fichaNombre.setText("Sin cita seleccionada");
        fichaEdad.setText("Selecciona una cita de la agenda");
        fichaFecha.setText("--");
        fichaHora.setText("--");
        if (btnIniciar != null) {
            btnIniciar.setDisable(true);
            btnIniciar.setText(TEXTO_INICIAR);
        }
        if (btnReprogramar != null) btnReprogramar.setDisable(true);
        if (btnCancelar != null) btnCancelar.setDisable(true);
    }

    @FXML
    private void onAgendarNueva() {
        List<LocalTime> disponibles = PlanificadorAgenda.iniciosDisponibles(seleccion, horarios, bloqueos);
        if (disponibles.isEmpty()) {
            Notificador.advertencia("Sin disponibilidad",
                    PlanificadorAgenda.diaBloqueado(seleccion, horarios, bloqueos)
                            ? "La fecha seleccionada está bloqueada."
                            : "No hay horario de trabajo configurado para la fecha seleccionada.");
            return;
        }
        dialogoCita(null, seleccion, disponibles.get(0));
    }

    @FXML
    private void onIniciarCita() {
        if (citaSeleccionada == null || !esAdmin() || citaSeleccionada.getEstado() == Estado.ATENDIDA) {
            return;
        }
        CitaController controlador = mainController.controlador("cita-panel.fxml");
        controlador.cargarCita(citaSeleccionada);
        mainController.ir("cita-panel.fxml");
    }

    @FXML
    private void onReprogramar() {
        if (citaSeleccionada == null || citaSeleccionada.getFechaCita() == null) {
            return;
        }
        LocalDateTime fecha = citaSeleccionada.fechaHora();
        dialogoCita(citaSeleccionada, fecha.toLocalDate(), fecha.toLocalTime());
    }

    @FXML
    private void onCancelarCita() {
        if (citaSeleccionada == null) {
            return;
        }
        if (!Confirmacion.destructiva("¿Cancelar la cita?", "Se cancelará la cita #" + citaSeleccionada.getIdCita()
                + " de " + Paciente.nombreParaMostrar(pacienteSeleccionado, citaSeleccionada.getCurp())
                + " y se notificará al paciente por correo.", "Cancelar cita")) {
            return;
        }
        if (!citaSeleccionada.cancelar()) {
            Notificador.error("No se pudo cancelar", "La cita no se actualizó en la base de datos.");
            return;
        }
        Cita cancelada = citaSeleccionada;
        limpiarFicha();
        refrescar();
        Notificador.exito("Cita cancelada", "La cita se marcó como cancelada.");
        ControladorCorreo.notificarCancelaciones(List.of(cancelada), Paciente.todos(), null,
                HorarioConfigController::avisoNotificacion);
    }

    @FXML
    private void onConfigurarHorario() {
        if (!esAdmin()) {
            return;
        }
        boolean cambios = HorarioConfigController.abrir(ventana());
        if (cambios) {
            refrescar();
        }
    }

    @FXML
    private void onBloquearDia() {
        if (dialogoBloqueo(seleccion, false)) {
            refrescar();
        }
    }

    @FXML
    private void onBloquearSemana() {
        if (dialogoBloqueo(PlanificadorAgenda.lunesDe(seleccion), true)) {
            refrescar();
        }
    }

    private void dialogoCita(Cita existente, LocalDate fechaInicial, LocalTime horaInicial) {
        dialogoCita(existente, fechaInicial, horaInicial, null);
    }

    private void dialogoCita(Cita existente, LocalDate fechaInicial, LocalTime horaInicial,
                             String curpPreseleccionada) {
        HashMap<String, Paciente> pacientesMapa = Paciente.todos();

        List<Paciente> pacientes = new ArrayList<>(pacientesMapa.values());
        pacientes.sort(Comparator.comparing(AgendaController::nombrePacienteOrden));

        Dialog<ButtonType> dialogo = new Dialog<>();
        if (ventana() != null) dialogo.initOwner(ventana());
        dialogo.setTitle(existente == null ? "Agendar nueva cita" : "Reprogramar cita");
        dialogo.getDialogPane().getStylesheets().add(
                AgendaController.class.getResource("/com/gestor/gestordecitasmedicas/cita.css").toExternalForm());
        dialogo.getDialogPane().getStyleClass().add("expediente-editor");
        Ui.tema(dialogo.getDialogPane());

        Label tituloEncabezado = new Label(existente == null ? "AGENDAR NUEVA CITA" : "REPROGRAMAR CITA");
        tituloEncabezado.getStyleClass().add("field-label");
        Label subtitulo = new Label(fechaLarga(fechaInicial));
        subtitulo.getStyleClass().add("panel-title");
        Label descripcion = new Label("Selecciona paciente, fecha y hora");
        descripcion.getStyleClass().add("card-subtitle");
        VBox encabezado = new VBox(4, tituloEncabezado, subtitulo, descripcion);
        encabezado.getStyleClass().add("edit-header");
        dialogo.getDialogPane().setHeader(encabezado);

        ComboBox<Paciente> comboPaciente = new ComboBox<>();
        comboPaciente.getItems().setAll(pacientes);
        comboPaciente.setConverter(new StringConverter<Paciente>() {
            @Override
            public String toString(Paciente paciente) {
                return paciente == null ? "" : Paciente.nombreParaMostrar(paciente, paciente.getCurp()) + " · " + paciente.getCurp();
            }

            @Override
            public Paciente fromString(String texto) {
                return null;
            }
        });
        comboPaciente.setPromptText(pacientes.isEmpty()
                ? "No hay pacientes registrados"
                : "Selecciona un paciente");
        comboPaciente.setMaxWidth(Double.MAX_VALUE);
        comboPaciente.getStyleClass().add("combo-field");

        DatePicker selectorFecha = new DatePicker(fechaInicial);
        selectorFecha.setMaxWidth(Double.MAX_VALUE);
        selectorFecha.getStyleClass().add("date-field");

        ComboBox<LocalTime> comboHora = comboHora(List.of(), null);
        LocalTime horaPreferida = existente != null ? existente.fechaHora().toLocalTime() : horaInicial;
        actualizarHorasDisponibles(comboHora, fechaInicial, horaPreferida);
        selectorFecha.valueProperty().addListener((observable, anterior, fecha) -> {
            if (fecha != null) {
                actualizarHorasDisponibles(comboHora, fecha,
                        comboHora.getSelectionModel().getSelectedItem());
            }
        });

        if (existente != null) {
            Paciente pacienteActual = pacientesMapa.get(existente.getCurp());
            if (pacienteActual != null) {
                comboPaciente.getSelectionModel().select(pacienteActual);
            }
            comboPaciente.setDisable(true);
        } else if (curpPreseleccionada != null) {
            Paciente preseleccionado = pacientesMapa.get(curpPreseleccionada);
            if (preseleccionado != null) {
                comboPaciente.getSelectionModel().select(preseleccionado);
            }
        }

        final boolean[] registroPacienteSolicitado = {false};
        Button registrarPaciente = new Button("Registrar nuevo paciente");
        registrarPaciente.getStyleClass().add("btn-secondary");
        registrarPaciente.setGraphic(new FontIcon("fi-rr-user-add"));
        registrarPaciente.setMaxWidth(Double.MAX_VALUE);
        registrarPaciente.setTooltip(new Tooltip("Registra al paciente y regresa a esta cita"));
        registrarPaciente.setOnAction(e -> {
            fechaPendienteAlta = selectorFecha.getValue() == null ? fechaInicial : selectorFecha.getValue();
            LocalTime horaSeleccionada = comboHora.getSelectionModel().getSelectedItem();
            horaPendienteAlta = horaSeleccionada != null ? horaSeleccionada : horaInicial;
            registroPacienteSolicitado[0] = true;
            dialogo.setResult(ButtonType.CANCEL);
            dialogo.close();
        });

        VBox campoPaciente = new VBox(8, etiquetaRequerida("PACIENTE"));
        if (existente == null) {
            campoPaciente.getChildren().addAll(comboPaciente, registrarPaciente);
        } else {
            Label nombreActual = new Label(Paciente.nombreParaMostrar(
                    pacientesMapa.get(existente.getCurp()), existente.getCurp()));
            nombreActual.getStyleClass().add("info-value");
            campoPaciente.getChildren().add(nombreActual);
        }
        VBox contenido = new VBox(16, campoPaciente,
                new VBox(8, etiquetaRequerida("FECHA"), selectorFecha),
                new VBox(8, etiquetaRequerida("HORA"), comboHora));
        contenido.getStyleClass().addAll("expediente-editor-fields", "form-body");
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().setPrefWidth(460);

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType bloquear = new ButtonType("Bloquear día", ButtonBar.ButtonData.LEFT);
        dialogo.getDialogPane().getButtonTypes().addAll(bloquear, guardar, ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(guardar).getStyleClass().add("btn-primary");
        dialogo.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("btn-secondary");
        Node botonBloquear = dialogo.getDialogPane().lookupButton(bloquear);
        botonBloquear.getStyleClass().add("btn-secondary");
        // Mismo margen lateral que form-body (24) para que los botones queden al ras de los campos.
        dialogo.setOnShown(e -> {
            Node barraBotones = dialogo.getDialogPane().lookup(".button-bar");
            if (barraBotones instanceof ButtonBar) {
                // Orden fijo en cualquier plataforma: Bloquear (izquierda), Cancelar, Guardar.
                ((ButtonBar) barraBotones).setButtonOrder("L+CO");
            }
            Node barra = dialogo.getDialogPane().lookup(".button-bar > .container");
            if (barra != null) {
                barra.setStyle("-fx-padding: 0 24 20 24;");
            }
            Node encabezadoNodo = dialogo.getDialogPane().getHeader();
            if (encabezadoNodo != null && encabezadoNodo.getParent() != null) {
                encabezadoNodo.getParent().setStyle("-fx-padding: 0;");
            }
        });
        ((Button) botonBloquear).setGraphic(new FontIcon("fi-rr-lock"));
        ((Button) dialogo.getDialogPane().lookupButton(guardar)).setGraphic(new FontIcon("fi-rr-check"));
        ((Button) dialogo.getDialogPane().lookupButton(ButtonType.CANCEL)).setGraphic(new FontIcon("fi-rr-cross"));
        botonBloquear.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            LocalDate fecha = selectorFecha.getValue() == null ? fechaInicial : selectorFecha.getValue();
            if (dialogoBloqueo(fecha, false)) {
                recargarHorarios();
                renderCalendario();
                renderSemana();
                selectorFecha.setValue(fecha);
                actualizarHorasDisponibles(comboHora, fecha, null);
            }
        });

        dialogo.getDialogPane().lookupButton(guardar).addEventFilter(ActionEvent.ACTION, event -> {
            LocalDate fecha = selectorFecha.getValue();
            LocalTime hora = comboHora.getSelectionModel().getSelectedItem();
            Paciente paciente = comboPaciente.getSelectionModel().getSelectedItem();

            if (fecha == null || hora == null || (existente == null && paciente == null)) {
                event.consume();
                Notificador.advertencia("Datos incompletos",
                        "Selecciona paciente, fecha y hora para la cita.");
                return;
            }

            if (!PlanificadorAgenda.esInicioDisponible(fecha, hora, horarios, bloqueos)) {
                event.consume();
                Notificador.advertencia("Horario no disponible",
                        PlanificadorAgenda.estaBloqueado(fecha.atTime(hora), fecha.atTime(hora)
                                .plusMinutes(PlanificadorAgenda.DURACION_CITA_MINUTOS), bloqueos)
                                ? "Ese horario está bloqueado."
                                : "La hora seleccionada está fuera del horario de trabajo.");
                return;
            }

            Cita ocupada = Cita.ocupadaEn(fecha, hora, existente == null ? 0 : existente.getIdCita());
            if (ocupada != null) {
                event.consume();
                Notificador.advertencia("Horario ocupado",
                        "Ya existe la cita #" + ocupada.getIdCita() + " en ese horario.");
                return;
            }

            Date fechaCita = Date.from(LocalDateTime.of(fecha, hora)
                    .atZone(ZoneId.systemDefault()).toInstant());

            Cita citaGuardada;
            boolean guardado;
            Date fechaAnterior = null;
            if (existente == null) {
                Cita nueva = new Cita();
                nueva.setCurp(paciente.getCurp());
                nueva.setFechaCita(fechaCita);
                guardado = nueva.agendar();
                citaGuardada = nueva;
            } else {
                fechaAnterior = existente.getFechaCita();
                guardado = existente.reprogramar(fechaCita);
                citaGuardada = existente;
            }

            if (!guardado) {
                event.consume();
                Notificador.error("No se pudo guardar",
                        "La cita no se guardó. Puedes volver a intentarlo.");
                return;
            }

            Paciente pacienteCita = existente == null
                    ? paciente : pacientesMapa.get(existente.getCurp());
            // Sin cambio real de fecha no se avisa al paciente.
            if (fechaAnterior == null || fechaAnterior.getTime() != fechaCita.getTime()) {
                enviarConfirmacionAsync(pacienteCita, citaGuardada, fechaAnterior);
            }
        });

        ButtonType resultado = dialogo.showAndWait().orElse(ButtonType.CANCEL);
        if (registroPacienteSolicitado[0]) {
            abrirRegistroPaciente();
            return;
        }
        if (resultado.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            seleccion = selectorFecha.getValue() == null ? seleccion : selectorFecha.getValue();
            mes = YearMonth.from(seleccion);
            limpiarFicha();
            refrescar();
            Notificador.exito(
                    existente == null ? "Cita agendada" : "Cita reprogramada",
                    "La agenda se actualizó correctamente.");
        }
    }

    private void actualizarHorasDisponibles(ComboBox<LocalTime> combo, LocalDate fecha, LocalTime preferida) {
        List<LocalTime> disponibles = PlanificadorAgenda.iniciosDisponibles(fecha, horarios, bloqueos);
        combo.getItems().setAll(disponibles);
        if (preferida != null && disponibles.contains(preferida)) {
            combo.getSelectionModel().select(preferida);
        } else if (!disponibles.isEmpty()) {
            combo.getSelectionModel().select(0);
        } else {
            combo.getSelectionModel().clearSelection();
        }
        combo.setPromptText(disponibles.isEmpty() ? "Sin horario disponible" : "Selecciona una hora");
    }

    private boolean dialogoBloqueo(LocalDate fechaInicial, boolean semana) {
        if (fechaInicial == null) {
            return false;
        }

        Dialog<ButtonType> dialogo = new Dialog<>();
        if (ventana() != null) dialogo.initOwner(ventana());
        dialogo.setTitle(semana ? "Bloquear semana" : "Bloquear fecha");
        dialogo.getDialogPane().getStylesheets().add(
                AgendaController.class.getResource("/com/gestor/gestordecitasmedicas/cita.css").toExternalForm());
        dialogo.getDialogPane().getStyleClass().add("expediente-editor");
        Ui.tema(dialogo.getDialogPane());

        Label titulo = new Label(semana ? "BLOQUEAR SEMANA" : "BLOQUEAR FECHA");
        titulo.getStyleClass().add("field-label");
        Label subtitulo = new Label("Las citas agendadas del periodo se cancelarán (se muestran antes de confirmar). "
                + "Con horas, la franja se repite en cada día.");
        subtitulo.getStyleClass().add("card-subtitle");
        VBox encabezado = new VBox(4, titulo, subtitulo);
        encabezado.getStyleClass().add("edit-header");
        dialogo.getDialogPane().setHeader(encabezado);

        DatePicker dpInicio = new DatePicker(fechaInicial);
        DatePicker dpFin = new DatePicker(semana ? fechaInicial.plusDays(6) : fechaInicial);
        dpInicio.setMaxWidth(Double.MAX_VALUE);
        dpFin.setMaxWidth(Double.MAX_VALUE);
        dpInicio.getStyleClass().add("date-field");
        dpFin.getStyleClass().add("date-field");

        ComboBox<LocalTime> comboInicio = comboHora(PlanificadorAgenda.horasDelDia(), LocalTime.of(9, 0));
        ComboBox<LocalTime> comboFin = comboHora(PlanificadorAgenda.horasDelDia(), LocalTime.of(18, 0));
        CheckBox todoElDia = new CheckBox("Todo el día");
        todoElDia.setSelected(true);
        comboInicio.setDisable(true);
        comboFin.setDisable(true);
        todoElDia.selectedProperty().addListener((observable, anterior, seleccionado) -> {
            comboInicio.setDisable(seleccionado);
            comboFin.setDisable(seleccionado);
        });

        VBox contenido = new VBox(16,
                new VBox(8, etiquetaRequerida("DESDE"), dpInicio),
                new VBox(8, etiquetaRequerida("HASTA"), dpFin),
                todoElDia,
                new VBox(8, etiqueta("HORA INICIO"), comboInicio),
                new VBox(8, etiqueta("HORA FIN"), comboFin));
        contenido.getStyleClass().addAll("expediente-editor-fields", "form-body");
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().setPrefWidth(460);

        ButtonType guardar = new ButtonType("Bloquear", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(guardar).getStyleClass().add("btn-primary");
        dialogo.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("btn-secondary");
        ((Button) dialogo.getDialogPane().lookupButton(guardar)).setGraphic(new FontIcon("fi-rr-lock"));

        dialogo.getDialogPane().lookupButton(guardar).addEventFilter(ActionEvent.ACTION, event -> {
            // Mismo flujo que "Fechas bloqueadas" de la configuración; si no se guarda, el diálogo sigue abierto.
            if (!HorarioConfigController.crearBloqueos(dpInicio.getValue(), dpFin.getValue(), todoElDia.isSelected(),
                    comboInicio.getSelectionModel().getSelectedItem(),
                    comboFin.getSelectionModel().getSelectedItem(), 0)) {
                event.consume();
            }
        });

        // Quien llama recarga y redibuja la agenda.
        return dialogo.showAndWait().orElse(ButtonType.CANCEL).getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    private void abrirRegistroPaciente() {
        if (mainController == null) {
            return;
        }
        curpPendienteAlta = null;
        altaPendiente = true;
        PacienteEditarController editor = mainController.controlador("paciente-editar-panel.fxml");
        editor.modoNuevo("agenda-panel.fxml",
                paciente -> curpPendienteAlta = paciente.getCurp(),
                () -> {
                    altaPendiente = false;
                    curpPendienteAlta = null;
                });
        mainController.ir("paciente-editar-panel.fxml");
    }

    /** fechaAnterior != null indica reprogramación (correo distinto al de cita nueva). */
    private void enviarConfirmacionAsync(Paciente paciente, Cita cita, Date fechaAnterior) {
        boolean reprogramada = fechaAnterior != null;
        if (paciente == null || cita == null) {
            Notificador.advertencia("Correo no enviado",
                    "No se encontró el paciente de la cita, así que no se envió la confirmación.");
            return;
        }
        if (paciente.getCorreo() == null || paciente.getCorreo().trim().isEmpty()) {
            Notificador.info("Correo no enviado",
                    "El paciente no tiene un correo registrado; no se envió la confirmación.");
            return;
        }

        ControladorCorreo controlador = new ControladorCorreo();
        if (!controlador.estaConfigurado()) {
            Notificador.advertencia("Correo no enviado",
                    "La cita se guardó, pero la configuración SMTP está incompleta. "
                            + "Crea correo.properties o define SMTP_USER, SMTP_PASSWORD y SMTP_FROM.");
            return;
        }

        String destinatario = paciente.getCorreo().trim();
        Thread hilo = new Thread(() -> {
            boolean enviado = reprogramada
                    ? controlador.enviarReprogramacionCita(paciente, cita, fechaAnterior)
                    : controlador.enviarConfirmacionCita(paciente, cita);
            Platform.runLater(() -> {
                if (enviado) {
                    Notificador.exito(
                            reprogramada ? "Reprogramación enviada" : "Confirmación enviada",
                            "Se envió " + (reprogramada ? "el aviso de reprogramación" : "la confirmación")
                                    + " de la cita a " + destinatario + ".");
                } else {
                    Notificador.advertencia("Correo no enviado",
                            "La cita se guardó, pero no se pudo enviar el correo a "
                                    + destinatario + ". Revisa la configuración SMTP.");
                }
            });
        }, "correo-confirmacion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private Window ventana() {
        return fichaNombre == null || fichaNombre.getScene() == null
                ? null : fichaNombre.getScene().getWindow();
    }

    private static HBox etiquetaRequerida(String texto) {
        Label asterisco = new Label("*");
        asterisco.getStyleClass().add("required");
        return new HBox(4, etiqueta(texto), asterisco);
    }

    private static Label etiqueta(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("field-label");
        return etiqueta;
    }

    private static String nombrePacienteOrden(Paciente paciente) {
        return (valor(paciente.getApellidoPaterno()) + " " + valor(paciente.getApellidoMaterno())
                + " " + valor(paciente.getNombre())).trim();
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
