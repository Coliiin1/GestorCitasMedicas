package com.gestor.gestordecitasmedicas.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class HorarioBloqueado {
    private int idHorarioBloqueado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

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

    // ---- Lógica de negocio: delegan la persistencia a BaseDatos ----

    /** Guarda el bloqueo y, en la misma transacción, cancela las citas agendadas del periodo. */
    public boolean guardar() {
        return new BaseDatos().guardarBloqueo(this);
    }

    public boolean eliminar() {
        return new BaseDatos().eliminarBloqueo(getIdHorarioBloqueado());
    }

    public static ArrayList<HorarioBloqueado> todos() {
        return new BaseDatos().cargarBloqueos();
    }
}
