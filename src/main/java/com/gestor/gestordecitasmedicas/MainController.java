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

    // Se cachean para conservar el estado de cada panel (p. ej. el cronómetro de la cita).
    private final Map<String, Node> paneles = new HashMap<>();

    @FXML
    private void initialize() {
        btnCita.fire();
    }

    @FXML
    private void onNavegar(ActionEvent e) {
        ir((String) ((Button) e.getSource()).getUserData());
    }

    private void ir(String fxml) {
        contenido.getChildren().setAll(paneles.computeIfAbsent(fxml, this::cargar));
        Button activo = null;
        for (Node n : menu.getChildren()) {
            n.getStyleClass().remove("nav-item-active");
            // Dashboard y Agenda comparten destino: gana el último, Agenda.
            if (n instanceof Button && fxml.equals(n.getUserData())) activo = (Button) n;
        }
        activo.getStyleClass().add("nav-item-active");
    }

    private Node cargar(String fxml) {
        try {
            Node panel = FXMLLoader.load(MainController.class.getResource(fxml));
            // Botones de panel con styleClass "nav-link" y userData = destino. Se escucha el ActionEvent que sube
            // al panel porque lookupAll no ve dentro de un ScrollPane sin skin.
            panel.addEventHandler(ActionEvent.ACTION, e -> {
                if (e.getTarget() instanceof Button && ((Node) e.getTarget()).getStyleClass().contains("nav-link")) ir((String) ((Node) e.getTarget()).getUserData());
            });
            return panel;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
