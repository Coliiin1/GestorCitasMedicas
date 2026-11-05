package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.Consulta;
import com.gestor.gestordecitasmedicas.modelo.Estado;
import com.gestor.gestordecitasmedicas.modelo.Expediente;
import com.gestor.gestordecitasmedicas.modelo.Medicamento;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaTest {
    @TempDir static Path temporal;
    private final BaseDatos bd = new BaseDatos();
    private Paciente paciente;
    private Expediente expediente;

    @BeforeAll static void configurar() {
        System.setProperty("gestor.db.url", "jdbc:sqlite:" + temporal.resolve("consultas.db"));
    }

    @BeforeEach void preparar() throws Exception {
        Connection con = BaseDatos.getConexion();
        assertTrue(con.getMetaData().getURL().contains(temporal.toString()));
        try (Statement s = con.createStatement()) {
            s.executeUpdate("DELETE FROM Paciente");
        }

        paciente = new Paciente("CONS000101HDFXXX01", java.sql.Date.valueOf("1990-05-10"),
                "Femenino", "Calle Consulta 1", "5555555555", "consulta@example.com");
        paciente.setNombre("Ana");
        paciente.setApellidoPaterno("Consulta");
        paciente.setApellidoMaterno("Prueba");
        assertTrue(bd.insertarPaciente(paciente));

        expediente = new Expediente();
        expediente.setDatosPaciente(paciente);
        expediente.setFechaApertura(new Date());
        assertTrue(bd.insertarExpediente(expediente));
    }

    private Cita cita(LocalDateTime fecha) {
        Cita cita = new Cita();
        cita.setCurp(paciente.getCurp());
        cita.setFechaCita(Date.from(fecha.atZone(ZoneId.systemDefault()).toInstant()));
        cita.setEstado(Estado.AGENDADA);
        assertTrue(bd.insertarCita(cita));
        return cita;
    }

    private static Receta receta(String diagnostico, String... medicamentos) {
        Receta receta = new Receta();
        receta.setFecha(new Date());
        receta.setDiagnostico(diagnostico);
        for (String nombre : medicamentos) {
            Medicamento medicamento = new Medicamento();
            medicamento.setNombreMedicamento(nombre);
            medicamento.setDosis("500 mg");
            receta.getMedicamentos().add(medicamento);
        }
        receta.setSignosVitales(new SignosVitales("120/80", 72, 18, 36.5f, 98, 70.5f, 170));
        return receta;
    }

    @Test void consultaCrudYLecturaPorCitaYExpediente() {
        Cita cita = cita(LocalDateTime.of(2026, 11, 15, 10, 0));

        Consulta consulta = new Consulta();
        consulta.setIdCita(cita.getIdCita());
        consulta.setIdExpediente(expediente.getIdExpediente());
        consulta.setFechaConsulta(LocalDateTime.of(2026, 11, 15, 10, 30));
        consulta.setPadecimiento("Faringitis");
        consulta.setMotivo("Dolor de garganta");
        consulta.setObservaciones("Sin fiebre");
        assertTrue(bd.insertarConsulta(consulta));
        assertTrue(consulta.getIdConsulta() > 0);

        Consulta porId = bd.cargarConsultaPorId(consulta.getIdConsulta());
        assertNotNull(porId);
        assertEquals(cita.getIdCita(), porId.getIdCita());
        assertEquals(expediente.getIdExpediente(), porId.getIdExpediente());
        assertEquals(LocalDateTime.of(2026, 11, 15, 10, 30), porId.getFechaConsulta());
        assertEquals("Faringitis", porId.getPadecimiento());
        assertEquals("Dolor de garganta", porId.getMotivo());
        assertEquals("Sin fiebre", porId.getObservaciones());

        Consulta porCita = bd.cargarConsultaPorCita(cita.getIdCita());
        assertNotNull(porCita);
        assertEquals(consulta.getIdConsulta(), porCita.getIdConsulta());
        assertEquals(1, bd.cargarConsultasPorExpediente(expediente.getIdExpediente()).size());

        consulta.setPadecimiento("Amigdalitis");
        consulta.setFechaConsulta(LocalDateTime.of(2026, 11, 15, 11, 0));
        assertTrue(bd.actualizarConsulta(consulta));
        assertEquals("Amigdalitis", bd.cargarConsultaPorId(consulta.getIdConsulta()).getPadecimiento());
        assertEquals(LocalDateTime.of(2026, 11, 15, 11, 0),
                bd.cargarConsultaPorId(consulta.getIdConsulta()).getFechaConsulta());

        assertTrue(bd.eliminarConsulta(consulta.getIdConsulta()));
        assertNull(bd.cargarConsultaPorId(consulta.getIdConsulta()));
        assertNull(bd.cargarConsultaPorCita(cita.getIdCita()));
        assertTrue(bd.cargarConsultasPorExpediente(expediente.getIdExpediente()).isEmpty());
    }

    @Test void finalizarConsultaGuardaTodoYReutilizaRegistros() {
        Cita cita = cita(LocalDateTime.of(2026, 11, 16, 9, 0));
        Receta receta = receta("Faringoamigdalitis aguda", "Paracetamol", "Loratadina");

        Consulta consulta = new Consulta();
        consulta.setPadecimiento("Faringoamigdalitis aguda");
        consulta.setMotivo("Dolor de garganta");
        consulta.setObservaciones("Sin datos de alarma");

        assertTrue(bd.finalizarConsulta(cita, consulta, receta));
        assertEquals(Estado.ATENDIDA, cita.getEstado());
        assertTrue(receta.getIdReceta() > 0);
        assertTrue(consulta.getIdConsulta() > 0);
        assertEquals(expediente.getIdExpediente(), consulta.getIdExpediente());
        assertNotNull(consulta.getFechaConsulta());
        assertEquals(receta.getIdReceta(), consulta.getReceta().getIdReceta());

        Receta recetaGuardada = bd.cargarRecetaPorCita(cita.getIdCita());
        assertNotNull(recetaGuardada);
        assertEquals(2, recetaGuardada.getMedicamentos().size());
        assertEquals("120/80", recetaGuardada.getSignosVitales().getPresionArterial());

        Consulta consultaGuardada = bd.cargarConsultaPorCita(cita.getIdCita());
        assertNotNull(consultaGuardada);
        assertEquals(consulta.getIdConsulta(), consultaGuardada.getIdConsulta());
        assertEquals("Faringoamigdalitis aguda", consultaGuardada.getPadecimiento());
        assertEquals("Dolor de garganta", consultaGuardada.getMotivo());
        assertEquals("Sin datos de alarma", consultaGuardada.getObservaciones());

        Expediente cargado = bd.cargarExpedientePorCurp(paciente.getCurp());
        assertNotNull(cargado);
        assertEquals(1, cargado.getConsultas().size());
        assertEquals(1, cargado.getRecetas().size());

        int idConsulta = consulta.getIdConsulta();
        int idReceta = receta.getIdReceta();
        receta.setDiagnostico("Cuadro viral en remisión");
        receta.getMedicamentos().clear();
        Medicamento ibuprofeno = new Medicamento();
        ibuprofeno.setNombreMedicamento("Ibuprofeno");
        ibuprofeno.setDosis("400 mg");
        receta.getMedicamentos().add(ibuprofeno);

        Consulta segunda = new Consulta();
        segunda.setPadecimiento("Cuadro viral en remisión");
        segunda.setMotivo("Control");
        segunda.setObservaciones("Mejoría clínica");

        assertTrue(bd.finalizarConsulta(cita, segunda, receta));
        assertEquals(idReceta, receta.getIdReceta());
        assertEquals(idConsulta, segunda.getIdConsulta());
        assertEquals(1, bd.cargarRecetas().size());
        assertEquals(1, bd.cargarConsultasPorExpediente(expediente.getIdExpediente()).size());
        assertEquals("Cuadro viral en remisión", bd.cargarRecetaPorCita(cita.getIdCita()).getDiagnostico());
        assertEquals(1, bd.cargarRecetaPorCita(cita.getIdCita()).getMedicamentos().size());
        assertEquals("Cuadro viral en remisión", bd.cargarConsultaPorCita(cita.getIdCita()).getPadecimiento());
        assertEquals(Estado.ATENDIDA, cita.getEstado());
    }

    @Test void finalizarConsultaCreaExpedienteSiFalta() {
        assertTrue(bd.eliminarExpediente(String.valueOf(expediente.getIdExpediente())));
        assertNull(bd.cargarExpedientePorCurp(paciente.getCurp()));

        Cita cita = cita(LocalDateTime.of(2026, 11, 17, 12, 0));
        Receta receta = receta("Control");
        Consulta consulta = new Consulta();
        consulta.setPadecimiento("Control");
        consulta.setMotivo("Revisión");
        consulta.setObservaciones("Sin hallazgos");

        assertTrue(bd.finalizarConsulta(cita, consulta, receta));
        Expediente creado = bd.cargarExpedientePorCurp(paciente.getCurp());
        assertNotNull(creado);
        assertTrue(creado.getIdExpediente() > 0);
        assertEquals(creado.getIdExpediente(), consulta.getIdExpediente());
        assertEquals(1, creado.getConsultas().size());
    }

    @Test void finalizarConsultaHaceRollbackSiFallaLaReceta() {
        Cita inexistente = new Cita();
        inexistente.setIdCita(999999);
        inexistente.setCurp(paciente.getCurp());
        inexistente.setEstado(Estado.AGENDADA);

        Receta receta = receta("No debe guardarse");
        Consulta consulta = new Consulta();
        consulta.setPadecimiento("No debe guardarse");

        assertFalse(bd.finalizarConsulta(inexistente, consulta, receta));
        assertEquals(0, receta.getIdReceta());
        assertEquals(0, consulta.getIdConsulta());
        assertTrue(bd.cargarRecetas().isEmpty());
        assertTrue(bd.cargarConsultasPorExpediente(expediente.getIdExpediente()).isEmpty());
    }

    @AfterAll static void cerrar() {
        BaseDatos.cerrarConexion();
        System.clearProperty("gestor.db.url");
    }
}
