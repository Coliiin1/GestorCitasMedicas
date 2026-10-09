package com.gestor.gestordecitasmedicas.modelo;

public abstract class Personal extends Usuario {

    public abstract Cita[] listadoSemanal();

    public abstract boolean cancelarCita();

    public abstract boolean agendarCita();

    public abstract boolean reprogramarCita();

    public abstract boolean validarHorario();
}
