package com.gestor.gestordecitasmedicas;

import javafx.application.Application;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Launcher {
    public static void main(String[] args) throws IOException {
        if (Boolean.getBoolean("gestor.instalado")) {
            prepararInstalacion();
        }
        Application.launch(HelloApplication.class, args);
    }

    /** La app instalada no controla su directorio de trabajo: datos, PDF y bitácora van a carpetas del usuario. */
    private static void prepararInstalacion() throws IOException {
        String local = System.getenv("LOCALAPPDATA");
        Path datos = Paths.get(local != null ? local : System.getProperty("user.home"), "GestorCitasMedicas");
        Files.createDirectories(datos);
        System.setProperty("gestor.datos", datos.toString());
        System.setProperty("gestor.db.url", "jdbc:sqlite:" + datos.resolve("citas_medicas.db"));
        System.setProperty("gestor.pdf",
                Paths.get(System.getProperty("user.home"), "Documents", "GestorCitasMedicas", "PDF").toString());
        // Sin consola, la bitácora es el único rastro de un fallo.
        PrintStream log = new PrintStream(new FileOutputStream(datos.resolve("gestor.log").toFile(), true), true, "UTF-8");
        System.setOut(log);
        System.setErr(log);
    }
}
