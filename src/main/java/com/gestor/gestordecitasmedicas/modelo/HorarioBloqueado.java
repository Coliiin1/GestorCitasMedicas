package com.gestor.gestordecitasmedicas.modelo;

import java.time.LocalDateTime;

public class HorarioBloqueado {
    private int idHorarioBloqueado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String motivo;

    public int getIdHorarioBloqueado() {
        return idHorarioBloqueado;
    }

    public void setIdHorarioBloqueado(int idHorarioBloqueado) {
        this.idHorarioBloqueado = idHorarioBloqueado;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
