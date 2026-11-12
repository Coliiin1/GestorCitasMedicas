package com.gestor.gestordecitasmedicas;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class HelloApplication extends Application {
    static final String[] FUENTES_MANROPE = {
            "/com/gestor/gestordecitasmedicas/fonts/Manrope/static/Manrope-ExtraLight.ttf",
            "/com/gestor/gestordecitasmedicas/fonts/Manrope/static/Manrope-Light.ttf",
            "/com/gestor/gestordecitasmedicas/fonts/Manrope/static/Manrope-Regular.ttf",
            "/com/gestor/gestordecitasmedicas/fonts/Manrope/static/Manrope-Medium.ttf",
            "/com/gestor/gestordecitasmedicas/fonts/Manrope/static/Manrope-SemiBold.ttf",
            "/com/gestor/gestordecitasmedicas/fonts/Manrope/static/Manrope-Bold.ttf",
            "/com/gestor/gestordecitasmedicas/fonts/Manrope/static/Manrope-ExtraBold.ttf"
    };

    @Override
    public void start(Stage stage) throws IOException {
        cargarFuentes();
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1368, 768);
        stage.setTitle("Gestor de Citas Medicas");
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    /** Registra la fuente de iconos y la familia tipográfica Manrope antes de crear la interfaz. */
    static void cargarFuentes() {
        cargarFuente("/com/gestor/gestordecitasmedicas/fonts/uicons-regular-rounded.ttf");
        for (String ruta : FUENTES_MANROPE) {
            cargarFuente(ruta);
        }
    }

    private static void cargarFuente(String ruta) {
        try (InputStream fuente = HelloApplication.class.getResourceAsStream(ruta)) {
            if (fuente == null || Font.loadFont(fuente, 14) == null) {
                System.err.println("No se pudo cargar la fuente " + ruta
                        + ". Ejecuta Maven > Reload project y Build > Rebuild Project.");
            }
        } catch (IOException ex) {
            System.err.println("Error al leer la fuente " + ruta + ": " + ex.getMessage());
        }
    }
}
