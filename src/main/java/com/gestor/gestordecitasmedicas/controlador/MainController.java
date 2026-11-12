package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.Administrador;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainController {
    @FXML private StackPane contenido;
    @FXML private Pane menu;
    @FXML private Button btnConfig;
    @FXML private Label lblConsultorio;
    @FXML private Label lblMedico;
    @FXML private Button btnNotificaciones;

    private boolean esAdmin;
    private int idAdmin;
    // Último botón del menú con resaltado, para conservarlo en paneles fuera del menú (p. ej. edición).
    private Button ultimoActivo;
    // Se cachean para conservar el estado de cada panel (p. ej. el cronómetro de la cita).
    private final Map<String, Node> paneles = new HashMap<>();
    private final Map<String, Object> controladores = new HashMap<>();

    /** Botones de módulo: los del menú más Configuración, que está junto a "Cerrar sesión". */
    private List<Node> modulos() {
        List<Node> l = new ArrayList<>(menu.getChildren());
        l.add(btnConfig);
        return l;
    }

    @FXML
    private void initialize() {
        PanelNotificaciones.instalar(btnNotificaciones);
    }

    public void setIdAdministrador(int id) {
        idAdmin = id;
    }

    public int idAdministrador() {
        return idAdmin;
    }

    public boolean esAdministrador() {
        return esAdmin;
    }

    /** Muestra en la barra lateral los datos del consultorio guardados en la tabla Administrador. */
    public void aplicarConfiguracion() {
        // Sin sesión de admin se toma el primer administrador (el consultorio es de un solo médico).
        Administrador a = Administrador.buscar(esAdmin ? idAdmin : 1);
        if (a == null) return;
        if (a.getNombreEstablecimiento() != null && !a.getNombreEstablecimiento().trim().isEmpty())
            lblConsultorio.setText(a.getNombreEstablecimiento());
        lblMedico.setText(("Dr. " + a.getNombre() + " " + a.getApellidoPaterno()).trim());
    }

    /** Ajusta menú y navegación según el rol ("admin" o "asis") y abre la pantalla inicial. */
    public void configurarRol(String rol, int idUsuario) {
        esAdmin = "admin".equals(rol);
        idAdmin = idUsuario;
        aplicarConfiguracion();
        for (Node n : modulos()) {
            if (n instanceof Button) {
                boolean permitido = destinoPermitido((String) n.getUserData());
                n.setVisible(permitido);
                n.setManaged(permitido);
            }
        }
        ir(esAdmin ? "dashboard-panel.fxml" : "agenda-panel.fxml");
    }

    private boolean destinoPermitido(String fxml) {
        return esAdmin || "agenda-panel.fxml".equals(fxml) || "paciente-editar-panel.fxml".equals(fxml);
    }

    @FXML
    private void onNavegar(ActionEvent e) {
        navegar((String) ((Button) e.getSource()).getUserData());
    }

    /** Navegación iniciada por el usuario: si hay una cita en curso pide confirmación antes de salir. */
    private boolean confirmarSalidaDeCita(String destino) {
        ConsultaController consulta = (ConsultaController) controladores.get("consulta-panel.fxml");
        if (consulta == null || !consulta.enCurso() || "consulta-panel.fxml".equals(destino)) return true;
        if (!Confirmacion.destructiva("¿Salir de la cita en curso?",
                "Hay una cita en curso. Si sales, se perderá el progreso no guardado.", "Salir de la cita")) {
            return false;
        }
        consulta.abandonar();
        return true;
    }

    private void navegar(String fxml) {
        if (confirmarSalidaDeCita(fxml)) ir(fxml);
    }

    @FXML
    private void onCerrarSesion() {
        if (!confirmarSalidaDeCita(null)) return;
        PanelNotificaciones.cerrar();
        Notificador.limpiar(); // el historial es de la sesión
        try {
            javafx.scene.Parent login = new FXMLLoader(getClass().getResource("/com/gestor/gestordecitasmedicas/login-view.fxml")).load();
            contenido.getScene().setRoot(login);
            Ui.entrada(login, 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void ir(String fxml) {
        if (!destinoPermitido(fxml)) return;
        Node panel = obtenerPanel(fxml);
        boolean cambia = !contenido.getChildren().contains(panel);
        contenido.getChildren().setAll(panel);
        if (cambia) Ui.entrada(panel, 0);

        Object controlador = controladores.get(fxml);
        if (controlador instanceof PanelRefrescable) {
            ((PanelRefrescable) controlador).refrescar();
        }

        Button activo = null;
        for (Node n : modulos()) {
            n.getStyleClass().remove("nav-item-active");
            if (n instanceof Button && fxml.equals(n.getUserData())) activo = (Button) n;
        }
        // Los paneles fuera del menú (p. ej. edición de paciente) mantienen el resaltado anterior.
        if (activo == null) activo = ultimoActivo;
        ultimoActivo = activo;
        if (activo != null) activo.getStyleClass().add("nav-item-active");
    }

    private Node obtenerPanel(String fxml) {
        return paneles.computeIfAbsent(fxml, this::cargar);
    }

    @SuppressWarnings("unchecked")
    public <T> T controlador(String fxml) {
        obtenerPanel(fxml);
        return (T) controladores.get(fxml);
    }

    private Node cargar(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(MainController.class.getResource("/com/gestor/gestordecitasmedicas/" + fxml));
            Node panel = loader.load();

            Object controlador = loader.getController();
            if (controlador != null) {
                controladores.put(fxml, controlador);
                if (controlador instanceof PanelControlado) {
                    ((PanelControlado) controlador).setMainController(this);
                }
            }

            // Botones de panel con styleClass "nav-link" y userData = destino. Se escucha el ActionEvent que sube
            // al panel porque lookupAll no ve dentro de un ScrollPane sin skin.
            panel.addEventHandler(ActionEvent.ACTION, e -> {
                if (e.getTarget() instanceof Button) {
                    Button boton = (Button) e.getTarget();
                    if (boton.getStyleClass().contains("nav-link")) {
                        navegar((String) boton.getUserData());
                    }
                }
            });
            return panel;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
