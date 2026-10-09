package com.gestor.gestordecitasmedicas.modelo;

import java.util.HashMap;

public class Expediente {

    private Paciente datosPaciente;
    private HashMap<Integer, Receta> recetas = new HashMap<>();

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
