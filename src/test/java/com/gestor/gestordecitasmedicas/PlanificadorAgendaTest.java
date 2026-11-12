package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.modelo.DiaSemana;
import com.gestor.gestordecitasmedicas.modelo.Horario;
import com.gestor.gestordecitasmedicas.modelo.HorarioBloqueado;
import com.gestor.gestordecitasmedicas.modelo.PlanificadorAgenda;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanificadorAgendaTest {

    private static Horario tramo(DiaSemana dia, String inicio, String fin, boolean activo) {
        Horario horario = new Horario();
        horario.setDia(dia);
        horario.setHoraInicio(LocalTime.parse(inicio));
        horario.setHoraFin(LocalTime.parse(fin));
        horario.setEstado(activo);
        return horario;
    }

    private static HorarioBloqueado bloqueo(String inicio, String fin) {
        HorarioBloqueado bloqueo = new HorarioBloqueado();
        bloqueo.setFechaInicio(LocalDateTime.parse(inicio));
        bloqueo.setFechaFin(LocalDateTime.parse(fin));
        return bloqueo;
    }

    @Test
    void generaSlotsDeUnaHoraEnVariosTramos() {
        LocalDate lunes = LocalDate.of(2026, 11, 9);
        List<Horario> horarios = Arrays.asList(
                tramo(DiaSemana.LUNES, "09:00", "13:00", true),
                tramo(DiaSemana.LUNES, "16:00", "18:00", true));

        assertEquals(Arrays.asList(
                        LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0),
                        LocalTime.of(16, 0), LocalTime.of(17, 0)),
                PlanificadorAgenda.iniciosDisponibles(lunes, horarios, new ArrayList<HorarioBloqueado>()));
        assertEquals(60, PlanificadorAgenda.DURACION_CITA_MINUTOS);
    }

    @Test
    void ignoraTramosInactivosYCitasFueraDelHorario() {
        LocalDate lunes = LocalDate.of(2026, 11, 9);
        List<Horario> horarios = Arrays.asList(
                tramo(DiaSemana.LUNES, "09:00", "10:00", true),
                tramo(DiaSemana.LUNES, "10:00", "12:00", false));

        assertEquals(Arrays.asList(LocalTime.of(9, 0)),
                PlanificadorAgenda.iniciosDisponibles(lunes, horarios, new ArrayList<HorarioBloqueado>()));
        assertTrue(PlanificadorAgenda.esDiaHabil(lunes, horarios));
        assertFalse(PlanificadorAgenda.esDiaHabil(lunes.plusDays(5), horarios));
    }

    @Test
    void excluyeSoloLosSlotsQueSeSolapanConUnBloqueo() {
        LocalDate lunes = LocalDate.of(2026, 11, 9);
        List<Horario> horarios = Arrays.asList(tramo(DiaSemana.LUNES, "09:00", "13:00", true));
        List<HorarioBloqueado> bloqueos = Arrays.asList(
                bloqueo("2026-11-09T10:30", "2026-11-09T10:45"));

        assertEquals(Arrays.asList(
                        LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(12, 0)),
                PlanificadorAgenda.iniciosDisponibles(lunes, horarios, bloqueos));
        assertTrue(PlanificadorAgenda.tieneBloqueo(lunes, bloqueos));
        assertFalse(PlanificadorAgenda.esInicioDisponible(lunes, LocalTime.of(10, 0), horarios, bloqueos));
    }

    @Test
    void unBloqueoParcialNoMarcaElDiaComoBloqueado() {
        LocalDate lunes = LocalDate.of(2026, 11, 9);
        List<Horario> horarios = Arrays.asList(tramo(DiaSemana.LUNES, "09:00", "13:00", true));

        assertFalse(PlanificadorAgenda.diaBloqueado(lunes, horarios,
                Arrays.asList(bloqueo("2026-11-09T10:00", "2026-11-09T11:00"))));
        assertTrue(PlanificadorAgenda.diaBloqueado(lunes, horarios,
                Arrays.asList(bloqueo("2026-11-09T00:00", "2026-11-10T00:00"))));
        // Martes no es hábil: aunque tenga bloqueo, no se pinta como bloqueado.
        assertFalse(PlanificadorAgenda.diaBloqueado(lunes.plusDays(1), horarios,
                Arrays.asList(bloqueo("2026-11-10T00:00", "2026-11-11T00:00"))));
    }

    @Test
    void elBloqueoConHorasRepiteLaFranjaEnCadaDia() {
        LocalDate lunes = LocalDate.of(2026, 11, 9);

        List<LocalDateTime[]> todoElDia = PlanificadorAgenda.intervalosDeBloqueo(
                lunes, lunes.plusDays(4), true, null, null);
        assertEquals(1, todoElDia.size());
        assertEquals(LocalDateTime.parse("2026-11-09T00:00"), todoElDia.get(0)[0]);
        assertEquals(LocalDateTime.parse("2026-11-14T00:00"), todoElDia.get(0)[1]);

        List<LocalDateTime[]> franja = PlanificadorAgenda.intervalosDeBloqueo(
                lunes, lunes.plusDays(4), false, LocalTime.of(13, 0), LocalTime.of(15, 0));
        assertEquals(5, franja.size());
        assertEquals(LocalDateTime.parse("2026-11-11T13:00"), franja.get(2)[0]);
        assertEquals(LocalDateTime.parse("2026-11-11T15:00"), franja.get(2)[1]);
    }

    @Test
    void reglaDeSolapamientoUsaIntervalosSemiabiertos() {
        LocalDateTime inicio = LocalDateTime.parse("2026-11-09T10:00");
        LocalDateTime fin = LocalDateTime.parse("2026-11-09T11:00");

        assertFalse(PlanificadorAgenda.seSolapan(inicio, fin, fin, fin.plusHours(1)));
        assertFalse(PlanificadorAgenda.seSolapan(inicio, fin, inicio.minusHours(1), inicio));
        assertTrue(PlanificadorAgenda.seSolapan(inicio, fin, inicio.plusMinutes(30), fin.plusMinutes(30)));
    }

    @Test
    void daFormatoLegibleARangoYHora() {
        assertEquals("09:00", PlanificadorAgenda.formatoHora(LocalTime.of(9, 0)));
        assertEquals("2026-11-09 · 09:00 a 13:00", PlanificadorAgenda.formatoRango(
                LocalDateTime.parse("2026-11-09T09:00"), LocalDateTime.parse("2026-11-09T13:00")));
        assertEquals("2026-11-09 · todo el día", PlanificadorAgenda.formatoRango(
                LocalDateTime.parse("2026-11-09T00:00"), LocalDateTime.parse("2026-11-10T00:00")));
        assertEquals("2026-11-09 a 2026-11-11 · todo el día", PlanificadorAgenda.formatoRango(
                LocalDateTime.parse("2026-11-09T00:00"), LocalDateTime.parse("2026-11-12T00:00")));
        assertEquals("--", PlanificadorAgenda.formatoHora(null));
    }
}
