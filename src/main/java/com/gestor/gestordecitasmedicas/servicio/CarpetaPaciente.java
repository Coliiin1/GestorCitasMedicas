package com.gestor.gestordecitasmedicas.servicio;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.DosFileAttributeView;

/**
 * Administra la carpeta de cada paciente dentro de la carpeta de PDF configurada.
 * Cada carpeta lleva un archivo oculto {@value #MARCA} con la CURP, de modo que se
 * identifica al paciente aunque cambie su nombre o existan homónimos.
 */
public final class CarpetaPaciente {

    static final String MARCA = ".paciente";
    public static final String RECETAS = "recetas";

    private CarpetaPaciente() {}

    /** Devuelve la carpeta del paciente; la crea, o la renombra si el nombre cambió. */
    public static File resolver(File base, String curp, String nombre) throws IOException {
        String deseado = sanear(nombre);
        if (deseado.isEmpty()) deseado = sanear(curp);
        if (deseado.isEmpty()) throw new IOException("El paciente no tiene nombre ni CURP.");
        Files.createDirectories(base.toPath());

        File existente = buscarPorCurp(base, curp);
        if (existente != null) {
            if (existente.getName().equals(deseado)
                    || existente.getName().equals(deseado + " (" + sanear(curp) + ")")) return existente;
            File destino = libre(base, deseado, curp);
            try {
                Files.move(existente.toPath(), destino.toPath());
                return destino;
            } catch (IOException e) {
                System.err.println("No se pudo renombrar la carpeta del paciente: " + e.getMessage());
                return existente;
            }
        }
        File nueva = libre(base, deseado, curp);
        Files.createDirectories(nueva.toPath());
        marcar(nueva, curp);
        return nueva;
    }

    /** Subcarpeta de recetas del paciente (se crea si no existe). */
    public static File recetas(File carpetaPaciente) throws IOException {
        File recetas = new File(carpetaPaciente, RECETAS);
        Files.createDirectories(recetas.toPath());
        return recetas;
    }

    /** Elimina caracteres inválidos en Windows y normaliza espacios. */
    static String sanear(String nombre) {
        if (nombre == null) return "";
        String limpio = nombre.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").replaceAll("\\s+", " ").trim();
        limpio = limpio.replaceAll("[. ]+$", "");
        if (limpio.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?")) limpio = limpio + "_";
        return limpio;
    }

    /** Nombre libre: el deseado si no lo ocupa otra CURP; si no, "Nombre (CURP)". */
    private static File libre(File base, String deseado, String curp) {
        File candidato = new File(base, deseado);
        if (!candidato.exists()) return candidato;
        return new File(base, deseado + " (" + sanear(curp) + ")");
    }

    private static File buscarPorCurp(File base, String curp) {
        File[] hijos = base.listFiles(File::isDirectory);
        if (hijos == null) return null;
        for (File hijo : hijos) {
            Path marca = new File(hijo, MARCA).toPath();
            if (!Files.isRegularFile(marca)) continue;
            try {
                if (new String(Files.readAllBytes(marca), StandardCharsets.UTF_8).trim().equalsIgnoreCase(curp.trim()))
                    return hijo;
            } catch (IOException ignorado) { /* carpeta ilegible: se omite */ }
        }
        return null;
    }

    private static void marcar(File carpeta, String curp) throws IOException {
        Path marca = new File(carpeta, MARCA).toPath();
        Files.write(marca, curp.trim().getBytes(StandardCharsets.UTF_8));
        try {
            DosFileAttributeView vista = Files.getFileAttributeView(marca, DosFileAttributeView.class);
            if (vista != null) vista.setHidden(true);
        } catch (IOException | UnsupportedOperationException ignorado) { /* ocultar es opcional */ }
    }
}
