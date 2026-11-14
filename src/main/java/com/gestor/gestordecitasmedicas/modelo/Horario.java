package com.gestor.gestordecitasmedicas.modelo;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

public class Horario {
    private int idHorario;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private DiaSemana dia;
    private boolean estado;

    public int getIdHorario() {
        return idHorario;
    }

    public void setIdHorario(int idHorario) {
        this.idHorario = idHorario;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public DiaSemana getDia() {
        return dia;
    }

    public void setDia(DiaSemana dia) {
        this.dia = dia;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    // ---- Lógica de negocio: delegan la persistencia a BaseDatos ----

    /** Inserta o actualiza el tramo; falla si se superpone con otro tramo del mismo día. */
    public boolean guardar() {
        return new BaseDatos().guardarHorario(this);
    }

    /** Elimina el tramo y cancela sus citas activas en la misma transacción. */
    public boolean eliminar() {
        return new BaseDatos().eliminarHorario(getIdHorario());
    }

    /** Citas AGENDADAS de hoy en adelante que caen dentro de este tramo. */
    public ArrayList<Cita> citasActivas() {
        return new BaseDatos().citasActivasEnHorario(this);
    }

    /** True si una cita que empieza en {@code fecha} cabe completa en este tramo. */
    public boolean cubre(LocalDateTime fecha) {
        if (fecha == null || dia == null || horaInicio == null || horaFin == null
                || dia != DiaSemana.desdeDayOfWeek(fecha.getDayOfWeek())) {
            return false;
        }
        return !fecha.toLocalTime().isBefore(horaInicio) && !fecha.plusMinutes(PlanificadorAgenda.DURACION_CITA_MINUTOS)
                .isAfter(fecha.toLocalDate().atTime(horaFin));
    }

    public static ArrayList<Horario> todos() {
        return new BaseDatos().cargarHorarios();
    }

    /** Solo los tramos activos (los que usa la agenda para ofrecer horarios). */
    public static ArrayList<Horario> activos() {
        return new BaseDatos().cargarHorariosActivos();
    }
}
