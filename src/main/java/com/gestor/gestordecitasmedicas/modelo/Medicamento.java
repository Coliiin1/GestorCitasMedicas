package com.gestor.gestordecitasmedicas.modelo;

public class Medicamento {

    private int idDetalleReceta;
    private int idReceta;
    private String nombreMedicamento;
    private String dosis;
    private String concentracion;
    private String frecuencia;
    private String duradcion;

    public int getIdDetalleReceta() {
        return idDetalleReceta;
    }

    public void setIdDetalleReceta(int idDetalleReceta) {
        this.idDetalleReceta = idDetalleReceta;
    }

    public int getIdReceta() {
        return idReceta;
    }

    public void setIdReceta(int idReceta) {
        this.idReceta = idReceta;
    }

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

    private String presentacion;
    public String getPresentacion() { return presentacion; }
    public void setPresentacion(String valor) { this.presentacion = valor; }

    private String viaAdministracion;
    public String getViaAdministracion() { return viaAdministracion; }
    public void setViaAdministracion(String valor) { this.viaAdministracion = valor; }

    private String denominacionDistintiva;
    public String getDenominacionDistintiva() { return denominacionDistintiva; }
    public void setDenominacionDistintiva(String valor) { this.denominacionDistintiva = valor; }

    private String cantidad;
    public String getCantidad() { return cantidad; }
    public void setCantidad(String valor) { this.cantidad = valor; }
    private String grupoRegulatorio;
    public String getGrupoRegulatorio() { return grupoRegulatorio; }
    public void setGrupoRegulatorio(String valor) { this.grupoRegulatorio = valor; }
}
