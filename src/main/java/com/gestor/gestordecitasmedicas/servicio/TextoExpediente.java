package com.gestor.gestordecitasmedicas.servicio;

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
                + "\nPersonales patológicos (datos previos): " + valor(e.getAntecedentesPersonales())
                + "\nObservaciones: " + valor(e.getObservaciones());
    }

    public static String historia(Expediente e) {
        List<String> pendientes = ValidacionClinica.historia(e);
        return (pendientes.isEmpty() ? "Captura completa para revisión y firma autógrafa."
                : "BORRADOR: historia clínica incompleta (" + pendientes.size() + " datos pendientes).")
                + "\n\n1. ESTABLECIMIENTO"
                + "\nTipo de establecimiento: " + valor(e.getTipoEstablecimiento())
                + "\nNombre del establecimiento: " + valor(e.getNombreEstablecimiento())
                + "\nDomicilio completo del establecimiento: " + valor(e.getDomicilioEstablecimiento())
                + "\nInstitución de pertenencia (si aplica): " + valor(e.getInstitucionEstablecimiento())
                + "\nRazón o denominación social (si aplica): " + valor(e.getPropietario())
                + "\n\n2. INTERROGATORIO Y ANTECEDENTES"
                + "\nPaciente: " + (e.getDatosPaciente() == null ? "No registrado" : nombre(e.getDatosPaciente()))
                + " | Edad a la elaboración: " + edadHistoria(e)
                + " | Sexo: " + (e.getDatosPaciente() == null ? "No registrado" : valor(e.getDatosPaciente().getSexo()))
                + "\nGrupo étnico (si aplica): " + valor(e.getGrupoEtnico())
                + "\nAntecedentes heredo-familiares: " + valor(e.getAntecedentesFamiliares())
                + "\nAntecedentes personales patológicos: " + valor(e.getAntecedentesPersonales())
                + "\nAntecedentes personales no patológicos: " + valor(e.getAntecedentesNoPatologicos())
                + "\nUso y dependencia de tabaco, alcohol y otras sustancias: " + valor(e.getConsumoSustancias())
                + "\nAlergias y advertencias: " + valor(e.getAlergias())
                + "\nTipo de sangre: " + valor(e.getTipoSangre())
                + "\n\n3. PADECIMIENTO Y EXPLORACIÓN"
                + "\nPadecimiento actual y tratamientos previos convencionales, alternativos y tradicionales: " + valor(e.getPadecimientoActual())
                + "\nInterrogatorio por aparatos y sistemas: " + valor(e.getInterrogatorioSistemas())
                + "\nExploración física: habitus exterior, cabeza, cuello, tórax, abdomen, miembros y genitales: " + valor(e.getExploracionFisica())
                + "\nSignos vitales: temperatura, presión arterial, frecuencias cardíaca y respiratoria, peso y talla (con unidades): " + valor(e.getSignosHistoria())
                + "\n\n4. ESTUDIOS, DIAGNÓSTICO Y TRATAMIENTO"
                + "\nResultados previos y actuales de estudios de laboratorio, gabinete y otros: " + valor(e.getResultadosEstudios())
                + "\nDiagnósticos o problemas clínicos: " + valor(e.getDiagnostico())
                + "\nPronóstico: " + valor(e.getPronostico())
                + "\nIndicación terapéutica: " + valor(e.getIndicacionTerapeutica())
                + "\nObservaciones: " + valor(e.getObservaciones())
                + "\n\n5. AUTOR Y FECHA DE ELABORACIÓN"
                + "\nNombre completo de quien elabora la historia: " + valor(e.getAutorHistoria())
                + "\nCédula profesional de quien elabora la historia: " + valor(e.getCedulaHistoria())
                + "\nFecha y hora de elaboración (aaaa-mm-ddThh:mm): " + valor(e.getFechaHoraHistoria())
                + "\n"
                + "\nFirma autógrafa de quien elabora: ______________________________"
                + "\nPendiente de firma autógrafa; la captura del nombre no constituye firma electrónica.";
    }

    public static String edadHistoria(Expediente e) {
        if (e.getDatosPaciente() == null || e.getFechaHoraHistoria() == null) return "No registrada";
        try {
            java.time.LocalDateTime elaboracion = java.time.LocalDateTime.parse(e.getFechaHoraHistoria());
            return edad(e.getDatosPaciente(), Date.from(elaboracion.atZone(java.time.ZoneId.systemDefault()).toInstant()));
        } catch (java.time.format.DateTimeParseException ex) { return "No registrada"; }
    }

    public static String edad(Paciente p, Date fecha) {
        if (p.getFechaNacimiento() == null || fecha == null) return "No registrada";
        java.time.LocalDate nacimiento = new Date(p.getFechaNacimiento().getTime()).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        java.time.LocalDate atencion = new Date(fecha.getTime()).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        if (nacimiento.isAfter(atencion)) return "Fecha de nacimiento posterior a la atención";
        java.time.Period periodo = java.time.Period.between(nacimiento, atencion);
        return periodo.getYears() + " años, " + periodo.getMonths() + " meses, " + periodo.getDays() + " días";
    }

    private static String opcional(String etiqueta, String texto) {
        return texto == null || texto.trim().isEmpty() ? "" : "\n" + etiqueta + ": " + texto;
    }

    public static String consultas(Expediente e) {
        StringBuilder texto = new StringBuilder();
        for (Consulta c : e.getConsultas()) {
            String fecha = c.getFechaConsulta() == null ? "Sin fecha"
                    : c.getFechaConsulta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            texto.append("Consulta #").append(c.getIdConsulta()).append(" - ").append(fecha)
                    .append("\nPaciente al atender: ").append(valor(c.getNombrePaciente()))
                    .append(" | Edad: ").append(valor(c.getEdadPaciente()))
                    .append(" | Sexo: ").append(valor(c.getSexoPaciente()))
                    .append(" | Expediente: ").append(e.getIdExpediente())
                    .append("\nMotivo: ").append(valor(c.getMotivo()))
                    .append("\nPadecimiento: ").append(valor(c.getPadecimiento()))
                    .append(opcional("Evolución", c.getEvolucion()))
                    .append("\nExploración física: ").append(valor(c.getExploracionFisica()))
                    .append(opcional("Resultados de estudios", c.getResultadosEstudios()))
                    .append("\nDiagnósticos: ").append(valor(c.getDiagnostico()))
                    .append(opcional("Pronóstico", c.getPronostico()))
                    .append("\nTratamiento e indicaciones: ").append(valor(c.getIndicaciones()))
                    .append("\nObservaciones previas: ").append(valor(c.getObservaciones()))
                    .append("\nAutor: ").append(valor(c.getNombreMedico()))
                    .append(" | Cédula profesional: ").append(valor(c.getCedulaMedico()))
                    .append("\nSignos vitales: ").append(signos(c.getSignosVitales()))
                    .append("\nFirma autógrafa: ______________________________ (pendiente de firma)\n\n");
        }
        return texto.length() == 0 ? "No hay consultas registradas para este paciente." : texto.toString().trim();
    }

    public static List<Receta> recetasOrdenadas(Expediente e) {
        List<Receta> recetas = new ArrayList<>(e.getRecetas().values());
        recetas.sort(Comparator.comparing(Receta::getFecha, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Comparator.comparingInt(Receta::getIdReceta).reversed()));
        return recetas;
    }

    private static void extra(StringBuilder texto, String etiqueta, String v) {
        if (v != null && !v.trim().isEmpty()) texto.append("\n").append(etiqueta).append(": ").append(v.trim());
    }

    public static String recetas(Expediente e) {
        StringBuilder texto = new StringBuilder();
        for (Receta r : recetasOrdenadas(e)) {
            texto.append("Receta #").append(r.getIdReceta()).append(" - ").append(fecha(r.getFecha()))
                    .append("\nDiagnóstico: ").append(valor(r.getDiagnostico()));
            if (r.getMedicamentos().isEmpty()) texto.append("\nSin medicamentos registrados.");
            for (Medicamento m : r.getMedicamentos()) {
                texto.append("\nMedicamento: ").append(valor(m.getNombreMedicamento()))
                        .append("\nDosis y posología: ").append(valor(m.getDosis()))
                        .append(" | Duración: ").append(valor(m.getDuracion()));
                extra(texto, "Marca", m.getDenominacionDistintiva());
                extra(texto, "Cantidad", m.getCantidad());
                extra(texto, "Tipo de receta", m.getGrupoRegulatorio());
                extra(texto, "Concentración", m.getConcentracion());
                extra(texto, "Presentación", m.getPresentacion());
                extra(texto, "Vía", m.getViaAdministracion());
                extra(texto, "Frecuencia", m.getFrecuencia());
            }
            texto.append("\n\n");
        }
        return texto.length() == 0 ? "No hay recetas registradas para este paciente." : texto.toString().trim();
    }

    public static String signos(SignosVitales s) {
        if (s == null) return "No registrados";
        return "Presión arterial: " + valor(s.getPresionArterial()) + " mmHg; frecuencia cardíaca: "
                + s.getFrecuenciaCardiaca() + " latidos por minuto; frecuencia respiratoria: "
                + s.getFrecuenciaRespiratoria() + " respiraciones por minuto; temperatura: "
                + s.getTemperatura() + " °C; peso: " + s.getPeso() + " kg; talla: " + s.getTalla()
                + " cm; saturación de oxígeno: " + s.getSaturacionOxigeno() + " %.";
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
