package com.gestor.gestordecitasmedicas.modelo;

import java.util.Date;
import java.util.HashMap;

public class Expediente {

    private int idExpediente;
    private Paciente datosPaciente;
    private Date fechaApertura;
    private String antecedentesFamiliares;
    private String antecedentesPersonales;
    private String alergias;
    private String observaciones;
    private HashMap<Integer, Receta> recetas = new HashMap<>();

    public int getIdExpediente() {
        return idExpediente;
    }

    public void setIdExpediente(int idExpediente) {
        this.idExpediente = idExpediente;
    }

    public Date getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(Date fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public String getAntecedentesFamiliares() {
        return antecedentesFamiliares;
    }

    public void setAntecedentesFamiliares(String antecedentesFamiliares) {
        this.antecedentesFamiliares = antecedentesFamiliares;
    }

    public String getAntecedentesPersonales() {
        return antecedentesPersonales;
    }

    public void setAntecedentesPersonales(String antecedentesPersonales) {
        this.antecedentesPersonales = antecedentesPersonales;
    }

    public String getAlergias() {
        return alergias;
    }

    public void setAlergias(String alergias) {
        this.alergias = alergias;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Paciente getDatosPaciente() {
        return datosPaciente;
    }

    public void setDatosPaciente(Paciente datosPaciente) {
        this.datosPaciente = datosPaciente;
    }

    public HashMap<Integer, Receta> getRecetas() {
        return recetas;
    }

    public void setRecetas(HashMap<Integer, Receta> recetas) {
        this.recetas = recetas;
    }
}
