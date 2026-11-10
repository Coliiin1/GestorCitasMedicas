package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Requisitos de captura para consulta externa y receta ordinaria para firma autógrafa. */
public final class ValidacionClinica {
    private ValidacionClinica() { }

    private static void requerido(List<String> errores, String etiqueta, String valor) {
        if (valor == null || valor.trim().isEmpty()) errores.add("Falta: " + etiqueta + ".");
    }

    public static List<String> receta(Receta r, boolean requiereMedicamentos) {
        List<String> errores = new ArrayList<>();
        if (r == null) { errores.add("No hay receta."); return errores; }
        if (r.getFecha() == null) errores.add("Falta la fecha de expedición.");
        requerido(errores, "nombre completo del paciente", r.getNombrePaciente());
        requerido(errores, "edad del paciente", r.getEdadPaciente());
        requerido(errores, "sexo del paciente", r.getSexoPaciente());
        requerido(errores, "nombre completo del prescriptor", r.getNombreMedico());
        requerido(errores, "cédula profesional", r.getCedulaMedico());
        if (r.getCedulaMedico() != null && !r.getCedulaMedico().matches("[0-9]{1,20}"))
            errores.add("La cédula profesional debe contener únicamente números.");
        if (r.getCedulaMedico() != null && r.getCedulaMedico().matches("0+"))
            errores.add("Capture una cédula profesional real; la cédula de ejemplo no es válida.");
        requerido(errores, "profesión", r.getProfesion());
        requerido(errores, "institución que expidió el título", r.getInstitucionTitulo());
        requerido(errores, "domicilio completo del prescriptor", r.getDomicilioMedico());
        requerido(errores, "nombre del establecimiento", r.getNombreEstablecimiento());
        requerido(errores, "domicilio completo del establecimiento", r.getDomicilioEstablecimiento());
        if (r.getEspecialidad() != null && !r.getEspecialidad().trim().isEmpty())
            requerido(errores, "registro de especialidad", r.getRegistroEspecialidad());
        requerido(errores, "diagnóstico o problemas clínicos", r.getDiagnostico());
        if (requiereMedicamentos && (r.getMedicamentos() == null || r.getMedicamentos().isEmpty()))
            errores.add("Agregue al menos un medicamento para exportar una receta.");
        if (r.getMedicamentos() != null) {
            int i = 0;
            for (Medicamento m : r.getMedicamentos()) {
                String prefijo = "medicamento " + (++i) + ": ";
                if (m == null) { errores.add(prefijo + "datos ausentes."); continue; }
                requerido(errores, prefijo + "denominación genérica", m.getNombreMedicamento());
                requerido(errores, prefijo + "dosis y posología", m.getDosis());
                requerido(errores, prefijo + "duración", m.getDuradcion());
                if (m.getGrupoRegulatorio() != null && !Arrays.asList("IV", "V", "VI").contains(m.getGrupoRegulatorio()))
                    errores.add(prefijo + "seleccione IV, V o VI para receta ordinaria. Las fracciones I, II y III requieren otro procedimiento de control y no se emiten en este formato.");
            }
        }
        return errores;
    }

    public static List<String> atencion(Consulta c, Receta r, boolean requiereMedicamentos) {
        List<String> errores = receta(r, requiereMedicamentos);
        if (c == null) { errores.add("Falta la nota de evolución."); return errores; }
        requerido(errores, "padecimiento actual", c.getPadecimiento());
        requerido(errores, "exploración física", c.getExploracionFisica());
        requerido(errores, "tratamiento, indicaciones y seguimiento", c.getIndicaciones());
        requerido(errores, "nombre completo de quien elabora la nota", c.getNombreMedico());
        return errores;
    }

    /** Las historias heredadas se pueden guardar como borrador sin inventar datos negativos. */
    public static List<String> historia(Expediente e) {
        List<String> errores = new ArrayList<>();
        if (e == null) { errores.add("Falta la historia clínica."); return errores; }
        Paciente p = e.getDatosPaciente();
        if (p == null) errores.add("Falta la identificación del paciente.");
        else {
            requerido(errores, "nombre del paciente", p.getNombre());
            requerido(errores, "apellido del paciente", p.getApellidoPaterno());
            if (p.getFechaNacimiento() == null) errores.add("Falta la fecha de nacimiento para calcular la edad.");
            requerido(errores, "sexo del paciente", p.getSexo());
            requerido(errores, "domicilio del paciente", p.getDomicilio());
        }
        requerido(errores, "tipo de establecimiento", e.getTipoEstablecimiento());
        requerido(errores, "nombre del establecimiento", e.getNombreEstablecimiento());
        requerido(errores, "domicilio del establecimiento", e.getDomicilioEstablecimiento());
        requerido(errores, "antecedentes heredo-familiares", e.getAntecedentesFamiliares());
        requerido(errores, "antecedentes personales patológicos", e.getAntecedentesPersonales());
        requerido(errores, "antecedentes personales no patológicos", e.getAntecedentesNoPatologicos());
        requerido(errores, "uso y dependencia de sustancias", e.getConsumoSustancias());
        requerido(errores, "padecimiento actual y tratamientos previos", e.getPadecimientoActual());
        requerido(errores, "interrogatorio por aparatos y sistemas", e.getInterrogatorioSistemas());
        requerido(errores, "exploración física", e.getExploracionFisica());
        requerido(errores, "signos vitales, peso y talla", e.getSignosHistoria());
        requerido(errores, "resultados de estudios", e.getResultadosEstudios());
        requerido(errores, "diagnósticos", e.getDiagnostico());
        requerido(errores, "pronóstico", e.getPronostico());
        requerido(errores, "indicación terapéutica", e.getIndicacionTerapeutica());
        requerido(errores, "nombre completo del autor de la historia", e.getAutorHistoria());
        requerido(errores, "fecha y hora de la historia", e.getFechaHoraHistoria());
        if (e.getFechaHoraHistoria() != null && !e.getFechaHoraHistoria().trim().isEmpty()) {
            try { java.time.LocalDateTime.parse(e.getFechaHoraHistoria()); }
            catch (java.time.format.DateTimeParseException ex) { errores.add("Fecha y hora de historia inválidas."); }
        }
        return errores;
    }
}
