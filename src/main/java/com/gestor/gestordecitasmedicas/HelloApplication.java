package com.gestor.gestordecitasmedicas;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        cargarFuenteIconos();
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1368, 768);
        stage.setTitle("Gestor de Citas Medicas");
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    /** Registra la fuente de iconos antes de crear la interfaz y avisa si no se pudo (iconos como cuadros). */
    private static void cargarFuenteIconos() {
        String ruta = "/com/gestor/gestordecitasmedicas/fonts/uicons-regular-rounded.ttf";
        try (InputStream fuente = HelloApplication.class.getResourceAsStream(ruta)) {
            if (fuente == null || Font.loadFont(fuente, 14) == null) {
                System.err.println("No se pudo cargar la fuente de iconos " + ruta
                        + ". Ejecuta Maven > Reload project y Build > Rebuild Project.");
            }
        } catch (IOException ex) {
            System.err.println("Error al leer la fuente de iconos: " + ex.getMessage());
        }
    }
}
