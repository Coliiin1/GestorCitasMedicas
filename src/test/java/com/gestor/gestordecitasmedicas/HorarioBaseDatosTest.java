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
    void noBloqueaSiHayCitasActivasEnElRango() {
        Paciente paciente = paciente();
        Cita cita = cita(paciente, LocalDateTime.of(2026, 12, 7, 10, 0));

        HorarioBloqueado bloqueo = new HorarioBloqueado();
        bloqueo.setFechaInicio(LocalDateTime.of(2026, 12, 7, 9, 0));
        bloqueo.setFechaFin(LocalDateTime.of(2026, 12, 7, 13, 0));
        bloqueo.setMotivo("Junta médica");
        assertFalse(bd.guardarBloqueo(bloqueo));
        assertTrue(bd.cargarBloqueos().isEmpty());

        cita.setEstado(Estado.CANCELADA);
        assertTrue(bd.actualizarCita(cita));

        assertTrue(bd.guardarBloqueo(bloqueo));
        assertEquals(1, bd.cargarBloqueos().size());
        assertEquals("Junta médica", bd.cargarBloqueos().get(0).getMotivo());
        assertTrue(bd.eliminarBloqueo(bloqueo.getIdHorarioBloqueado()));
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
