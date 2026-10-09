package com.gestor.gestordecitasmedicas.modelo;

public class Asistente extends Personal {

    private int idAsistente;

    public int getIdAsistente() {
        return idAsistente;
    }

    public void setIdAsistente(int idAsistente) {
        this.idAsistente = idAsistente;
    }

    @Override
    public Cita[] listadoSemanal() {
        return new Cita[0];
    }

    @Override
    public boolean cancelarCita() {
        return false;
    }

    @Override
    public boolean agendarCita() {
        return false;
    }

    @Override
    public boolean reprogramarCita() {
        return false;
    }

    @Override
    public boolean validarHorario() {
        return false;
    }
}
