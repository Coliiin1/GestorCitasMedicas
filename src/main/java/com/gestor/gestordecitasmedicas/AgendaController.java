package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorCorreo;
import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
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
import javafx.scene.control.Alert;
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
import java.time.Period;
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

public class AgendaController implements PanelControlado, PanelRefrescable {
    @FXML private GridPane calendario, semana;
    @FXML private TextField fichaNombre;
    @FXML private Label fichaEdad, fichaFecha, fichaHora, mesLabel, rangoLabel, resumenLabel;
    @FXML private Button btnAgendar, btnIniciar, btnReprogramar, btnCancelar;
    @FXML private Button btnConfigHorario, btnBloquearDia, btnBloquearSemana;

    private static final String[] DIAS_CAL = {"L", "M", "X", "J", "V", "S", "D"};
    private static final String[] NOMBRES_DIA_CORTO = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final String[] MESES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
    private static final String[] MESES_CORTO = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    private final BaseDatos baseDatos = new BaseDatos();
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
            Cita actualizada = buscarCita(citaSeleccionada.getIdCita());
            if (actualizada == null || actualizada.getFechaCita() == null) {
                limpiarFicha();
            } else {
                pacienteSeleccionado = baseDatos.cargarPacientePorCurp(actualizada.getCurp());
                LocalDateTime fecha = fechaHora(actualizada);
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

    private void mostrarBotonesAdmin(boolean visible) {
        if (btnConfigHorario != null) {
            btnConfigHorario.setVisible(visible);
            btnConfigHorario.setManaged(visible);
        }
        if (btnBloquearDia != null) {
            btnBloquearDia.setVisible(visible);
            btnBloquearDia.setManaged(visible);
        }
        if (btnBloquearSemana != null) {
            btnBloquearSemana.setVisible(visible);
            btnBloquearSemana.setManaged(visible);
        }
    }

    private boolean esAdmin() {
        return mainController != null && mainController.esAdministrador();
    }

    private void recargarHorarios() {
        horarios.clear();
        horarios.addAll(baseDatos.cargarHorariosActivos());
        bloqueos.clear();
        bloqueos.addAll(baseDatos.cargarBloqueos());
    }

    private void renderCalendario() {
        calendario.getChildren().clear();
        mesLabel.setText(MESES[mes.getMonthValue() - 1] + " " + mes.getYear());
        for (int c = 0; c < DIAS_CAL.length; c++) calendario.add(celdaCalendario(DIAS_CAL[c], "cal-head"), c, 0);

        LocalDate dia = mes.atDay(1).minusDays(mes.atDay(1).getDayOfWeek().getValue() - 1);
        for (int i = 0; i < 42; i++, dia = dia.plusDays(1)) {
            LocalDate fecha = dia;
            String estilo;
            if (fecha.equals(seleccion)) {
                estilo = "cal-sel";
            } else if (fecha.getMonthValue() != mes.getMonthValue()) {
                estilo = "cal-other";
            } else if (PlanificadorAgenda.tieneBloqueo(fecha, bloqueos)) {
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
                citaSeleccionada = null;
                limpiarFicha();
                renderCalendario();
                renderSemana();
            });
            calendario.add(celda, i % 7, 1 + i / 7);
        }
    }

    private void renderSemana() {
        LocalDate lunes = lunesSemana(seleccion);
        rangoLabel.setText("Semana del " + lunes.getDayOfMonth() + " al " + lunes.plusDays(6).getDayOfMonth()
                + " de " + MESES[lunes.getMonthValue() - 1] + ", " + lunes.getYear());

        ArrayList<Cita> citasSemana = baseDatos.cargarCitasEntre(lunes, lunes.plusDays(6));
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
            columna.setHgrow(javafx.scene.layout.Priority.ALWAYS);
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
                    + fecha.getDayOfMonth() + " " + MESES_CORTO[fecha.getMonthValue() - 1]);
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
            LocalDateTime fecha = fechaHora(cita);
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
            LocalDateTime fecha = fechaHora(cita);
            if (fecha != null) {
                horas.add(fecha.toLocalTime().withSecond(0).withNano(0));
            }
        }
        return new ArrayList<>(horas);
    }

    private Turno[][] cargarTurnos(List<LocalDate> fechas, List<LocalTime> horas, List<Cita> citasSemana) {
        Turno[][] turnos = new Turno[horas.size()][fechas.size()];
        HashMap<String, Paciente> pacientes = baseDatos.cargarPacientes();
        for (Cita cita : citasSemana) {
            LocalDateTime fecha = fechaHora(cita);
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
        citaSeleccionada = null;
        limpiarFicha();
        renderCalendario();
        renderSemana();
    }

    private static LocalDate lunesSemana(LocalDate d) {
        return d.minusDays(d.getDayOfWeek().getValue() - 1);
    }

    private static String fechaLarga(LocalDate d) {
        return NOMBRES_DIA_CORTO[d.getDayOfWeek().getValue() - 1] + ", " + d.getDayOfMonth() + " "
                + MESES_CORTO[d.getMonthValue() - 1] + " " + d.getYear();
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
        Label estado = new Label(etiquetaEstado(turno.cita.getEstado()));
        estado.getStyleClass().add("caption");
        Label paciente = new Label(nombrePaciente(turno.paciente, turno.cita.getCurp()));
        paciente.getStyleClass().add("info-value");
        paciente.setWrapText(true);
        Label referencia = new Label("Cita #" + turno.cita.getIdCita());
        referencia.getStyleClass().add("hint");
        VBox contenido = new VBox(2, estado, paciente, referencia);
        contenido.setMaxWidth(Double.MAX_VALUE);
        contenido.setMinWidth(0);

        Button celda = new Button();
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
        fichaNombre.setText(nombrePaciente(turno.paciente, turno.cita.getCurp()));
        fichaEdad.setText("Edad: " + edad(turno.paciente) + " · " + etiquetaEstado(turno.cita.getEstado()));
        fichaFecha.setText(fecha);
        fichaHora.setText(PlanificadorAgenda.formatoHora(hora) + " ("
                + PlanificadorAgenda.DURACION_CITA_MINUTOS + " min)");
        boolean cancelada = turno.cita.getEstado() == Estado.CANCELADA;
        boolean atendida = turno.cita.getEstado() == Estado.ATENDIDA;
        btnIniciar.setDisable(cancelada);
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
        if (btnIniciar != null) btnIniciar.setDisable(true);
        if (btnReprogramar != null) btnReprogramar.setDisable(true);
        if (btnCancelar != null) btnCancelar.setDisable(true);
    }

    @FXML
    private void onAgendarNueva() {
        List<LocalTime> disponibles = PlanificadorAgenda.iniciosDisponibles(seleccion, horarios, bloqueos);
        if (disponibles.isEmpty()) {
            aviso(Alert.AlertType.WARNING, "Sin disponibilidad",
                    PlanificadorAgenda.tieneBloqueo(seleccion, bloqueos)
                            ? "La fecha seleccionada está bloqueada."
                            : "No hay horario de trabajo configurado para la fecha seleccionada.");
            return;
        }
        dialogoCita(null, seleccion, disponibles.get(0));
    }

    @FXML
    private void onIniciarCita() {
        if (citaSeleccionada == null || !esAdmin()) {
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
        LocalDateTime fecha = fechaHora(citaSeleccionada);
        dialogoCita(citaSeleccionada, fecha.toLocalDate(), fecha.toLocalTime());
    }

    @FXML
    private void onCancelarCita() {
        if (citaSeleccionada == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cancelar cita");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Cancelar la cita #" + citaSeleccionada.getIdCita() + " del paciente "
                + nombrePaciente(pacienteSeleccionado, citaSeleccionada.getCurp()) + "?");
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        citaSeleccionada.setEstado(Estado.CANCELADA);
        if (!baseDatos.actualizarCita(citaSeleccionada)) {
            aviso(Alert.AlertType.ERROR, "No se pudo cancelar", "La cita no se actualizó en la base de datos.");
            return;
        }
        limpiarFicha();
        refrescar();
        aviso(Alert.AlertType.INFORMATION, "Cita cancelada", "La cita se marcó como cancelada.");
    }

    @FXML
    private void onConfigurarHorario() {
        if (!esAdmin()) {
            return;
        }
        boolean cambios = HorarioConfigController.abrir(ventana(), baseDatos);
        if (cambios) {
            refrescar();
        }
    }

    @FXML
    private void onBloquearDia() {
        if (!esAdmin()) {
            return;
        }
        if (dialogoBloqueo(seleccion, false)) {
            refrescar();
        }
    }

    @FXML
    private void onBloquearSemana() {
        if (!esAdmin()) {
            return;
        }
        if (dialogoBloqueo(lunesSemana(seleccion), true)) {
            refrescar();
        }
    }

    private void dialogoCita(Cita existente, LocalDate fechaInicial, LocalTime horaInicial) {
        dialogoCita(existente, fechaInicial, horaInicial, null);
    }

    private void dialogoCita(Cita existente, LocalDate fechaInicial, LocalTime horaInicial,
                             String curpPreseleccionada) {
        HashMap<String, Paciente> pacientesMapa = baseDatos.cargarPacientes();

        List<Paciente> pacientes = new ArrayList<>(pacientesMapa.values());
        pacientes.sort(Comparator.comparing(AgendaController::nombrePacienteOrden));

        Dialog<ButtonType> dialogo = new Dialog<>();
        if (ventana() != null) dialogo.initOwner(ventana());
        dialogo.setTitle(existente == null ? "Agendar nueva cita" : "Reprogramar cita");
        dialogo.getDialogPane().getStylesheets().add(
                AgendaController.class.getResource("cita.css").toExternalForm());
        dialogo.getDialogPane().getStyleClass().add("expediente-editor");

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
                return paciente == null ? "" : nombrePaciente(paciente, paciente.getCurp()) + " · " + paciente.getCurp();
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

        ComboBox<LocalTime> comboHora = new ComboBox<>();
        comboHora.setMaxWidth(Double.MAX_VALUE);
        comboHora.getStyleClass().add("combo-field");
        comboHora.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime hora) {
                return PlanificadorAgenda.formatoHora(hora);
            }

            @Override
            public LocalTime fromString(String texto) {
                return null;
            }
        });
        LocalTime horaPreferida = existente != null ? fechaHora(existente).toLocalTime() : horaInicial;
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
            Label nombreActual = new Label(nombrePaciente(
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
        botonBloquear.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            LocalDate fecha = selectorFecha.getValue() == null ? fechaInicial : selectorFecha.getValue();
            if (dialogoBloqueo(fecha, false)) {
                recargarHorarios();
                selectorFecha.setValue(fecha);
                actualizarHorasDisponibles(comboHora, fecha, null);
            }
        });

        dialogo.getDialogPane().lookupButton(guardar).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            LocalDate fecha = selectorFecha.getValue();
            LocalTime hora = comboHora.getSelectionModel().getSelectedItem();
            Paciente paciente = comboPaciente.getSelectionModel().getSelectedItem();

            if (fecha == null || hora == null || (existente == null && paciente == null)) {
                event.consume();
                aviso(Alert.AlertType.WARNING, "Datos incompletos",
                        "Selecciona paciente, fecha y hora para la cita.");
                return;
            }

            if (!PlanificadorAgenda.esInicioDisponible(fecha, hora, horarios, bloqueos)) {
                event.consume();
                aviso(Alert.AlertType.WARNING, "Horario no disponible",
                        PlanificadorAgenda.tieneBloqueo(fecha, bloqueos)
                                ? "La fecha seleccionada está bloqueada."
                                : "La hora seleccionada está fuera del horario de trabajo.");
                return;
            }

            Cita ocupada = buscarOcupada(fecha, hora, existente == null ? 0 : existente.getIdCita());
            if (ocupada != null) {
                event.consume();
                aviso(Alert.AlertType.WARNING, "Horario ocupado",
                        "Ya existe la cita #" + ocupada.getIdCita() + " en ese horario.");
                return;
            }

            Date fechaCita = Date.from(LocalDateTime.of(fecha, hora)
                    .atZone(ZoneId.systemDefault()).toInstant());

            Cita citaGuardada;
            boolean guardado;
            if (existente == null) {
                Cita nueva = new Cita();
                nueva.setCurp(paciente.getCurp());
                nueva.setFechaCita(fechaCita);
                nueva.setEstado(Estado.AGENDADA);
                guardado = baseDatos.insertarCita(nueva);
                citaGuardada = nueva;
            } else {
                existente.setFechaCita(fechaCita);
                existente.setEstado(Estado.AGENDADA);
                guardado = baseDatos.actualizarCita(existente);
                citaGuardada = existente;
            }

            if (!guardado) {
                event.consume();
                aviso(Alert.AlertType.ERROR, "No se pudo guardar",
                        "La cita no se guardó. Puedes volver a intentarlo.");
                return;
            }

            Paciente pacienteCita = existente == null
                    ? paciente : pacientesMapa.get(existente.getCurp());
            enviarConfirmacionAsync(pacienteCita, citaGuardada);
        });

        ButtonType resultado = dialogo.showAndWait().orElse(ButtonType.CANCEL);
        if (registroPacienteSolicitado[0]) {
            abrirRegistroPaciente();
            return;
        }
        if (resultado.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            seleccion = selectorFecha.getValue() == null ? seleccion : selectorFecha.getValue();
            mes = YearMonth.from(seleccion);
            citaSeleccionada = null;
            limpiarFicha();
            refrescar();
            aviso(Alert.AlertType.INFORMATION,
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
        if (!esAdmin() || fechaInicial == null) {
            return false;
        }

        Dialog<ButtonType> dialogo = new Dialog<>();
        if (ventana() != null) dialogo.initOwner(ventana());
        dialogo.setTitle(semana ? "Bloquear semana" : "Bloquear fecha");
        dialogo.getDialogPane().getStylesheets().add(
                AgendaController.class.getResource("cita.css").toExternalForm());
        dialogo.getDialogPane().getStyleClass().add("expediente-editor");

        Label titulo = new Label(semana ? "BLOQUEAR SEMANA" : "BLOQUEAR FECHA");
        titulo.getStyleClass().add("field-label");
        Label subtitulo = new Label("Las citas activas no se cancelan automáticamente.");
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

        TextField motivo = new TextField();
        motivo.setPromptText("Motivo (opcional)");
        motivo.getStyleClass().add("search-field");

        VBox contenido = new VBox(16,
                new VBox(8, etiquetaRequerida("DESDE"), dpInicio),
                new VBox(8, etiquetaRequerida("HASTA"), dpFin),
                todoElDia,
                new VBox(8, etiqueta("HORA INICIO"), comboInicio),
                new VBox(8, etiqueta("HORA FIN"), comboFin),
                new VBox(8, etiqueta("MOTIVO"), motivo));
        contenido.getStyleClass().addAll("expediente-editor-fields", "form-body");
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().setPrefWidth(460);

        ButtonType guardar = new ButtonType("Bloquear", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(guardar).getStyleClass().add("btn-primary");
        dialogo.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("btn-secondary");
        ((Button) dialogo.getDialogPane().lookupButton(guardar)).setGraphic(new FontIcon("fi-rr-lock"));

        dialogo.getDialogPane().lookupButton(guardar).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            LocalDate inicioDia = dpInicio.getValue();
            LocalDate finDia = dpFin.getValue();
            if (inicioDia == null || finDia == null || finDia.isBefore(inicioDia)) {
                event.consume();
                aviso(Alert.AlertType.WARNING, "Fechas inválidas",
                        "Selecciona un rango de fechas válido.");
                return;
            }

            LocalDateTime inicio;
            LocalDateTime fin;
            if (todoElDia.isSelected()) {
                inicio = inicioDia.atStartOfDay();
                fin = finDia.plusDays(1).atStartOfDay();
            } else {
                LocalTime horaInicio = comboInicio.getSelectionModel().getSelectedItem();
                LocalTime horaFin = comboFin.getSelectionModel().getSelectedItem();
                if (horaInicio == null || horaFin == null || !horaInicio.isBefore(horaFin)) {
                    event.consume();
                    aviso(Alert.AlertType.WARNING, "Horas inválidas",
                            "La hora de inicio debe ser anterior a la hora de fin.");
                    return;
                }
                inicio = inicioDia.atTime(horaInicio);
                fin = finDia.atTime(horaFin);
            }

            ArrayList<Cita> conflictos = baseDatos.citasActivasEnRango(inicio, fin);
            if (!conflictos.isEmpty()) {
                event.consume();
                aviso(Alert.AlertType.WARNING, "Hay citas en el rango",
                        "No se puede bloquear porque hay " + conflictos.size()
                                + " cita(s) activa(s) en ese periodo. Reprograma o cancela primero.");
                return;
            }

            if (PlanificadorAgenda.estaBloqueado(inicio, fin, bloqueos)) {
                event.consume();
                aviso(Alert.AlertType.WARNING, "Bloqueo superpuesto",
                        "Ya existe un bloqueo que se cruza con ese periodo.");
                return;
            }

            HorarioBloqueado bloqueo = new HorarioBloqueado();
            bloqueo.setFechaInicio(inicio);
            bloqueo.setFechaFin(fin);
            bloqueo.setMotivo(motivo.getText() == null ? null : motivo.getText().trim());
            if (!baseDatos.guardarBloqueo(bloqueo)) {
                event.consume();
                aviso(Alert.AlertType.ERROR, "No se pudo bloquear",
                        "El bloqueo no se guardó. Revisa el rango e inténtalo de nuevo.");
            }
        });

        ButtonType resultado = dialogo.showAndWait().orElse(ButtonType.CANCEL);
        if (resultado.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            recargarHorarios();
            renderCalendario();
            renderSemana();
            return true;
        }
        return false;
    }

    private static ComboBox<LocalTime> comboHora(List<LocalTime> horas, LocalTime seleccionada) {
        ComboBox<LocalTime> combo = new ComboBox<>();
        combo.getItems().setAll(horas);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add("combo-field");
        combo.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime hora) {
                return PlanificadorAgenda.formatoHora(hora);
            }

            @Override
            public LocalTime fromString(String texto) {
                return null;
            }
        });
        if (seleccionada != null && horas.contains(seleccionada)) {
            combo.getSelectionModel().select(seleccionada);
        }
        return combo;
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

    private void enviarConfirmacionAsync(Paciente paciente, Cita cita) {
        if (paciente == null || cita == null) {
            aviso(Alert.AlertType.WARNING, "Correo no enviado",
                    "No se encontró el paciente de la cita, así que no se envió la confirmación.");
            return;
        }
        if (paciente.getCorreo() == null || paciente.getCorreo().trim().isEmpty()) {
            aviso(Alert.AlertType.INFORMATION, "Correo no enviado",
                    "El paciente no tiene un correo registrado; no se envió la confirmación.");
            return;
        }

        ControladorCorreo controlador = new ControladorCorreo();
        if (!controlador.estaConfigurado()) {
            aviso(Alert.AlertType.WARNING, "Correo no enviado",
                    "La cita se guardó, pero la configuración SMTP está incompleta. "
                            + "Crea correo.properties o define SMTP_USER, SMTP_PASSWORD y SMTP_FROM.");
            return;
        }

        String destinatario = paciente.getCorreo().trim();
        Thread hilo = new Thread(() -> {
            boolean enviado = controlador.enviarConfirmacionCita(paciente, cita);
            Platform.runLater(() -> {
                if (enviado) {
                    aviso(Alert.AlertType.INFORMATION, "Confirmación enviada",
                            "Se envió la confirmación de la cita a " + destinatario + ".");
                } else {
                    aviso(Alert.AlertType.WARNING, "Correo no enviado",
                            "La cita se guardó, pero no se pudo enviar el correo a "
                                    + destinatario + ". Revisa la configuración SMTP.");
                }
            });
        }, "correo-confirmacion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private Cita buscarOcupada(LocalDate fecha, LocalTime hora, int idExcluir) {
        LocalDateTime inicio = LocalDateTime.of(fecha, hora);
        LocalDateTime fin = inicio.plusMinutes(PlanificadorAgenda.DURACION_CITA_MINUTOS);
        for (Cita cita : baseDatos.cargarCitasEntre(fecha, fecha)) {
            if (cita.getFechaCita() == null || cita.getIdCita() == idExcluir
                    || cita.getEstado() == Estado.CANCELADA) {
                continue;
            }
            LocalDateTime inicioCita = fechaHora(cita);
            if (PlanificadorAgenda.seSolapan(inicio, fin, inicioCita,
                    inicioCita.plusMinutes(PlanificadorAgenda.DURACION_CITA_MINUTOS))) {
                return cita;
            }
        }
        return null;
    }

    private Cita buscarCita(int idCita) {
        for (Cita cita : baseDatos.cargarCitas().values()) {
            if (cita.getIdCita() == idCita) {
                return cita;
            }
        }
        return null;
    }

    private javafx.stage.Window ventana() {
        return fichaNombre == null || fichaNombre.getScene() == null
                ? null : fichaNombre.getScene().getWindow();
    }

    private static LocalDateTime fechaHora(Cita cita) {
        if (cita == null || cita.getFechaCita() == null) {
            return null;
        }
        return LocalDateTime.ofInstant(cita.getFechaCita().toInstant(), ZoneId.systemDefault());
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

    private static String nombrePaciente(Paciente paciente, String curp) {
        if (paciente == null) {
            return curp == null ? "Paciente no registrado" : curp;
        }
        String nombre = (valor(paciente.getNombre()) + " " + valor(paciente.getApellidoPaterno())
                + " " + valor(paciente.getApellidoMaterno())).trim();
        return nombre.isEmpty() ? (curp == null ? "Paciente sin nombre" : curp) : nombre;
    }

    private static String nombrePacienteOrden(Paciente paciente) {
        return (valor(paciente.getApellidoPaterno()) + " " + valor(paciente.getApellidoMaterno())
                + " " + valor(paciente.getNombre())).trim();
    }

    private static String edad(Paciente paciente) {
        if (paciente == null || paciente.getFechaNacimiento() == null) {
            return "--";
        }
        LocalDate nacimiento = paciente.getFechaNacimiento().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return String.valueOf(Period.between(nacimiento, LocalDate.now()).getYears());
    }

    private static String etiquetaEstado(Estado estado) {
        if (estado == null) return "Agendada";
        switch (estado) {
            case ATENDIDA: return "Atendida";
            case CANCELADA: return "Cancelada";
            default: return "Agendada";
        }
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
