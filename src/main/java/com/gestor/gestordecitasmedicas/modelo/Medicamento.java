package com.gestor.gestordecitasmedicas.modelo;

public class Medicamento {

    private String nombreMedicamento;
    private String dosis;

    public String getNombreMedicamento() {
        return nombreMedicamento;
    }

    public void setNombre(String nombreMedicamento) {
        this.nombreMedicamento = nombreMedicamento;
    }

    public String getDosis() {
        return dosis;
    }

    public void setDosis(String dosis) {
        this.dosis = dosis;
    }
}
