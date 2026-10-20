package com.gestor.gestordecitasmedicas.modelo;

public class Medicamento {

    private String nombreMedicamento;
    private String dosis;
    private String concentracion;
    private String frecuencia;
    private String duradcion;

    public String getNombreMedicamento() {
        return nombreMedicamento;
    }

    public void setNombreMedicamento(String nombreMedicamento) {
        this.nombreMedicamento = nombreMedicamento;
    }

    public String getDosis() {
        return dosis;
    }

    public void setDosis(String dosis) {
        this.dosis = dosis;
    }

    public String getConcentracion() {
        return concentracion;
    }

    public void setConcentracion(String concentracion) {
        this.concentracion = concentracion;
    }

    public String getFrecuencia() {
        return frecuencia;
    }

    public void setFrecuencia(String frecuencia) {
        this.frecuencia = frecuencia;
    }

    public String getDuradcion() {
        return duradcion;
    }

    public void setDuradcion(String duradcion) {
        this.duradcion = duradcion;
    }
}
