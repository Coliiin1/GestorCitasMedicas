package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.DiaSemana;
import com.gestor.gestordecitasmedicas.modelo.Horario;
import com.gestor.gestordecitasmedicas.modelo.HorarioBloqueado;
import com.gestor.gestordecitasmedicas.modelo.PlanificadorAgenda;
import javafx.geometry.Pos;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Configuración de horario semanal y fechas bloqueadas. Se abre únicamente
 * desde la agenda cuando la sesión es de administrador.
 */
public final class HorarioConfigController {
    private final Window owner;
    private final BaseDatos baseDatos;
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
    private TextField bloqueoMotivo;
    private Button btnGuardarBloqueo;
    private HorarioBloqueado bloqueoEditando;

    private boolean cambios;

    private HorarioConfigController(Window owner, BaseDatos baseDatos) {
        this.owner = owner;
        this.baseDatos = baseDatos;
    }

    public static boolean abrir(Window owner, BaseDatos baseDatos) {
        return new HorarioConfigController(owner, baseDatos).mostrar();
    }

    private boolean mostrar() {
        dialogo = new Dialog<>();
        if (owner != null) {
            dialogo.initOwner(owner);
        }
        dialogo.setTitle("Horario de trabajo y bloqueos");
        dialogo.getDialogPane().getStylesheets().add(
                HorarioConfigController.class.getResource("cita.css").toExternalForm());
        dialogo.getDialogPane().getStyleClass().add("expediente-editor");

        Label titulo = new Label("HORARIO DE TRABAJO Y FECHAS BLOQUEADAS");
        titulo.getStyleClass().add("field-label");
        Label subtitulo = new Label("Define los tramos semanales y las excepciones de la agenda.");
        subtitulo.getStyleClass().add("card-subtitle");
        VBox encabezado = new VBox(4, titulo, subtitulo);
        encabezado.getStyleClass().add("edit-header");
        dialogo.getDialogPane().setHeader(encabezado);

        TabPane tabs = new TabPane();
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

        comboInicio = comboHora(LocalTime.of(9, 0));
        comboFin = comboHora(LocalTime.of(13, 0));
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

        bloqueoHoraInicio = comboHora(LocalTime.of(9, 0));
        bloqueoHoraFin = comboHora(LocalTime.of(18, 0));
        bloqueoTodoElDia = new CheckBox("Todo el día");
        bloqueoTodoElDia.setSelected(true);
        bloqueoHoraInicio.setDisable(true);
        bloqueoHoraFin.setDisable(true);
        bloqueoTodoElDia.selectedProperty().addListener((observable, anterior, seleccionado) -> {
            bloqueoHoraInicio.setDisable(seleccionado);
            bloqueoHoraFin.setDisable(seleccionado);
        });

        bloqueoMotivo = new TextField();
        bloqueoMotivo.setPromptText("Motivo (opcional)");
        bloqueoMotivo.getStyleClass().add("search-field");

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
        formulario.add(campo("HASTA", bloqueoHasta), 1, 0);
        formulario.add(campo("HORA INICIO", bloqueoHoraInicio), 2, 0);
        formulario.add(campo("HORA FIN", bloqueoHoraFin), 3, 0);
        formulario.add(bloqueoTodoElDia, 0, 1);
        formulario.add(campo("MOTIVO", bloqueoMotivo), 1, 1, 3, 1);
        HBox acciones = new HBox(8, btnGuardarBloqueo, limpiar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        listaBloqueos = new VBox(8);
        ScrollPane scroll = new ScrollPane(listaBloqueos);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(240);

        VBox contenido = new VBox(12,
                new Label("Los bloqueos impiden agendar en ese periodo. Si hay citas activas, "
                        + "debes reprogramarlas o cancelarlas antes de bloquear."),
                formulario,
                acciones,
                scroll);
        contenido.getStyleClass().add("form-body");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return contenido;
    }

    private void recargarListas() {
        horarios.clear();
        horarios.addAll(baseDatos.cargarHorarios());
        bloqueos.clear();
        bloqueos.addAll(baseDatos.cargarBloqueos());
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
        if (bloqueos.isEmpty()) {
            listaBloqueos.getChildren().add(mensajeVacio("No hay fechas bloqueadas."));
            return;
        }
        List<HorarioBloqueado> ordenados = new ArrayList<>(bloqueos);
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
        String motivoTexto = bloqueo.getMotivo() == null || bloqueo.getMotivo().trim().isEmpty()
                ? "Sin motivo" : bloqueo.getMotivo().trim();
        Label motivo = new Label(motivoTexto);
        motivo.getStyleClass().add("hint");

        VBox datos = new VBox(4, nombre, motivo);

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
            aviso(Alert.AlertType.WARNING, "Tramo inválido",
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
                aviso(Alert.AlertType.WARNING, "Tramo superpuesto",
                        "Ya existe un tramo activo ese día en un horario que se cruza.");
                return;
            }
        }

        if (horarioEditando != null) {
            ArrayList<Cita> citas = baseDatos.citasActivasEnHorario(horarioEditando);
            for (Cita cita : citas) {
                LocalDateTime fecha = LocalDateTime.ofInstant(
                        cita.getFechaCita().toInstant(), ZoneId.systemDefault());
                if (!horario.isEstado() || !cubre(horario, fecha)) {
                    aviso(Alert.AlertType.WARNING, "Hay citas en ese tramo",
                            "No se puede modificar o desactivar el tramo porque hay "
                                    + citas.size() + " cita(s) activa(s). Reprograma o cancela primero.");
                    return;
                }
            }
        }

        if (!baseDatos.guardarHorario(horario)) {
            aviso(Alert.AlertType.ERROR, "No se pudo guardar",
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
        if (!baseDatos.eliminarHorario(horario.getIdHorario())) {
            aviso(Alert.AlertType.WARNING, "No se pudo eliminar",
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
        LocalDate desde = bloqueoDesde.getValue();
        LocalDate hasta = bloqueoHasta.getValue();
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            aviso(Alert.AlertType.WARNING, "Fechas inválidas",
                    "Selecciona un rango de fechas válido.");
            return;
        }

        LocalDateTime inicio;
        LocalDateTime fin;
        if (bloqueoTodoElDia.isSelected()) {
            inicio = desde.atStartOfDay();
            fin = hasta.plusDays(1).atStartOfDay();
        } else {
            LocalTime horaInicio = bloqueoHoraInicio.getSelectionModel().getSelectedItem();
            LocalTime horaFin = bloqueoHoraFin.getSelectionModel().getSelectedItem();
            if (horaInicio == null || horaFin == null || !horaInicio.isBefore(horaFin)) {
                aviso(Alert.AlertType.WARNING, "Horas inválidas",
                        "La hora de inicio debe ser anterior a la hora de fin.");
                return;
            }
            inicio = desde.atTime(horaInicio);
            fin = hasta.atTime(horaFin);
        }

        int idEditando = bloqueoEditando == null ? 0 : bloqueoEditando.getIdHorarioBloqueado();
        for (HorarioBloqueado existente : bloqueos) {
            if (existente.getIdHorarioBloqueado() != idEditando
                    && PlanificadorAgenda.seSolapan(inicio, fin,
                    existente.getFechaInicio(), existente.getFechaFin())) {
                aviso(Alert.AlertType.WARNING, "Bloqueo superpuesto",
                        "Ya existe un bloqueo que se cruza con ese periodo.");
                return;
            }
        }

        ArrayList<Cita> conflictos = baseDatos.citasActivasEnRango(inicio, fin);
        if (!conflictos.isEmpty()) {
            aviso(Alert.AlertType.WARNING, "Hay citas en el rango",
                    "No se puede bloquear porque hay " + conflictos.size()
                            + " cita(s) activa(s). Reprograma o cancela primero.");
            return;
        }

        HorarioBloqueado bloqueo = new HorarioBloqueado();
        bloqueo.setIdHorarioBloqueado(idEditando);
        bloqueo.setFechaInicio(inicio);
        bloqueo.setFechaFin(fin);
        bloqueo.setMotivo(bloqueoMotivo.getText() == null ? null : bloqueoMotivo.getText().trim());

        if (!baseDatos.guardarBloqueo(bloqueo)) {
            aviso(Alert.AlertType.ERROR, "No se pudo guardar",
                    "El bloqueo no se guardó. Revisa el rango e inténtalo de nuevo.");
            return;
        }
        cambios = true;
        limpiarFormularioBloqueo();
        recargarListas();
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
            seleccionarHora(bloqueoHoraInicio, inicio.toLocalTime());
            seleccionarHora(bloqueoHoraFin, fin.toLocalTime());
        }
        bloqueoMotivo.setText(bloqueo.getMotivo() == null ? "" : bloqueo.getMotivo());
        btnGuardarBloqueo.setText("Guardar cambios");
    }

    private void eliminarBloqueo(HorarioBloqueado bloqueo) {
        if (!confirmar("Eliminar bloqueo",
                "¿Eliminar el bloqueo " + PlanificadorAgenda.formatoRango(
                        bloqueo.getFechaInicio(), bloqueo.getFechaFin()) + "?")) {
            return;
        }
        if (!baseDatos.eliminarBloqueo(bloqueo.getIdHorarioBloqueado())) {
            aviso(Alert.AlertType.ERROR, "No se pudo eliminar",
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
        bloqueoMotivo.clear();
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

    private static ComboBox<LocalTime> comboHora(LocalTime seleccionada) {
        ComboBox<LocalTime> combo = new ComboBox<>();
        combo.getItems().setAll(PlanificadorAgenda.horasDelDia());
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
        seleccionarHora(combo, seleccionada);
        return combo;
    }

    private static void seleccionarHora(ComboBox<LocalTime> combo, LocalTime hora) {
        if (hora != null && combo.getItems().contains(hora)) {
            combo.getSelectionModel().select(hora);
        }
    }

    private boolean confirmar(String titulo, String mensaje) {
        ButtonType aceptar = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.getButtonTypes().setAll(aceptar, cancelar);
        return alerta.showAndWait().orElse(cancelar) == aceptar;
    }

    private static void aviso(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
