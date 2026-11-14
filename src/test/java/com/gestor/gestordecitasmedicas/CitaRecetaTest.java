package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.Asistente;
import com.gestor.gestordecitasmedicas.modelo.Estado;
import com.gestor.gestordecitasmedicas.modelo.Expediente;
import com.gestor.gestordecitasmedicas.modelo.Medicamento;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import com.gestor.gestordecitasmedicas.modelo.Personal;
import com.gestor.gestordecitasmedicas.modelo.Receta;
import com.gestor.gestordecitasmedicas.modelo.SignosVitales;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitaRecetaTest {
    @TempDir static Path temporal;
    private final BaseDatos bd = new BaseDatos();
    private Paciente paciente;

    @BeforeAll static void configurar() {
        System.setProperty("gestor.db.url", "jdbc:sqlite:" + temporal.resolve("citas-recetas.db"));
    }

    @BeforeEach void preparar() throws Exception {
        Connection con = BaseDatos.getConexion();
        assertTrue(con.getMetaData().getURL().contains(temporal.toString()));
        try (Statement s = con.createStatement()) {
            s.executeUpdate("DELETE FROM Paciente");
        }
        paciente = new Paciente("CITA000101HDFXXX01", java.sql.Date.valueOf("1990-05-10"),
                "Femenino", "Calle Cita 1", "5555555555", "cita@example.com");
        paciente.setNombre("Ana");
        paciente.setApellidoPaterno("Cita");
        paciente.setApellidoMaterno("Prueba");
        assertTrue(bd.insertarPaciente(paciente));
    }

    private Cita cita(LocalDateTime fecha) {
        Cita cita = new Cita();
        cita.setCurp(paciente.getCurp());
        cita.setFechaCita(Date.from(fecha.atZone(ZoneId.systemDefault()).toInstant()));
        cita.setEstado(Estado.AGENDADA);
        assertTrue(bd.insertarCita(cita));
        return cita;
    }

    @Test void citaConservaHoraSeReprogramaYSeCancela() {
        Cita cita = cita(LocalDateTime.of(2026, 11, 10, 10, 0));

        HashMap<Integer, Cita> citas = bd.cargarCitas();
        assertEquals(1, citas.size());
        LocalDateTime guardada = LocalDateTime.ofInstant(
                citas.get(cita.getIdCita()).getFechaCita().toInstant(), ZoneId.systemDefault());
        assertEquals(LocalDateTime.of(2026, 11, 10, 10, 0), guardada);

        cita.setFechaCita(Date.from(LocalDateTime.of(2026, 11, 11, 16, 0)
                .atZone(ZoneId.systemDefault()).toInstant()));
        cita.setEstado(Estado.CANCELADA);
        assertTrue(bd.actualizarCita(cita));

        Cita actualizada = bd.cargarCitas().get(cita.getIdCita());
        LocalDateTime reprogramada = LocalDateTime.ofInstant(
                actualizada.getFechaCita().toInstant(), ZoneId.systemDefault());
        assertEquals(LocalDateTime.of(2026, 11, 11, 16, 0), reprogramada);
        assertEquals(Estado.CANCELADA, actualizada.getEstado());

        assertEquals(1, bd.cargarCitasEntre(LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 15)).size());
        assertTrue(bd.cargarCitasEntre(LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 22)).isEmpty());

        assertTrue(bd.eliminarCita(cita.getIdCita()));
        assertTrue(bd.cargarCitas().isEmpty());
    }

    @Test void recetaGuardaDetalleYSeActualizaSinDuplicar() {
        Cita cita = cita(LocalDateTime.of(2026, 11, 10, 9, 0));
        Receta receta = new Receta();
        receta.setIdCita(cita.getIdCita());
        receta.setFecha(new Date());
        receta.setDiagnostico("Faringoamigdalitis aguda");
        receta.getMedicamentos().add(medicamento("Paracetamol", "500 mg"));
        receta.getMedicamentos().add(medicamento("Loratadina", "10 mg"));
        receta.setSignosVitales(new SignosVitales("120/80", 72, 18, 36.5f, 98, 70.5f, 170));
        assertTrue(bd.insertarReceta(receta));
        assertTrue(receta.getIdReceta() > 0);

        Receta guardada = bd.cargarRecetaPorCita(cita.getIdCita());
        assertNotNull(guardada);
        assertEquals("Faringoamigdalitis aguda", guardada.getDiagnostico());
        assertEquals(2, guardada.getMedicamentos().size());
        assertEquals("120/80", guardada.getSignosVitales().getPresionArterial());
        assertEquals(1, bd.cargarRecetasPorCurp(paciente.getCurp()).size());

        guardada.setDiagnostico("Cuadro viral en remisión");
        guardada.getMedicamentos().clear();
        guardada.getMedicamentos().add(medicamento("Ibuprofeno", "400 mg"));
        guardada.setSignosVitales(new SignosVitales("110/70", 68, 16, 36.2f, 99, 69f, 171));
        assertTrue(bd.actualizarReceta(guardada));

        Receta actualizada = bd.cargarRecetaPorCita(cita.getIdCita());
        assertEquals("Cuadro viral en remisión", actualizada.getDiagnostico());
        assertEquals(1, actualizada.getMedicamentos().size());
        assertEquals("Ibuprofeno", actualizada.getMedicamentos().get(0).getNombreMedicamento());
        assertEquals("110/70", actualizada.getSignosVitales().getPresionArterial());
        assertEquals(1, bd.cargarRecetas().size());
    }

    @Test void recetaSeEliminaYLaCitaQuedaSinReceta() {
        Cita cita = cita(LocalDateTime.of(2026, 11, 12, 11, 0));
        Receta receta = new Receta();
        receta.setIdCita(cita.getIdCita());
        receta.setFecha(new Date());
        receta.setDiagnostico("Control");
        receta.getMedicamentos().add(medicamento("Paracetamol", "500 mg"));
        receta.setSignosVitales(new SignosVitales("120/80", 72, 18, 36.5f, 98, 70.5f, 170));
        assertTrue(bd.insertarReceta(receta));

        assertTrue(bd.eliminarReceta(receta.getIdReceta()));
        assertNull(bd.cargarRecetaPorCita(cita.getIdCita()));
        assertFalse(bd.eliminarReceta(receta.getIdReceta()));
    }

    @Test void elEsquemaRechazaDuplicados() {
        Cita cita = cita(LocalDateTime.of(2026, 11, 13, 9, 0));
        Cita mismoTurno = new Cita();
        mismoTurno.setCurp(paciente.getCurp());
        mismoTurno.setFechaCita(cita.getFechaCita());
        mismoTurno.setEstado(Estado.AGENDADA);
        assertFalse(bd.insertarCita(mismoTurno));
        assertTrue(cita.cancelar());
        assertTrue(bd.insertarCita(mismoTurno), "un turno cancelado se puede volver a ocupar");

        Receta receta = new Receta();
        receta.setIdCita(mismoTurno.getIdCita());
        assertTrue(bd.insertarReceta(receta));
        Receta repetida = new Receta();
        repetida.setIdCita(mismoTurno.getIdCita());
        assertFalse(bd.insertarReceta(repetida));

        Expediente expediente = new Expediente();
        expediente.setDatosPaciente(paciente);
        assertTrue(bd.insertarExpediente(expediente));
        Expediente segundo = new Expediente();
        segundo.setDatosPaciente(paciente);
        assertFalse(bd.insertarExpediente(segundo));
    }

    @Test void idDeAccesoSeGeneraDeLosDatosYNoSeRepite() {
        assertEquals("PERELOJU01", Personal.generarUsuario("Juan", "Pérez", "López", new HashSet<>()));
        assertEquals("NUXXXXAN01", Personal.generarUsuario("Ana", "Ñu", null, new HashSet<>()));

        Asistente primero = new Asistente("Juan", "Pérez", "López", "clave1");
        Asistente homonimo = new Asistente("Juana", "Pereira", "Lozano", "clave2");
        assertTrue(primero.registrar());
        assertTrue(homonimo.registrar());
        try {
            assertEquals("PERELOJU01", primero.getIdAsistente());
            assertEquals("PERELOJU02", homonimo.getIdAsistente());
            assertEquals("asis", Personal.autenticar("PERELOJU02", "clave2"));
            assertNull(Personal.autenticar("PERELOJU02", "clave1"));
            assertEquals("admin", Personal.autenticar("SISTXXAD01", "admin"));
            assertEquals(2, Asistente.todos().size());
        } finally {
            assertTrue(primero.eliminar());
            assertTrue(homonimo.eliminar());
        }
    }

    private static Medicamento medicamento(String nombre, String dosis) {
        Medicamento medicamento = new Medicamento();
        medicamento.setNombreMedicamento(nombre);
        medicamento.setDosis(dosis);
        return medicamento;
    }

    @AfterAll static void cerrar() {
        BaseDatos.cerrarConexion();
        System.clearProperty("gestor.db.url");
    }
}
