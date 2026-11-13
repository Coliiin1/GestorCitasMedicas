package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.DiaSemana;
import com.gestor.gestordecitasmedicas.modelo.Horario;
import com.gestor.gestordecitasmedicas.modelo.HorarioBloqueado;
import com.gestor.gestordecitasmedicas.modelo.PlanificadorAgenda;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.gestor.gestordecitasmedicas.servicio.ControladorCorreo;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.time.temporal.ChronoUnit;


/**
 * Configuración de horario semanal y fechas bloqueadas. Se abre únicamente
 * desde la agenda cuando la sesión es de administrador.
 */
public final class HorarioConfigController {
    private final Window owner;
    private final ArrayList<Horario> horarios = new ArrayList<>();
    private final ArrayList<HorarioBloqueado> bloqueos = new ArrayList<>();

    private Dialog<ButtonType> dialogo;
    private VBox listaHorarios;
    private VBox listaBloqueos;
    private ComboBox<DiaSemana> comboDia;
    private ComboBox<LocalTime> comboInicio;
    private ComboBox<LocalTime> comboFin;
    private CheckBox checkActivo;
    private Button btnGuardarHorario;
    private Horario horarioEditando;

    private DatePicker bloqueoDesde;
    private DatePicker bloqueoHasta;
    private ComboBox<LocalTime> bloqueoHoraInicio;
    private ComboBox<LocalTime> bloqueoHoraFin;
    private CheckBox bloqueoTodoElDia;
    private Button btnGuardarBloqueo;
    private HorarioBloqueado bloqueoEditando;

    private boolean cambios;

    private HorarioConfigController(Window owner) {
        this.owner = owner;
    }

    public static boolean abrir(Window owner) {
        return new HorarioConfigController(owner).mostrar();
    }

    private boolean mostrar() {
        dialogo = new Dialog<>();
        if (owner != null) {
            dialogo.initOwner(owner);
        }
        dialogo.setTitle("Horario de trabajo y bloqueos");
        dialogo.getDialogPane().getStylesheets().add(
                HorarioConfigController.class.getResource("/com/gestor/gestordecitasmedicas/cita.css").toExternalForm());
        dialogo.getDialogPane().getStyleClass().add("expediente-editor");
        Ui.tema(dialogo.getDialogPane());

        Label titulo = new Label("HORARIO DE TRABAJO Y FECHAS BLOQUEADAS");
        titulo.getStyleClass().add("card-title");
        Label subtitulo = new Label("Define los tramos semanales y las excepciones de la agenda.");
        subtitulo.getStyleClass().add("card-subtitle");
        VBox encabezado = new VBox(4, titulo, subtitulo);
        encabezado.getStyleClass().add("edit-header");
        dialogo.getDialogPane().setHeader(encabezado);

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("expediente-tabs");
        tabs.setPadding(new Insets(16, 24, 0, 24));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab tabHorario = new Tab("Horario semanal", panelHorario());
        Tab tabBloqueos = new Tab("Fechas bloqueadas", panelBloqueos());
        tabs.getTabs().addAll(tabHorario, tabBloqueos);
        tabs.setPrefSize(720, 470);
        dialogo.getDialogPane().setContent(tabs);
        dialogo.getDialogPane().setPrefWidth(760);

        ButtonType cerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().add(cerrar);
        Node botonCerrar = dialogo.getDialogPane().lookupButton(cerrar);
        botonCerrar.getStyleClass().add("btn-secondary");

        recargarListas();
        dialogo.showAndWait();
        return cambios;
    }

    private Node panelHorario() {
        comboDia = new ComboBox<>();
        comboDia.getItems().setAll(DiaSemana.values());
        comboDia.setConverter(new StringConverter<DiaSemana>() {
            @Override
            public String toString(DiaSemana dia) {
                return dia == null ? "" : dia.getEtiqueta();
            }

            @Override
            public DiaSemana fromString(String texto) {
                return null;
            }
        });
        comboDia.getSelectionModel().select(LocalDate.now().getDayOfWeek().getValue() - 1);
        comboDia.setMaxWidth(Double.MAX_VALUE);
        comboDia.getStyleClass().add("combo-field");

        comboInicio = Ui.comboHora(PlanificadorAgenda.horasDelDia(), LocalTime.of(9, 0));
        comboFin = Ui.comboHora(PlanificadorAgenda.horasDelDia(), LocalTime.of(13, 0));
        checkActivo = new CheckBox("Activo");
        checkActivo.setSelected(true);

        btnGuardarHorario = new Button("Agregar tramo");
        btnGuardarHorario.getStyleClass().add("btn-primary");
        btnGuardarHorario.setOnAction(e -> guardarHorario());

        Button limpiar = new Button("Limpiar");
        limpiar.getStyleClass().add("btn-secondary");
        limpiar.setOnAction(e -> limpiarFormularioHorario());

        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(8);
        formulario.add(campo("DÍA", comboDia), 0, 0);
        formulario.add(campo("INICIO", comboInicio), 1, 0);
        formulario.add(campo("FIN", comboFin), 2, 0);
        formulario.add(checkActivo, 3, 0);
        HBox acciones = new HBox(8, btnGuardarHorario, limpiar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        listaHorarios = new VBox(8);
        ScrollPane scroll = new ScrollPane(listaHorarios);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(260);

        VBox contenido = new VBox(12,
                new Label("Cada tramo genera citas de 60 minutos. Puedes tener mañana y tarde el mismo día."),
                formulario,
                acciones,
                scroll);
        contenido.getStyleClass().add("form-body");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return contenido;
    }

    private Node panelBloqueos() {
        bloqueoDesde = new DatePicker(LocalDate.now());
        bloqueoHasta = new DatePicker(LocalDate.now());
        bloqueoDesde.setMaxWidth(Double.MAX_VALUE);
        bloqueoHasta.setMaxWidth(Double.MAX_VALUE);
        bloqueoDesde.getStyleClass().add("date-field");
        bloqueoHasta.getStyleClass().add("date-field");

        bloqueoHoraInicio = Ui.comboHora(PlanificadorAgenda.horasDelDia(), LocalTime.of(9, 0));
        bloqueoHoraFin = Ui.comboHora(PlanificadorAgenda.horasDelDia(), LocalTime.of(18, 0));
        bloqueoTodoElDia = new CheckBox("Todo el día");
        bloqueoTodoElDia.setSelected(true);
        bloqueoHoraInicio.setDisable(true);
        bloqueoHoraFin.setDisable(true);
        bloqueoTodoElDia.selectedProperty().addListener((observable, anterior, seleccionado) -> {
            bloqueoHoraInicio.setDisable(seleccionado);
            bloqueoHoraFin.setDisable(seleccionado);
        });

        btnGuardarBloqueo = new Button("Agregar bloqueo");
        btnGuardarBloqueo.getStyleClass().add("btn-primary");
        btnGuardarBloqueo.setOnAction(e -> guardarBloqueo());

        Button limpiar = new Button("Limpiar");
        limpiar.getStyleClass().add("btn-secondary");
        limpiar.setOnAction(e -> limpiarFormularioBloqueo());

        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(8);
        formulario.add(campo("DESDE", bloqueoDesde), 0, 0);
        formulario.add(campo("HORA INICIO", bloqueoHoraInicio), 1, 0);
        formulario.add(campo("HASTA", bloqueoHasta), 2, 0);
        formulario.add(campo("HORA FIN", bloqueoHoraFin), 3, 0);
        formulario.add(bloqueoTodoElDia, 0, 1);
        HBox acciones = new HBox(8, btnGuardarBloqueo, limpiar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        listaBloqueos = new VBox(8);
        ScrollPane scroll = new ScrollPane(listaBloqueos);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(240);

        VBox contenido = new VBox(12,
                new Label("Los bloqueos impiden agendar en ese periodo. Si hay citas agendadas, "
                        + "se cancelarán al bloquear (se te mostrarán antes de confirmar)."),
                formulario,
                acciones,
                scroll);
        contenido.getStyleClass().add("form-body");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return contenido;
    }

    private void recargarListas() {
        horarios.clear();
        horarios.addAll(Horario.todos());
        bloqueos.clear();
        bloqueos.addAll(HorarioBloqueado.todos());
        renderHorarios();
        renderBloqueos();
    }

    private void renderHorarios() {
        if (listaHorarios == null) {
            return;
        }
        listaHorarios.getChildren().clear();
        if (horarios.isEmpty()) {
            listaHorarios.getChildren().add(mensajeVacio("Aún no hay tramos configurados."));
            return;
        }
        List<Horario> ordenados = new ArrayList<>(horarios);
        ordenados.sort(Comparator.comparing((Horario h) -> h.getDia() == null ? 0 : h.getDia().getNumero())
                .thenComparing(Horario::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder())));
        for (Horario horario : ordenados) {
            listaHorarios.getChildren().add(filaHorario(horario));
        }
    }

    private HBox filaHorario(Horario horario) {
        String dia = horario.getDia() == null ? "Día inválido" : horario.getDia().getEtiqueta();
        Label nombre = new Label(dia + " · " + PlanificadorAgenda.formatoHora(horario.getHoraInicio())
                + " a " + PlanificadorAgenda.formatoHora(horario.getHoraFin()));
        nombre.getStyleClass().add("row-name");

        Label estado = new Label(horario.isEstado() ? "Activo" : "Inactivo");
        estado.getStyleClass().add(horario.isEstado() ? "chip" : "hint");

        VBox datos = new VBox(4, nombre, new Label("Tramo #" + horario.getIdHorario()));
        ((Label) datos.getChildren().get(1)).getStyleClass().add("hint");

        Region separador = new Region();
        HBox.setHgrow(separador, Priority.ALWAYS);

        Button editar = new Button("Editar");
        editar.getStyleClass().add("btn-edit");
        editar.setOnAction(e -> editarHorario(horario));

        Button eliminar = new Button("Eliminar");
        eliminar.getStyleClass().add("btn-del");
        eliminar.setOnAction(e -> eliminarHorario(horario));

        HBox fila = new HBox(12, datos, estado, separador, editar, eliminar);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("row-card");
        return fila;
    }

    private void renderBloqueos() {
        if (listaBloqueos == null) {
            return;
        }
        listaBloqueos.getChildren().clear();
        // Los bloqueos ya vencidos no se listan: no afectan la agenda y solo estorban.
        List<HorarioBloqueado> ordenados = new ArrayList<>(bloqueos);
        LocalDateTime ahora = LocalDateTime.now();
        ordenados.removeIf(b -> b.getFechaFin() != null && !b.getFechaFin().isAfter(ahora));
        if (ordenados.isEmpty()) {
            listaBloqueos.getChildren().add(mensajeVacio("No hay fechas bloqueadas vigentes."));
            return;
        }
        ordenados.sort(Comparator.comparing(HorarioBloqueado::getFechaInicio,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (HorarioBloqueado bloqueo : ordenados) {
            listaBloqueos.getChildren().add(filaBloqueo(bloqueo));
        }
    }

    private HBox filaBloqueo(HorarioBloqueado bloqueo) {
        Label nombre = new Label(PlanificadorAgenda.formatoRango(
                bloqueo.getFechaInicio(), bloqueo.getFechaFin()));
        nombre.getStyleClass().add("row-name");
        VBox datos = new VBox(4, nombre);

        Region separador = new Region();
        HBox.setHgrow(separador, Priority.ALWAYS);

        Button editar = new Button("Editar");
        editar.getStyleClass().add("btn-edit");
        editar.setOnAction(e -> editarBloqueo(bloqueo));

        Button eliminar = new Button("Eliminar");
        eliminar.getStyleClass().add("btn-del");
        eliminar.setOnAction(e -> eliminarBloqueo(bloqueo));

        HBox fila = new HBox(12, datos, separador, editar, eliminar);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("row-card");
        return fila;
    }

    private void guardarHorario() {
        DiaSemana dia = comboDia.getSelectionModel().getSelectedItem();
        LocalTime inicio = comboInicio.getSelectionModel().getSelectedItem();
        LocalTime fin = comboFin.getSelectionModel().getSelectedItem();
        if (dia == null || inicio == null || fin == null || !inicio.isBefore(fin)) {
            Notificador.advertencia("Tramo inválido",
                    "Selecciona un día y un horario donde el inicio sea anterior al fin.");
            return;
        }

        Horario horario = new Horario();
        horario.setIdHorario(horarioEditando == null ? 0 : horarioEditando.getIdHorario());
        horario.setDia(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        horario.setEstado(checkActivo.isSelected());

        for (Horario existente : horarios) {
            if (existente.getIdHorario() == horario.getIdHorario() || !existente.isEstado()
                    || !horario.isEstado()) {
                continue;
            }
            if (PlanificadorAgenda.tramoSeSolapa(existente, horario)) {
                Notificador.advertencia("Tramo superpuesto",
                        "Ya existe un tramo activo ese día en un horario que se cruza.");
                return;
            }
        }

        if (horarioEditando != null) {
            int fuera = 0;
            for (Cita cita : horarioEditando.citasActivas()) {
                if (!horario.isEstado() || !cubre(horario, cita.fechaHora())) {
                    fuera++;
                }
            }
            if (fuera > 0) {
                Notificador.advertencia("Hay citas en ese tramo",
                        "No se puede modificar o desactivar el tramo porque " + fuera
                                + " cita(s) agendada(s) quedarían fuera de horario. Reprograma o cancela primero.");
                return;
            }
        }

        if (!horario.guardar()) {
            Notificador.error("No se pudo guardar",
                    "El tramo no se guardó. Revisa que no se cruce con otro tramo activo.");
            return;
        }
        cambios = true;
        limpiarFormularioHorario();
        recargarListas();
    }

    private void editarHorario(Horario horario) {
        horarioEditando = horario;
        comboDia.getSelectionModel().select(horario.getDia());
        seleccionarHora(comboInicio, horario.getHoraInicio());
        seleccionarHora(comboFin, horario.getHoraFin());
        checkActivo.setSelected(horario.isEstado());
        btnGuardarHorario.setText("Guardar cambios");
    }

    private void eliminarHorario(Horario horario) {
        if (!confirmar("Eliminar tramo",
                "¿Eliminar el tramo " + horario.getDia().getEtiqueta() + " de "
                        + PlanificadorAgenda.formatoHora(horario.getHoraInicio()) + " a "
                        + PlanificadorAgenda.formatoHora(horario.getHoraFin()) + "?")) {
            return;
        }
        if (!horario.eliminar()) {
            Notificador.advertencia("No se pudo eliminar",
                    "El tramo tiene citas activas o no existe. Reprograma o cancela las citas primero.");
            return;
        }
        cambios = true;
        if (horarioEditando != null && horarioEditando.getIdHorario() == horario.getIdHorario()) {
            limpiarFormularioHorario();
        }
        recargarListas();
    }

    private void limpiarFormularioHorario() {
        horarioEditando = null;
        comboDia.getSelectionModel().select(LocalDate.now().getDayOfWeek().getValue() - 1);
        seleccionarHora(comboInicio, LocalTime.of(9, 0));
        seleccionarHora(comboFin, LocalTime.of(13, 0));
        checkActivo.setSelected(true);
        btnGuardarHorario.setText("Agregar tramo");
    }

    private void guardarBloqueo() {
        if (!crearBloqueos(bloqueoDesde.getValue(), bloqueoHasta.getValue(), bloqueoTodoElDia.isSelected(),
                bloqueoHoraInicio.getSelectionModel().getSelectedItem(),
                bloqueoHoraFin.getSelectionModel().getSelectedItem(),
                bloqueoEditando == null ? 0 : bloqueoEditando.getIdHorarioBloqueado())) {
            return;
        }
        cambios = true;
        limpiarFormularioBloqueo();
        recargarListas();
    }

    /**
     * Flujo único de alta/edición de bloqueos (lo usan este diálogo y la agenda): valida, revisa cruces,
     * confirma la cancelación de citas, guarda y notifica. Con horas, la franja se repite en cada día del rango.
     * Devuelve true si se guardó; si no, ya mostró el aviso correspondiente.
     */
    static boolean crearBloqueos(LocalDate desde, LocalDate hasta, boolean todoElDia,
                                 LocalTime horaInicio, LocalTime horaFin, int idEditando) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            Notificador.advertencia("Fechas inválidas", "Selecciona un rango de fechas válido.");
            return false;
        }
        if (!todoElDia && (horaInicio == null || horaFin == null || !horaInicio.isBefore(horaFin))) {
            Notificador.advertencia("Horas inválidas", "La hora de inicio debe ser anterior a la hora de fin.");
            return false;
        }

        List<LocalDateTime[]> intervalos =
                PlanificadorAgenda.intervalosDeBloqueo(desde, hasta, todoElDia, horaInicio, horaFin);
        LocalDateTime ahora = LocalDateTime.now();
        intervalos.removeIf(i -> !i[1].isAfter(ahora));
        if (intervalos.isEmpty()) {
            Notificador.advertencia("Periodo vencido", "El periodo seleccionado ya pasó; no hay nada que bloquear.");
            return false;
        }

        for (HorarioBloqueado existente : HorarioBloqueado.todos()) {
            if (existente.getIdHorarioBloqueado() == idEditando) {
                continue;
            }
            for (LocalDateTime[] i : intervalos) {
                if (PlanificadorAgenda.seSolapan(i[0], i[1], existente.getFechaInicio(), existente.getFechaFin())) {
                    Notificador.advertencia("Bloqueo superpuesto", "Se cruza con un bloqueo existente: "
                            + PlanificadorAgenda.formatoRango(existente.getFechaInicio(), existente.getFechaFin())
                            + ". Edita o elimina ese bloqueo primero.");
                    return false;
                }
            }
        }

        List<Cita> conflictos = new ArrayList<>();
        List<HorarioBloqueado> nuevos = new ArrayList<>();
        for (LocalDateTime[] i : intervalos) {
            conflictos.addAll(Cita.activasEnRango(i[0], i[1]));
            HorarioBloqueado bloqueo = new HorarioBloqueado();
            bloqueo.setIdHorarioBloqueado(nuevos.isEmpty() ? idEditando : 0); // al editar, el primero conserva el id
            bloqueo.setFechaInicio(i[0]);
            bloqueo.setFechaFin(i[1]);
            nuevos.add(bloqueo);
        }

        String motivo = confirmarCancelacionCitas(conflictos);
        if (motivo == null) {
            return false;
        }
        if (!HorarioBloqueado.guardarTodos(nuevos)) {
            Notificador.error("No se pudo guardar", "El bloqueo no se guardó. Revisa el rango e inténtalo de nuevo.");
            return false;
        }
        if (!conflictos.isEmpty()) {
            ControladorCorreo.notificarCancelaciones(
                    conflictos, Paciente.todos(), motivo, HorarioConfigController::avisoNotificacion);
        }
        return true;
    }

    private void editarBloqueo(HorarioBloqueado bloqueo) {
        bloqueoEditando = bloqueo;
        LocalDateTime inicio = bloqueo.getFechaInicio();
        LocalDateTime fin = bloqueo.getFechaFin();
        if (inicio == null || fin == null) {
            return;
        }

        bloqueoDesde.setValue(inicio.toLocalDate());
        boolean diaCompleto = inicio.toLocalTime().equals(LocalTime.MIDNIGHT)
                && fin.toLocalTime().equals(LocalTime.MIDNIGHT)
                && fin.toLocalDate().isAfter(inicio.toLocalDate());
        bloqueoTodoElDia.setSelected(diaCompleto);
        if (diaCompleto) {
            bloqueoHasta.setValue(fin.toLocalDate().minusDays(1));
        } else {
            bloqueoHasta.setValue(fin.toLocalDate());
            // Los combos solo tienen horas exactas: una hora intermedia se redondea hacia fuera para no encoger el bloqueo.
            LocalTime horaFin = fin.toLocalTime();
            seleccionarHora(bloqueoHoraInicio, inicio.toLocalTime().truncatedTo(ChronoUnit.HOURS));
            seleccionarHora(bloqueoHoraFin, horaFin.equals(horaFin.truncatedTo(ChronoUnit.HOURS))
                    ? horaFin : horaFin.truncatedTo(ChronoUnit.HOURS).plusHours(1));
        }
        btnGuardarBloqueo.setText("Guardar cambios");
    }

    private void eliminarBloqueo(HorarioBloqueado bloqueo) {
        if (!confirmar("Eliminar bloqueo",
                "¿Eliminar el bloqueo " + PlanificadorAgenda.formatoRango(
                        bloqueo.getFechaInicio(), bloqueo.getFechaFin())
                        + "? Las citas que se cancelaron al bloquear no se restauran.")) {
            return;
        }
        if (!bloqueo.eliminar()) {
            Notificador.error("No se pudo eliminar",
                    "El bloqueo no se eliminó. Inténtalo de nuevo.");
            return;
        }
        cambios = true;
        if (bloqueoEditando != null
                && bloqueoEditando.getIdHorarioBloqueado() == bloqueo.getIdHorarioBloqueado()) {
            limpiarFormularioBloqueo();
        }
        recargarListas();
    }

    private void limpiarFormularioBloqueo() {
        bloqueoEditando = null;
        bloqueoDesde.setValue(LocalDate.now());
        bloqueoHasta.setValue(LocalDate.now());
        bloqueoTodoElDia.setSelected(true);
        seleccionarHora(bloqueoHoraInicio, LocalTime.of(9, 0));
        seleccionarHora(bloqueoHoraFin, LocalTime.of(18, 0));
        btnGuardarBloqueo.setText("Agregar bloqueo");
    }

    private static boolean cubre(Horario horario, LocalDateTime fecha) {
        if (horario == null || fecha == null || horario.getDia() == null
                || horario.getHoraInicio() == null || horario.getHoraFin() == null) {
            return false;
        }
        DiaSemana dia = DiaSemana.desdeDayOfWeek(fecha.getDayOfWeek());
        if (dia != horario.getDia()) {
            return false;
        }
        LocalDateTime inicio = fecha.toLocalDate().atTime(horario.getHoraInicio());
        LocalDateTime fin = fecha.toLocalDate().atTime(horario.getHoraFin());
        return !fecha.isBefore(inicio) && !fecha.plusMinutes(PlanificadorAgenda.DURACION_CITA_MINUTOS)
                .isAfter(fin);
    }

    private static VBox campo(String etiqueta, Node control) {
        Label label = new Label(etiqueta);
        label.getStyleClass().add("field-label");
        VBox caja = new VBox(4, label, control);
        HBox.setHgrow(caja, Priority.ALWAYS);
        VBox.setVgrow(caja, Priority.NEVER);
        return caja;
    }

    private static Label mensajeVacio(String texto) {
        Label label = new Label(texto);
        label.getStyleClass().add("hint");
        return label;
    }

    private static void seleccionarHora(ComboBox<LocalTime> combo, LocalTime hora) {
        if (hora != null && combo.getItems().contains(hora)) {
            combo.getSelectionModel().select(hora);
        }
    }

    private boolean confirmar(String titulo, String mensaje) {
        return Confirmacion.destructiva(titulo, mensaje, "Eliminar");
    }

    /**
     * Lista las citas que se cancelarán al bloquear y pide confirmación con un motivo opcional. Devuelve null si
     * se rechaza; si no, el motivo escrito ("" si no hay citas o se dejó vacío). El motivo solo se usa en el
     * correo a los pacientes: no se guarda.
     */
    static String confirmarCancelacionCitas(List<Cita> citas) {
        if (citas.isEmpty()) {
            return "";
        }
        Map<String, Paciente> pacientes = Paciente.todos();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        StringBuilder texto = new StringBuilder();
        citas.stream().sorted(Comparator.comparing(Cita::getFechaCita)).forEach(cita -> {
            Paciente p = pacientes.get(cita.getCurp());
            String nombre = p == null ? cita.getCurp()
                    : (p.getNombre() + " " + p.getApellidoPaterno() + " " + p.getApellidoMaterno()).replace("null", "").trim();
            texto.append(formato.format(cita.fechaHora()))
                    .append(" · ").append(nombre).append('\n');
        });
        TextArea lista = new TextArea(texto.toString());
        lista.setEditable(false);
        lista.setPrefRowCount(Math.min(citas.size(), 10) + 1);

        TextField motivo = new TextField();
        motivo.setPromptText("Motivo (opcional): se incluye en el correo a los pacientes");
        motivo.getStyleClass().add("form-field");

        boolean aceptado = Confirmacion.preguntar("¿Cancelar las citas del periodo?",
                "Se cancelarán " + citas.size() + " cita(s) agendada(s) en este periodo:",
                "Cancelar citas y bloquear", true, new VBox(8, lista, motivo));
        return aceptado ? motivo.getText().trim() : null;
    }

    /** Resumen del envío de correos de cancelación; lo reutilizan Agenda y Consulta. */
    static void avisoNotificacion(int enviados, int total) {
        if (enviados == total) {
            Notificador.exito("Cancelación notificada",
                    "Se notificó por correo a " + enviados + " paciente(s).");
        } else {
            Notificador.advertencia("Notificación incompleta",
                    "Se notificó por correo a " + enviados + " de " + total + " paciente(s). "
                            + "Los demás no tienen correo o el SMTP no está configurado.");
        }
    }
}
