package com.gestor.gestordecitasmedicas.modelo;

import java.time.LocalDateTime;

public class Consulta {
    private int idConsulta;
    private int idExpediente;
    private int idCita;
    private LocalDateTime fechaConsulta;
    private String padecimiento;
    private String motivo;
    private String observaciones;
    private Receta receta;
    private SignosVitales signosVitales;

    public int getIdConsulta() {
        return idConsulta;
    }

    public void setIdConsulta(int idConsulta) {
        this.idConsulta = idConsulta;
    }

    public int getIdExpediente() {
        return idExpediente;
    }

    public void setIdExpediente(int idExpediente) {
        this.idExpediente = idExpediente;
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

    public SignosVitales getSignosVitales() {
        return signosVitales;
    }

    public void setSignosVitales(SignosVitales signosVitales) {
        this.signosVitales = signosVitales;
    }

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

    private String diagnostico;
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String valor) { this.diagnostico = valor; }

    private String pronostico;
    public String getPronostico() { return pronostico; }
    public void setPronostico(String valor) { this.pronostico = valor; }

    private String indicaciones;
    public String getIndicaciones() { return indicaciones; }
    public void setIndicaciones(String valor) { this.indicaciones = valor; }

    private String nombreMedico;
    public String getNombreMedico() { return nombreMedico; }
    public void setNombreMedico(String valor) { this.nombreMedico = valor; }

    private String cedulaMedico;
    public String getCedulaMedico() { return cedulaMedico; }
    public void setCedulaMedico(String valor) { this.cedulaMedico = valor; }

    private String nombrePaciente;
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String valor) { this.nombrePaciente = valor; }

    private String edadPaciente;
    public String getEdadPaciente() { return edadPaciente; }
    public void setEdadPaciente(String valor) { this.edadPaciente = valor; }

    private String sexoPaciente;
    public String getSexoPaciente() { return sexoPaciente; }
    public void setSexoPaciente(String valor) { this.sexoPaciente = valor; }
}
