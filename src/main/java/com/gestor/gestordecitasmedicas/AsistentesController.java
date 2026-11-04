package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.modelo.Asistente;
import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Optional;

public class AsistentesController implements PanelControlado, PanelRefrescable {
    @FXML private TextField txtBuscar;
    @FXML private VBox listaAsistentes;
    @FXML private Label lblContador;

    private final BaseDatos baseDatos = new BaseDatos();
    private final ArrayList<Asistente> asistentes = new ArrayList<>();
    private MainController mainController;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        refrescar();
    }

    @Override
    public void refrescar() {
        asistentes.clear();
        asistentes.addAll(baseDatos.cargarAsistentes());
        aplicarFiltro();
    }

    @FXML
    private void onBuscar() {
        aplicarFiltro();
    }

    @FXML
    private void onNuevo() {
        AsistenteEditarController editor = mainController.controlador("asistente-editar-panel.fxml");
        editor.modoNuevo();
        mainController.ir("asistente-editar-panel.fxml");
    }

    private void aplicarFiltro() {
        String filtro = normalizar(txtBuscar.getText());
        ArrayList<Asistente> filtrados = new ArrayList<>();
        for (Asistente asistente : asistentes) {
            if (filtro.isEmpty() || normalizar(nombreCompleto(asistente)).contains(filtro)) {
                filtrados.add(asistente);
            }
        }
        mostrarAsistentes(filtrados);
        lblContador.setText("Asistentes Registrados: " + asistentes.size());
    }

    private void mostrarAsistentes(ArrayList<Asistente> filtrados) {
        listaAsistentes.getChildren().clear();
        if (filtrados.isEmpty()) {
            Label vacio = new Label("No se encontraron asistentes registrados.");
            vacio.getStyleClass().add("hint");
            listaAsistentes.getChildren().add(vacio);
            return;
        }
        for (Asistente asistente : filtrados) {
            listaAsistentes.getChildren().add(crearFila(asistente));
        }
    }

    private HBox crearFila(Asistente asistente) {
        Label nombre = new Label(nombreCompleto(asistente));
        nombre.getStyleClass().add("row-name");

        Label detalle = new Label("ID: #" + asistente.getIdAsistente());
        detalle.getStyleClass().add("hint");

        VBox datos = new VBox(4, nombre, detalle);
        datos.setMinWidth(200);

        Label estado = new Label("Asistente Registrado");
        estado.getStyleClass().add("chip");

        Region separador = new Region();
        HBox.setHgrow(separador, Priority.ALWAYS);

        Button editar = new Button("Editar");
        editar.getStyleClass().add("btn-edit");
        editar.setOnAction(e -> editar(asistente));

        Button eliminar = new Button("Eliminar");
        eliminar.getStyleClass().add("btn-del");
        eliminar.setOnAction(e -> eliminar(asistente));

        HBox fila = new HBox(16, datos, estado, separador, editar, eliminar);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("row-card");
        return fila;
    }

    private void editar(Asistente asistente) {
        AsistenteEditarController editor = mainController.controlador("asistente-editar-panel.fxml");
        editor.modoEditar(asistente);
        mainController.ir("asistente-editar-panel.fxml");
    }

    private void eliminar(Asistente asistente) {
        ButtonType eliminar = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar asistente");
        confirmacion.setHeaderText("¿Eliminar a " + nombreCompleto(asistente) + "?");
        confirmacion.setContentText("Esta acción no se puede deshacer.");
        confirmacion.getButtonTypes().setAll(eliminar, cancelar);

        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (!respuesta.isPresent() || respuesta.get() != eliminar) {
            return;
        }

        if (baseDatos.eliminarAsistente(asistente.getIdAsistente())) {
            refrescar();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Asistente eliminado",
                    "El asistente se eliminó correctamente.");
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo eliminar",
                    "Ocurrió un error al eliminar el asistente. Inténtalo de nuevo.");
        }
    }

    private static String nombreCompleto(Asistente asistente) {
        StringBuilder nombre = new StringBuilder();
        agregar(nombre, asistente.getNombre());
        agregar(nombre, asistente.getApellidoPaterno());
        agregar(nombre, asistente.getApellidoMaterno());
        return nombre.toString();
    }

    private static void agregar(StringBuilder texto, String parte) {
        if (parte == null || parte.trim().isEmpty()) {
            return;
        }
        if (texto.length() > 0) {
            texto.append(' ');
        }
        texto.append(parte.trim());
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim().toLowerCase();
    }

    private static void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
