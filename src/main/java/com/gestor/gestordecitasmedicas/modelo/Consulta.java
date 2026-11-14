package com.gestor.gestordecitasmedicas.modelo;

import java.time.LocalDateTime;

public class Consulta {
    private int idConsulta;
    private int idCita;
    private LocalDateTime fechaConsulta;
    private String padecimiento;
    private String motivo;
    private String observaciones;
    private Receta receta;

    public int getIdConsulta() {
        return idConsulta;
    }

    public void setIdConsulta(int idConsulta) {
        this.idConsulta = idConsulta;
    }

    public LocalDateTime getFechaConsulta() {
        return fechaConsulta;
    }

    public void setFechaConsulta(LocalDateTime fechaConsulta) {
        this.fechaConsulta = fechaConsulta;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Receta getReceta() {
        return receta;
    }

    public void setReceta(Receta receta) {
        this.receta = receta;
    }

    // Diagnóstico, indicaciones, signos y datos de médico/paciente se guardan una sola vez, en la receta de la cita.

    public SignosVitales getSignosVitales() { return receta == null ? null : receta.getSignosVitales(); }
    public String getDiagnostico() { return receta == null ? null : receta.getDiagnostico(); }
    public String getIndicaciones() { return receta == null ? null : receta.getIndicaciones(); }
    public String getNombreMedico() { return receta == null ? null : receta.getNombreMedico(); }
    public String getCedulaMedico() { return receta == null ? null : receta.getCedulaMedico(); }
    public String getNombrePaciente() { return receta == null ? null : receta.getNombrePaciente(); }
    public String getEdadPaciente() { return receta == null ? null : receta.getEdadPaciente(); }
    public String getSexoPaciente() { return receta == null ? null : receta.getSexoPaciente(); }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getPadecimiento() {
        return padecimiento;
    }

    public void setPadecimiento(String padecimiento) {
        this.padecimiento = padecimiento;
    }

    public int getIdCita() {
        return idCita;
    }

    public void setIdCita(int idCita) {
        this.idCita = idCita;
    }

    private String evolucion;
    public String getEvolucion() { return evolucion; }
    public void setEvolucion(String valor) { this.evolucion = valor; }

    private String exploracionFisica;
    public String getExploracionFisica() { return exploracionFisica; }
    public void setExploracionFisica(String valor) { this.exploracionFisica = valor; }

    private String resultadosEstudios;
    public String getResultadosEstudios() { return resultadosEstudios; }
    public void setResultadosEstudios(String valor) { this.resultadosEstudios = valor; }

    private String pronostico;
    public String getPronostico() { return pronostico; }
    public void setPronostico(String valor) { this.pronostico = valor; }

    // ---- Lógica de negocio: delegan la persistencia a BaseDatos ----

    /** Nota de consulta registrada para la cita, o null si aún no tiene. */
    public static Consulta deCita(int idCita) {
        return new BaseDatos().cargarConsultaPorCita(idCita);
    }

    /**
     * Guarda en una sola transacción esta nota, la receta (con medicamentos y signos) y, si
     * {@code finalizar}, marca la cita como ATENDIDA. Si algo falla no se guarda nada.
     */
    public boolean guardarAtencion(Cita cita, Receta receta, boolean finalizar) {
        return new BaseDatos().guardarAtencion(cita, this, receta, finalizar);
    }
}
