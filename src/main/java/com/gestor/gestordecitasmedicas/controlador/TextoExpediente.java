package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.*;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/** Contenido compartido por la ficha clínica y su exportación. */
public final class TextoExpediente {
    private TextoExpediente() { }

    public static String valor(String texto) {
        return texto == null || texto.trim().isEmpty() ? "No registrado" : texto.trim();
    }

    public static String fecha(Date fecha) {
        return fecha == null ? "No registrada" : new SimpleDateFormat("dd/MM/yyyy").format(fecha);
    }

    public static String nombre(Paciente p) {
        return (valor(p.getNombre()) + " " + valor(p.getApellidoPaterno()) + " "
                + (p.getApellidoMaterno() == null ? "" : p.getApellidoMaterno())).trim();
    }

    public static String antecedentes(Expediente e) {
        return "Familiares: " + valor(e.getAntecedentesFamiliares())
                + "\nPersonales: " + valor(e.getAntecedentesPersonales())
                + "\nObservaciones: " + valor(e.getObservaciones());
    }

    public static String consultas(Expediente e) {
        StringBuilder texto = new StringBuilder();
        for (Consulta c : e.getConsultas()) {
            String fecha = c.getFechaConsulta() == null ? "Sin fecha"
                    : c.getFechaConsulta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            texto.append("Consulta #").append(c.getIdConsulta()).append(" - ").append(fecha)
                    .append("\nMotivo: ").append(valor(c.getMotivo()))
                    .append("\nPadecimiento: ").append(valor(c.getPadecimiento()))
                    .append("\nObservaciones: ").append(valor(c.getObservaciones())).append("\n\n");
        }
        return texto.length() == 0 ? "No hay consultas registradas para este paciente." : texto.toString().trim();
    }

    public static List<Receta> recetasOrdenadas(Expediente e) {
        List<Receta> recetas = new ArrayList<>(e.getRecetas().values());
        recetas.sort(Comparator.comparing(Receta::getFecha, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Comparator.comparingInt(Receta::getIdReceta).reversed()));
        return recetas;
    }

    public static String recetas(Expediente e) {
        StringBuilder texto = new StringBuilder();
        for (Receta r : recetasOrdenadas(e)) {
            texto.append("Receta #").append(r.getIdReceta()).append(" - ").append(fecha(r.getFecha()))
                    .append("\nDiagnóstico: ").append(valor(r.getDiagnostico()));
            if (r.getMedicamentos().isEmpty()) texto.append("\nSin medicamentos registrados.");
            for (Medicamento m : r.getMedicamentos()) {
                texto.append("\nMedicamento: ").append(valor(m.getNombreMedicamento()))
                        .append("\nConcentración: ").append(valor(m.getConcentracion()))
                        .append(" | Dosis: ").append(valor(m.getDosis()))
                        .append("\nFrecuencia: ").append(valor(m.getFrecuencia()))
                        .append(" | Duración: ").append(valor(m.getDuradcion()));
            }
            texto.append("\n\n");
        }
        return texto.length() == 0 ? "No hay recetas registradas para este paciente." : texto.toString().trim();
    }

    public static String signos(Expediente e) {
        StringBuilder texto = new StringBuilder();
        for (Receta r : recetasOrdenadas(e)) {
            SignosVitales s = r.getSignosVitales();
            if (s == null) continue;
            texto.append(fecha(r.getFecha())).append(" - Receta #").append(r.getIdReceta())
                    .append("\nPresión arterial: ").append(valor(s.getPresionArterial()))
                    .append(" | Frecuencia cardíaca: ").append(s.getFrecuenciaCardiaca()).append(" lpm")
                    .append("\nFrecuencia respiratoria: ").append(s.getFrecuenciaRespiratoria()).append(" rpm")
                    .append(" | Temperatura: ").append(s.getTemperatura()).append(" °C")
                    .append("\nSaturación de oxígeno: ").append(s.getSaturacionOxigeno()).append(" %")
                    .append(" | Peso: ").append(s.getPeso()).append(" kg")
                    .append(" | Talla: ").append(s.getTalla()).append(" cm\n\n");
        }
        return texto.length() == 0 ? "No hay signos vitales registrados para este paciente." : texto.toString().trim();
    }
}
