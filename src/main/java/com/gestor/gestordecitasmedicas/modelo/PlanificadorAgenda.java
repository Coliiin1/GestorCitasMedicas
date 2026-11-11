package com.gestor.gestordecitasmedicas.modelo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Reglas únicas de disponibilidad compartidas por la agenda y la configuración
 * de horarios. Las citas ocupan intervalos de una hora dentro de los tramos
 * activos y fuera de los bloqueos configurados.
 */
public final class PlanificadorAgenda {
    public static final String[] MESES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
            "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
    public static final String[] MESES_CORTO = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep",
            "Oct", "Nov", "Dic"};
    public static final int DURACION_CITA_MINUTOS = 60;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private PlanificadorAgenda() {
    }

    public static List<Horario> horariosDelDia(LocalDate fecha, List<Horario> horarios) {
        List<Horario> resultado = new ArrayList<>();
        if (fecha == null || horarios == null) {
            return resultado;
        }
        DiaSemana dia = DiaSemana.desdeDayOfWeek(fecha.getDayOfWeek());
        for (Horario horario : horarios) {
            if (horario != null && horario.isEstado() && horario.getDia() == dia
                    && horario.getHoraInicio() != null && horario.getHoraFin() != null
                    && horario.getHoraInicio().isBefore(horario.getHoraFin())) {
                resultado.add(horario);
            }
        }
        return resultado;
    }

    public static List<LocalTime> iniciosDisponibles(LocalDate fecha, List<Horario> horarios,
                                                     List<HorarioBloqueado> bloqueos) {
        TreeSet<LocalTime> inicios = new TreeSet<>();
        if (fecha == null) {
            return new ArrayList<>(inicios);
        }
        for (Horario horario : horariosDelDia(fecha, horarios)) {
            LocalTime hora = horario.getHoraInicio();
            while (!hora.plusMinutes(DURACION_CITA_MINUTOS).isAfter(horario.getHoraFin())) {
                LocalDateTime inicio = LocalDateTime.of(fecha, hora);
                LocalDateTime fin = inicio.plusMinutes(DURACION_CITA_MINUTOS);
                if (!estaBloqueado(inicio, fin, bloqueos)) {
                    inicios.add(hora);
                }
                hora = hora.plusMinutes(DURACION_CITA_MINUTOS);
            }
        }
        return new ArrayList<>(inicios);
    }

    public static List<LocalTime> horasDeTrabajo(List<Horario> horarios) {
        TreeSet<LocalTime> horas = new TreeSet<>();
        if (horarios == null) {
            return new ArrayList<>(horas);
        }
        for (Horario horario : horarios) {
            if (horario == null || !horario.isEstado()
                    || horario.getHoraInicio() == null || horario.getHoraFin() == null) {
                continue;
            }
            LocalTime hora = horario.getHoraInicio();
            while (!hora.plusMinutes(DURACION_CITA_MINUTOS).isAfter(horario.getHoraFin())) {
                horas.add(hora);
                hora = hora.plusMinutes(DURACION_CITA_MINUTOS);
            }
        }
        return new ArrayList<>(horas);
    }

    public static boolean esInicioDisponible(LocalDate fecha, LocalTime hora, List<Horario> horarios,
                                             List<HorarioBloqueado> bloqueos) {
        return hora != null && iniciosDisponibles(fecha, horarios, bloqueos).contains(hora);
    }

    public static boolean estaBloqueado(LocalDateTime inicio, LocalDateTime fin,
                                        List<HorarioBloqueado> bloqueos) {
        if (inicio == null || fin == null || bloqueos == null) {
            return false;
        }
        for (HorarioBloqueado bloqueo : bloqueos) {
            if (bloqueo != null && seSolapan(inicio, fin, bloqueo.getFechaInicio(), bloqueo.getFechaFin())) {
                return true;
            }
        }
        return false;
    }

    public static boolean tieneBloqueo(LocalDate fecha, List<HorarioBloqueado> bloqueos) {
        if (fecha == null) {
            return false;
        }
        return estaBloqueado(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay(), bloqueos);
    }

    public static boolean seSolapan(LocalDateTime inicioA, LocalDateTime finA,
                                    LocalDateTime inicioB, LocalDateTime finB) {
        return inicioA != null && finA != null && inicioB != null && finB != null
                && inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }

    public static boolean tramoSeSolapa(Horario a, Horario b) {
        if (a == null || b == null || a.getDia() == null || a.getDia() != b.getDia()
                || a.getHoraInicio() == null || a.getHoraFin() == null
                || b.getHoraInicio() == null || b.getHoraFin() == null) {
            return false;
        }
        return a.getHoraInicio().isBefore(b.getHoraFin()) && a.getHoraFin().isAfter(b.getHoraInicio());
    }

    public static boolean esDiaHabil(LocalDate fecha, List<Horario> horarios) {
        return !horariosDelDia(fecha, horarios).isEmpty();
    }

    public static List<LocalTime> horasDelDia() {
        List<LocalTime> horas = new ArrayList<>();
        for (int hora = 0; hora < 24; hora++) {
            horas.add(LocalTime.of(hora, 0));
        }
        return horas;
    }

    public static String formatoHora(LocalTime hora) {
        return hora == null ? "--" : FORMATO_HORA.format(hora);
    }

    public static String formatoRango(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null && fin == null) {
            return "--";
        }
        if (inicio != null && fin != null) {
            boolean diaCompleto = inicio.toLocalTime().equals(LocalTime.MIDNIGHT)
                    && fin.toLocalTime().equals(LocalTime.MIDNIGHT)
                    && fin.toLocalDate().isAfter(inicio.toLocalDate());
            if (diaCompleto) {
                LocalDate ultimo = fin.toLocalDate().minusDays(1);
                if (ultimo.equals(inicio.toLocalDate())) {
                    return inicio.toLocalDate() + " · todo el día";
                }
                return inicio.toLocalDate() + " a " + ultimo + " · todo el día";
            }
            if (inicio.toLocalDate().equals(fin.toLocalDate())) {
                return inicio.toLocalDate() + " · " + formatoHora(inicio.toLocalTime())
                        + " a " + formatoHora(fin.toLocalTime());
            }
        }
        return (inicio == null ? "--" : inicio.toString().replace('T', ' '))
                + " a " + (fin == null ? "--" : fin.toString().replace('T', ' '));
    }

    public static LocalDate lunesDe(LocalDate fecha) {
        return fecha == null ? null : fecha.minusDays(fecha.getDayOfWeek().getValue()
                - DayOfWeek.MONDAY.getValue());
    }
}
