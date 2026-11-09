package com.gestor.gestordecitasmedicas;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {
    @FXML private StackPane contenido;
    @FXML private Pane menu;
    @FXML private Button btnCita;
    @FXML private Button btnDashboard;

    // Se cachean para conservar el estado de cada panel (p. ej. el cronómetro de la cita).
    private final Map<String, Node> paneles = new HashMap<>();
    private final Map<String, Object> controladores = new HashMap<>();
    // Último botón del menú con resaltado, para conservarlo en paneles fuera del menú (p. ej. edición).
    private Button ultimoActivo;

    private boolean esAdmin;
    private boolean rolConfigurado;

    public boolean esAdministrador() {
        return esAdmin;
    }

    /** Ajusta menú y navegación según el rol ("admin" o "asis") y abre la pantalla inicial. */
    public void configurarRol(String rol) {
        esAdmin = "admin".equals(rol);
        rolConfigurado = true;
        for (Node n : menu.getChildren()) {
            if (n instanceof Button) {
                boolean permitido = destinoPermitido((String) n.getUserData())
                        && n != btnCita && (esAdmin || n != btnDashboard) && !"consulta-panel.fxml".equals(n.getUserData());
                n.setVisible(permitido);
                n.setManaged(permitido);
            }
        }
        ir(esAdmin ? "dashboard-panel.fxml" : "agenda-panel.fxml");
    }

    private boolean destinoPermitido(String fxml) {
        if (!rolConfigurado || esAdmin) return true;
        return "agenda-panel.fxml".equals(fxml) || "paciente-editar-panel.fxml".equals(fxml);
    }

    @FXML
    private void onNavegar(ActionEvent e) {
        ir((String) ((Button) e.getSource()).getUserData());
    }

    @FXML
    private void onCerrarSesion() {
        try {
            contenido.getScene().setRoot(new FXMLLoader(getClass().getResource("login-view.fxml")).load());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void ir(String fxml) {
        if (!destinoPermitido(fxml)) return;
        Node panel = obtenerPanel(fxml);
        contenido.getChildren().setAll(panel);

        Object controlador = controladores.get(fxml);
        if (controlador instanceof PanelRefrescable) {
            ((PanelRefrescable) controlador).refrescar();
        }

        Button activo = null;
        for (Node n : menu.getChildren()) {
            n.getStyleClass().remove("nav-item-active");
            if (n instanceof Button && fxml.equals(n.getUserData())) activo = (Button) n;
        }
        // Los paneles fuera del menú (p. ej. edición de paciente) mantienen el resaltado anterior.
        if (activo == null) activo = ultimoActivo;
        ultimoActivo = activo;
        if (activo != null) activo.getStyleClass().add("nav-item-active");
    }

    public Node obtenerPanel(String fxml) {
        return paneles.computeIfAbsent(fxml, this::cargar);
    }

    @SuppressWarnings("unchecked")
    public <T> T controlador(String fxml) {
        obtenerPanel(fxml);
        return (T) controladores.get(fxml);
    }

    private Node cargar(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(MainController.class.getResource(fxml));
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
                        ir((String) boton.getUserData());
                    }
                }
            });
            return panel;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
