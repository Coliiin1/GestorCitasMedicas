package com.gestor.gestordecitasmedicas.modelo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class Cita {

    private int idCita;
    private Date fechaCita;
    private Estado estado;
    private String curp;

    public int getIdCita() {
        return idCita;
    }

    public void setIdCita(int idCita) {
        this.idCita = idCita;
    }

    public Date getFechaCita() {
        return fechaCita;
    }

    public void setFechaCita(Date fechaCita) {
        this.fechaCita = fechaCita;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getCurp() {
        return curp;
    }

    public void setCurp(String curp) {
        this.curp = curp;
    }

    /** Fecha y hora de la cita en la zona local, o null si no tiene fecha. */
    public LocalDateTime fechaHora() {
        return fechaCita == null ? null : LocalDateTime.ofInstant(fechaCita.toInstant(), ZoneId.systemDefault());
    }
}
