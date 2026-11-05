package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorCorreo;
import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.Estado;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class AgendaController implements PanelControlado, PanelRefrescable {
    @FXML private GridPane calendario, semana;
    @FXML private TextField fichaNombre;
    @FXML private Label fichaEdad, fichaFecha, fichaHora, mesLabel, rangoLabel, resumenLabel;
    @FXML private Button btnAgendar, btnIniciar, btnReprogramar, btnCancelar;

    private static final String[] DIAS_CAL = {"L", "M", "X", "J", "V", "S", "D"};
    private static final String[] NOMBRES_DIA = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final String[] NOMBRES_DIA_CORTO = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final String[] MESES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
    private static final String[] MESES_CORTO = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
    private static final String[] HORAS = {"09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "04:00 PM", "05:00 PM"};
    private static final LocalTime[] HORAS_T = {
            LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
            LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(17, 0)
    };

    private final BaseDatos baseDatos = new BaseDatos();

    private MainController mainController;
    private YearMonth mes = YearMonth.now();
    private LocalDate seleccion = LocalDate.now();
    private Cita citaSeleccionada;
    private Paciente pacienteSeleccionado;

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
        renderCalendario();
        renderSemana();
        limpiarFicha();
    }

    @Override
    public void refrescar() {
        boolean admin = mainController == null || mainController.esAdministrador();
        btnIniciar.setVisible(admin);
        btnIniciar.setManaged(admin);
        renderCalendario();
        renderSemana();
        if (citaSeleccionada != null) {
            Cita actualizada = buscarCita(citaSeleccionada.getIdCita());
            if (actualizada == null || actualizada.getFechaCita() == null) {
                limpiarFicha();
            } else {
                pacienteSeleccionado = baseDatos.cargarPacientePorCurp(actualizada.getCurp());
                LocalDateTime fecha = LocalDateTime.ofInstant(
                        actualizada.getFechaCita().toInstant(), ZoneId.systemDefault());
                mostrarCita(new Turno(actualizada, pacienteSeleccionado),
                        fechaLarga(fecha.toLocalDate()), fecha.toLocalTime());
            }
        }
    }

    private void renderCalendario() {
        calendario.getChildren().clear();
        mesLabel.setText(MESES[mes.getMonthValue() - 1] + " " + mes.getYear());
        for (int c = 0; c < DIAS_CAL.length; c++) calendario.add(celdaCalendario(DIAS_CAL[c], "cal-head"), c, 0);

        LocalDate dia = mes.atDay(1).minusDays(mes.atDay(1).getDayOfWeek().getValue() - 1);
        for (int i = 0; i < 42; i++, dia = dia.plusDays(1)) {
            LocalDate fecha = dia;
            String estilo = fecha.equals(seleccion) ? "cal-sel"
                    : fecha.getMonthValue() != mes.getMonthValue() ? "cal-other" : "cal-day";
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
        rangoLabel.setText("Semana del " + lunes.getDayOfMonth() + " al " + lunes.plusDays(4).getDayOfMonth()
                + " de " + MESES[lunes.getMonthValue() - 1] + ", " + lunes.getYear());

        semana.getChildren().clear();
        Label horario = new Label("HORARIO");
        horario.getStyleClass().add("caption");
        semana.add(horario, 0, 0);

        Turno[][] datos = cargarTurnos(lunes);

        for (int c = 0; c < 5; c++) {
            LocalDate dia = lunes.plusDays(c);
            Label l = new Label(NOMBRES_DIA[dia.getDayOfWeek().getValue() - 1] + "\n"
                    + dia.getDayOfMonth() + " " + MESES_CORTO[dia.getMonthValue() - 1]);
            l.getStyleClass().addAll("day-head", dia.equals(seleccion) ? "day-head-sel" : "day-head-off");
            l.setMaxWidth(Double.MAX_VALUE);
            semana.add(l, c + 1, 0);
        }
        for (int h = 0; h < HORAS.length; h++) {
            Label hora = new Label(HORAS[h]);
            hora.getStyleClass().add("info-value");
            semana.add(hora, 0, h + 1);
            for (int c = 0; c < 5; c++) {
                LocalDate fecha = lunes.plusDays(c);
                semana.add(celdaCita(datos[h][c], fecha, HORAS_T[h]), c + 1, h + 1);
            }
        }
        resumenLabel.setText(resumen(datos));
    }

    private Turno[][] cargarTurnos(LocalDate lunes) {
        Turno[][] turnos = new Turno[HORAS.length][5];

        HashMap<String, Paciente> pacientes = baseDatos.cargarPacientes();
        for (Cita cita : baseDatos.cargarCitasEntre(lunes, lunes.plusDays(4))) {
            if (cita.getFechaCita() == null) {
                continue;
            }
            LocalDateTime fecha = LocalDateTime.ofInstant(cita.getFechaCita().toInstant(), ZoneId.systemDefault());
            int dia = (int) ChronoUnit.DAYS.between(lunes, fecha.toLocalDate());
            int hora = indiceHora(fecha.toLocalTime());
            if (dia < 0 || dia > 4 || hora < 0) {
                continue;
            }
            if (turnos[hora][dia] != null) {
                continue;
            }
            turnos[hora][dia] = new Turno(cita, pacientes.get(cita.getCurp()));
        }
        return turnos;
    }

    private static int indiceHora(LocalTime hora) {
        for (int i = 0; i < HORAS_T.length; i++) {
            if (HORAS_T[i].getHour() == hora.getHour()) {
                return i;
            }
        }
        return -1;
    }

    private String resumen(Turno[][] datos) {
        int atendidas = 0, agendadas = 0, canceladas = 0, libres = 0;
        for (Turno[] fila : datos) {
            for (Turno turno : fila) {
                if (turno == null) {
                    libres++;
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
        l.setMinSize(32, 32);
        return l;
    }

    private Node celdaCita(Turno turno, LocalDate fecha, LocalTime hora) {
        if (turno == null) {
            Button libre = new Button("+ Disponible");
            libre.getStyleClass().add("slot-free");
            libre.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            libre.setOnAction(e -> dialogoCita(null, fecha, hora));
            return libre;
        }
        Label estado = new Label(etiquetaEstado(turno.cita.getEstado()));
        estado.getStyleClass().add("caption");
        Label paciente = new Label(nombrePaciente(turno.paciente, turno.cita.getCurp()));
        paciente.getStyleClass().add("info-value");
        Label referencia = new Label("Cita #" + turno.cita.getIdCita());
        referencia.getStyleClass().add("hint");
        VBox contenido = new VBox(2, estado, paciente, referencia);
        contenido.setMaxWidth(Double.MAX_VALUE);

        Button celda = new Button();
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
        fichaHora.setText(etiquetaHora(hora) + " (45 min)");
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
        dialogoCita(null, seleccion, HORAS_T[0]);
    }

    @FXML
    private void onIniciarCita() {
        if (citaSeleccionada == null || mainController == null || !mainController.esAdministrador()) {
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
        LocalDateTime fecha = LocalDateTime.ofInstant(
                citaSeleccionada.getFechaCita().toInstant(), ZoneId.systemDefault());
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

    private void dialogoCita(Cita existente, LocalDate fechaInicial, LocalTime horaInicial) {
        HashMap<String, Paciente> pacientesMapa = baseDatos.cargarPacientes();
        if (pacientesMapa.isEmpty() && existente == null) {
            aviso(Alert.AlertType.INFORMATION, "Sin pacientes",
                    "Registra un paciente antes de agendar una cita.");
            return;
        }

        List<Paciente> pacientes = new ArrayList<>(pacientesMapa.values());
        pacientes.sort(Comparator.comparing(AgendaController::nombrePacienteOrden));

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.initOwner(fichaNombre.getScene().getWindow());
        dialogo.setTitle(existente == null ? "Agendar nueva cita" : "Reprogramar cita");
        dialogo.getDialogPane().getStylesheets().add(
                AgendaController.class.getResource("cita.css").toExternalForm());
        dialogo.getDialogPane().getStyleClass().add("expediente-editor");

        Label tituloEncabezado = new Label(existente == null ? "AGENDAR NUEVA CITA" : "REPROGRAMAR CITA");
        tituloEncabezado.getStyleClass().add("field-label");
        Label subtitulo = new Label(fechaLarga(fechaInicial));
        subtitulo.getStyleClass().add("panel-title");
        VBox encabezado = new VBox(8, tituloEncabezado, subtitulo);
        encabezado.getStyleClass().add("expediente-editor-header");
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
        comboPaciente.setMaxWidth(Double.MAX_VALUE);

        DatePicker selectorFecha = new DatePicker(fechaInicial);
        selectorFecha.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> comboHora = new ComboBox<>();
        comboHora.getItems().setAll(HORAS);
        comboHora.setMaxWidth(Double.MAX_VALUE);
        comboHora.getSelectionModel().select(indiceHoraSeguro(horaInicial));

        if (existente != null) {
            Paciente pacienteActual = pacientesMapa.get(existente.getCurp());
            if (pacienteActual != null) {
                comboPaciente.getSelectionModel().select(pacienteActual);
            }
            comboPaciente.setDisable(true);
        }

        VBox contenido = new VBox(8,
                etiqueta("PACIENTE"),
                existente == null ? comboPaciente : new Label(nombrePaciente(
                        pacientesMapa.get(existente.getCurp()), existente.getCurp())),
                etiqueta("FECHA"),
                selectorFecha,
                etiqueta("HORA"),
                comboHora);
        contenido.getStyleClass().add("expediente-editor-fields");
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().setPrefWidth(460);

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType bloquear = new ButtonType("Bloquear día", ButtonBar.ButtonData.LEFT);
        dialogo.getDialogPane().getButtonTypes().addAll(bloquear, guardar, ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(guardar).getStyleClass().add("btn-primary");
        dialogo.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("btn-secondary");
        Node botonBloquear = dialogo.getDialogPane().lookupButton(bloquear);
        botonBloquear.getStyleClass().add("btn-secondary");
        // Maqueta: sin lógica todavía, se consume el evento para no cerrar el diálogo.
        botonBloquear.addEventFilter(javafx.event.ActionEvent.ACTION, javafx.event.Event::consume);

        dialogo.getDialogPane().lookupButton(guardar).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            LocalDate fecha = selectorFecha.getValue();
            int indice = comboHora.getSelectionModel().getSelectedIndex();
            Paciente paciente = comboPaciente.getSelectionModel().getSelectedItem();

            if (fecha == null || indice < 0 || (existente == null && paciente == null)) {
                event.consume();
                aviso(Alert.AlertType.WARNING, "Datos incompletos",
                        "Selecciona paciente, fecha y hora para la cita.");
                return;
            }

            LocalTime hora = HORAS_T[indice];
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

        if (dialogo.showAndWait().orElse(ButtonType.CANCEL).getButtonData()
                == ButtonBar.ButtonData.OK_DONE) {
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
        for (Cita cita : baseDatos.cargarCitasEntre(fecha, fecha)) {
            if (cita.getFechaCita() == null || cita.getIdCita() == idExcluir
                    || cita.getEstado() == Estado.CANCELADA) {
                continue;
            }
            LocalDateTime fechaCita = LocalDateTime.ofInstant(
                    cita.getFechaCita().toInstant(), ZoneId.systemDefault());
            if (fechaCita.toLocalTime().getHour() == hora.getHour()) {
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

    private static int indiceHoraSeguro(LocalTime hora) {
        int indice = indiceHora(hora);
        return indice < 0 ? 0 : indice;
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

    private static String etiquetaHora(LocalTime hora) {
        return HORAS[indiceHoraSeguro(hora)];
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
