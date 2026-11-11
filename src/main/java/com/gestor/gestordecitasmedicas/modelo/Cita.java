package com.gestor.gestordecitasmedicas.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

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

    // ---- Lógica de negocio: los controladores llaman estos métodos, que delegan la persistencia a BaseDatos ----

    /** Registra la cita nueva como AGENDADA. */
    public boolean agendar() {
        estado = Estado.AGENDADA;
        return new BaseDatos().insertarCita(this);
    }

    /** Mueve la cita a {@code nuevaFecha} y la deja AGENDADA. */
    public boolean reprogramar(Date nuevaFecha) {
        fechaCita = nuevaFecha;
        estado = Estado.AGENDADA;
        return new BaseDatos().actualizarCita(this);
    }

    /** Marca la cita como CANCELADA. */
    public boolean cancelar() {
        estado = Estado.CANCELADA;
        return new BaseDatos().actualizarCita(this);
    }

    /** Todas las citas, indexadas por id. */
    public static HashMap<Integer, Cita> todas() {
        return new BaseDatos().cargarCitas();
    }

    public static Cita buscar(int idCita) {
        return todas().get(idCita);
    }

    /** Citas cuya fecha está entre {@code inicio} y {@code fin} (ambos incluidos), ordenadas por fecha. */
    public static ArrayList<Cita> entre(LocalDate inicio, LocalDate fin) {
        return new BaseDatos().cargarCitasEntre(inicio, fin);
    }

    /** Citas AGENDADAS que se cruzan con el periodo (las que un bloqueo cancelaría). */
    public static ArrayList<Cita> activasEnRango(LocalDateTime inicio, LocalDateTime fin) {
        return new BaseDatos().citasActivasEnRango(inicio, fin);
    }

    /**
     * Primera cita no cancelada que se cruza con un turno que empieza en {@code fecha} {@code hora};
     * {@code idExcluir} permite ignorar la propia cita al reprogramarla. Devuelve null si el turno está libre.
     */
    public static Cita ocupadaEn(LocalDate fecha, LocalTime hora, int idExcluir) {
        LocalDateTime inicio = LocalDateTime.of(fecha, hora);
        LocalDateTime fin = inicio.plusMinutes(PlanificadorAgenda.DURACION_CITA_MINUTOS);
        for (Cita cita : entre(fecha, fecha)) {
            if (cita.getFechaCita() == null || cita.getIdCita() == idExcluir
                    || cita.getEstado() == Estado.CANCELADA) {
                continue;
            }
            LocalDateTime inicioCita = cita.fechaHora();
            if (PlanificadorAgenda.seSolapan(inicio, fin, inicioCita,
                    inicioCita.plusMinutes(PlanificadorAgenda.DURACION_CITA_MINUTOS))) {
                return cita;
            }
        }
        return null;
    }
}
