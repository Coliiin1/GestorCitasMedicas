package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.DiaSemana;
import com.gestor.gestordecitasmedicas.modelo.Estado;
import com.gestor.gestordecitasmedicas.modelo.Horario;
import com.gestor.gestordecitasmedicas.modelo.HorarioBloqueado;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorarioBaseDatosTest {
    @TempDir static Path temporal;
    private final BaseDatos bd = new BaseDatos();

    @BeforeAll static void configurar() {
        System.setProperty("gestor.db.url", "jdbc:sqlite:" + temporal.resolve("horarios.db"));
    }

    @BeforeEach void limpiar() throws Exception {
        Connection con = BaseDatos.getConexion();
        try (Statement s = con.createStatement()) {
            s.executeUpdate("DELETE FROM Cita");
            s.executeUpdate("DELETE FROM Horario_bloqueado");
            s.executeUpdate("DELETE FROM Horario");
            s.executeUpdate("DELETE FROM Paciente");
        }
    }

    private static Horario tramo(DiaSemana dia, String inicio, String fin) {
        Horario horario = new Horario();
        horario.setDia(dia);
        horario.setHoraInicio(LocalTime.parse(inicio));
        horario.setHoraFin(LocalTime.parse(fin));
        horario.setEstado(true);
        return horario;
    }

    private Paciente paciente() {
        Paciente paciente = new Paciente("HORA000101HDFXXX01", java.sql.Date.valueOf("1990-05-10"),
                "Femenino", "Calle Horario 1", "5555555555", "horario@example.com");
        paciente.setNombre("Laura");
        paciente.setApellidoPaterno("Horario");
        paciente.setApellidoMaterno("Prueba");
        assertTrue(bd.insertarPaciente(paciente));
        return paciente;
    }

    private Cita cita(Paciente paciente, LocalDateTime fecha) {
        Cita cita = new Cita();
        cita.setCurp(paciente.getCurp());
        cita.setFechaCita(Date.from(fecha.atZone(ZoneId.systemDefault()).toInstant()));
        cita.setEstado(Estado.AGENDADA);
        assertTrue(bd.insertarCita(cita));
        return cita;
    }

    @Test
    void permiteTramosSeparadosYRechazaCruces() {
        Horario manana = tramo(DiaSemana.LUNES, "09:00", "13:00");
        assertTrue(bd.guardarHorario(manana));
        assertTrue(manana.getIdHorario() > 0);

        assertFalse(bd.guardarHorario(tramo(DiaSemana.LUNES, "12:00", "14:00")));
        assertTrue(bd.guardarHorario(tramo(DiaSemana.LUNES, "13:00", "15:00")));
        assertTrue(bd.guardarHorario(tramo(DiaSemana.LUNES, "16:00", "18:00")));

        assertEquals(3, bd.cargarHorariosActivos().size());
    }

    @Test
    void elBloqueoCancelaLasCitasAgendadasFuturasYNoTocaLasPasadas() {
        Paciente paciente = paciente();
        LocalDate ayer = LocalDate.now().minusDays(1);
        LocalDate manana = LocalDate.now().plusDays(1);
        Cita pasada = cita(paciente, ayer.atTime(10, 0));
        Cita futura = cita(paciente, manana.atTime(10, 0));
        Cita fueraDelRango = cita(paciente, manana.atTime(15, 0));

        // Abarca desde ayer: la cita vencida no debe cancelarse ni contarse.
        LocalDateTime inicio = ayer.atStartOfDay();
        LocalDateTime fin = manana.atTime(13, 0);
        assertEquals(1, bd.citasActivasEnRango(inicio, fin).size());
        assertTrue(bd.guardarBloqueo(bloqueo(inicio, fin)));

        assertEquals(Estado.AGENDADA, bd.cargarCitas().get(pasada.getIdCita()).getEstado());
        assertEquals(Estado.CANCELADA, bd.cargarCitas().get(futura.getIdCita()).getEstado());
        assertEquals(Estado.AGENDADA, bd.cargarCitas().get(fueraDelRango.getIdCita()).getEstado());
        assertEquals(1, bd.cargarBloqueos().size());
    }

    @Test
    void guardarBloqueosEsTodoONada() {
        LocalDate dia = LocalDate.now().plusDays(3);
        assertTrue(bd.guardarBloqueo(bloqueo(dia.plusDays(1).atTime(13, 0), dia.plusDays(1).atTime(15, 0))));

        // El segundo se cruza con el existente: tampoco debe guardarse el primero.
        assertFalse(bd.guardarBloqueos(java.util.Arrays.asList(
                bloqueo(dia.atTime(13, 0), dia.atTime(15, 0)),
                bloqueo(dia.plusDays(1).atTime(14, 0), dia.plusDays(1).atTime(16, 0)))));
        assertEquals(1, bd.cargarBloqueos().size());

        assertTrue(bd.guardarBloqueos(java.util.Arrays.asList(
                bloqueo(dia.atTime(13, 0), dia.atTime(15, 0)),
                bloqueo(dia.plusDays(2).atTime(13, 0), dia.plusDays(2).atTime(15, 0)))));
        assertEquals(3, bd.cargarBloqueos().size());
    }

    private static HorarioBloqueado bloqueo(LocalDateTime inicio, LocalDateTime fin) {
        HorarioBloqueado bloqueo = new HorarioBloqueado();
        bloqueo.setFechaInicio(inicio);
        bloqueo.setFechaFin(fin);
        return bloqueo;
    }

    @Test
    void noEliminaUnTramoConCitasFuturasActivas() {
        Horario horario = tramo(DiaSemana.LUNES, "09:00", "13:00");
        assertTrue(bd.guardarHorario(horario));

        LocalDate proximoLunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Paciente paciente = paciente();
        Cita cita = cita(paciente, proximoLunes.atTime(10, 0));

        assertEquals(1, bd.citasActivasEnHorario(horario).size());
        assertFalse(bd.eliminarHorario(horario.getIdHorario()));

        cita.setEstado(Estado.CANCELADA);
        assertTrue(bd.actualizarCita(cita));
        assertTrue(bd.citasActivasEnHorario(horario).isEmpty());
        assertTrue(bd.eliminarHorario(horario.getIdHorario()));
    }

    @AfterAll static void cerrar() {
        BaseDatos.cerrarConexion();
        System.clearProperty("gestor.db.url");
    }
}
