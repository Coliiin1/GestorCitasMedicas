package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.servicio.ControladorPDF;
import com.gestor.gestordecitasmedicas.servicio.TextoExpediente;
import com.gestor.gestordecitasmedicas.servicio.ValidacionClinica;
import com.gestor.gestordecitasmedicas.modelo.*;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class FormatoClinicoTest {
    @TempDir static Path temporal;
    final BaseDatos bd = new BaseDatos();
    Paciente paciente;
    Cita cita;

    @BeforeEach void preparar() {
        BaseDatos.cerrarConexion();
        System.setProperty("gestor.db.url", "jdbc:sqlite:" + temporal.resolve("formato-" + System.nanoTime() + ".db"));
        paciente = new Paciente("PRUE900101MDFXXX01", java.sql.Date.valueOf("1990-01-01"), "Femenino",
                "Calle Paciente 10, Centro, Ciudad de México", "5555555555", "prueba@example.com");
        paciente.setNombre("Ana"); paciente.setApellidoPaterno("Prueba"); paciente.setApellidoMaterno("Ejemplo");
        assertTrue(bd.insertarPaciente(paciente));
        cita = new Cita(); cita.setCurp(paciente.getCurp()); cita.setFechaCita(new Date()); cita.setEstado(Estado.AGENDADA);
        assertTrue(bd.insertarCita(cita));
    }

    static Receta recetaCompleta() {
        Receta r = new Receta();
        r.setFecha(Timestamp.valueOf("2026-11-05 10:35:45"));
        r.setNombrePaciente("Ana Prueba Ejemplo"); r.setEdadPaciente("36 años, 10 meses, 4 días"); r.setSexoPaciente("Femenino");
        r.setNombreMedico("María Médica Ejemplo"); r.setCedulaMedico("12345678"); r.setProfesion("Médica cirujana");
        r.setInstitucionTitulo("Universidad de prueba");
        r.setDomicilioMedico("Calle Consultorio 123, colonia Centro, Ciudad de México, C.P. 06000");
        r.setNombreEstablecimiento("Consultorio de prueba"); r.setDomicilioEstablecimiento(r.getDomicilioMedico());
        r.setDiagnostico("Diagnóstico de prueba para validar el formato");
        r.setIndicaciones("Indicaciones de ejemplo para comprobar la impresión y el seguimiento.");
        Medicamento m = new Medicamento(); m.setNombreMedicamento("Medicamento de prueba"); m.setConcentracion("10 mg");
        m.setPresentacion("Tabletas, caja con 10 unidades"); m.setDosis("1 tableta"); m.setViaAdministracion("Oral");
        m.setFrecuencia("Cada 24 horas"); m.setDuracion("10 días"); m.setCantidad("1 caja con 10 tabletas"); m.setGrupoRegulatorio("IV");
        r.getMedicamentos().add(m);
        r.setSignosVitales(new SignosVitales("120/80", 72, 18, 36.5f, 98, 70f, 170));
        return r;
    }

    static Consulta nota(Receta r) {
        Consulta c = new Consulta(); c.setFechaConsulta(LocalDateTime.of(2026,11,5,10,35,45));
        c.setPadecimiento("Padecimiento de prueba"); c.setEvolucion("Evolución de prueba"); c.setExploracionFisica("Exploración de prueba");
        c.setResultadosEstudios("No se solicitaron estudios"); c.setPronostico("Pronóstico de prueba");
        c.setReceta(r); return c;
    }

    @Test void impideExportarRecetasIncompletasYFraccionesFueraDelAlcance() throws Exception {
        Receta r = recetaCompleta();
        assertTrue(ValidacionClinica.receta(r,true).isEmpty());
        Medicamento m = r.getMedicamentos().get(0); m.setDosis(null);
        Path salida = temporal.resolve("no-emitir.pdf");
        assertThrows(IllegalArgumentException.class, () -> new ControladorPDF().generarReceta(salida.toFile(),r));
        assertFalse(Files.exists(salida));
        m.setDosis("1 tableta oral cada 8 horas");
        for (String grupo : new String[]{"I", "II", "III"}) {
            m.setGrupoRegulatorio(grupo);
            assertFalse(ValidacionClinica.receta(r,true).isEmpty());
        }
        m.setGrupoRegulatorio("IV"); r.setEspecialidad("Medicina interna");
        assertTrue(ValidacionClinica.receta(r,true).stream().anyMatch(e->e.contains("registro de especialidad")));
        r.setRegistroEspecialidad("87654321"); assertTrue(ValidacionClinica.receta(r,true).isEmpty());
        r.setCedulaMedico("00000000"); assertFalse(ValidacionClinica.receta(r,true).isEmpty());
    }

    @Test void permiteAtencionSinFarmacosPeroNoRecetaVacia() {
        Receta r = recetaCompleta(); r.getMedicamentos().clear(); Consulta c = nota(r);
        assertTrue(ValidacionClinica.atencion(c,r,false).isEmpty());
        assertFalse(ValidacionClinica.atencion(c,r,true).isEmpty());
        c.setEvolucion(""); c.setResultadosEstudios(""); c.setPronostico("");
        assertTrue(ValidacionClinica.atencion(c,r,false).isEmpty());
    }

    @Test void guardaBorradorYFinalizaConSnapshotsYVersionesPersistentes() throws Exception {
        Receta r = recetaCompleta(); r.setCurp(paciente.getCurp()); Consulta c = nota(r);
        assertTrue(bd.guardarAtencion(cita,c,r,false));
        assertEquals(Estado.AGENDADA, bd.cargarCitas().get(cita.getIdCita()).getEstado());
        BaseDatos.cerrarConexion();
        Receta recuperada = bd.cargarRecetaPorCita(cita.getIdCita());
        assertEquals(r.getFecha().getTime(), recuperada.getFecha().getTime());
        assertEquals("Oral", recuperada.getMedicamentos().get(0).getViaAdministracion());
        assertEquals("Tabletas, caja con 10 unidades", recuperada.getMedicamentos().get(0).getPresentacion());
        assertEquals("IV", recuperada.getMedicamentos().get(0).getGrupoRegulatorio());
        Consulta recuperadaNota=bd.cargarConsultaPorCita(cita.getIdCita());
        assertEquals(c.getPronostico(),recuperadaNota.getPronostico());
        // Guardar de nuevo sin cambios no debe archivar copias idénticas.
        assertTrue(bd.guardarAtencion(cita,recuperadaNota,recuperada,false));
        try (Statement st=BaseDatos.getConexion().createStatement(); ResultSet rs=st.executeQuery("SELECT (SELECT COUNT(*) FROM Version_Receta)"
                + " + (SELECT COUNT(*) FROM Version_Consulta) + (SELECT COUNT(*) FROM Version_Signos_vitales)")) {
            assertTrue(rs.next()); assertEquals(0,rs.getInt(1));
        }
        recuperada.setDiagnostico("Diagnóstico actualizado");
        assertTrue(bd.guardarAtencion(cita,recuperadaNota,recuperada,true));
        assertEquals(Estado.ATENDIDA, bd.cargarCitas().get(cita.getIdCita()).getEstado());
        paciente.setNombre("Nombre modificado después"); assertTrue(bd.actualizarPaciente(paciente));
        assertEquals("Ana Prueba Ejemplo", bd.cargarRecetaPorCita(cita.getIdCita()).getNombrePaciente());
        try (Statement st=BaseDatos.getConexion().createStatement(); ResultSet rs=st.executeQuery("SELECT diagnostico FROM Version_Receta WHERE id_receta="+r.getIdReceta())) {
            assertTrue(rs.next()); assertEquals(r.getDiagnostico(),rs.getString(1));
        }
        assertEquals(1,bd.cargarExpedientePorCurp(paciente.getCurp()).getConsultas().size());
    }

    @Test void rollbackNoDejaIdsFalsosNiVersionesParciales() throws Exception {
        Receta r=recetaCompleta(); Consulta c=nota(r);
        try(Statement s=BaseDatos.getConexion().createStatement()) {
            s.execute("CREATE TRIGGER falla_nota BEFORE INSERT ON Consulta BEGIN SELECT RAISE(ABORT, 'fallo de prueba'); END");
        }
        assertFalse(bd.guardarAtencion(cita,c,r,true));
        assertEquals(0,r.getIdReceta()); assertEquals(0,c.getIdConsulta());
        assertNull(bd.cargarRecetaPorCita(cita.getIdCita()));
        assertNull(bd.cargarExpedientePorCurp(paciente.getCurp()));
        assertEquals(Estado.AGENDADA,bd.cargarCitas().get(cita.getIdCita()).getEstado());
    }

    @Test void noPermiteCruzarRecetaConOtraCita() {
        Receta r=recetaCompleta(); Consulta c=nota(r); assertTrue(bd.guardarAtencion(cita,c,r,false));
        Cita otra=new Cita(); otra.setCurp(paciente.getCurp()); otra.setFechaCita(new Date(System.currentTimeMillis() + 3_600_000L)); otra.setEstado(Estado.AGENDADA);
        assertTrue(bd.insertarCita(otra));
        assertFalse(bd.guardarAtencion(otra,nota(r),r,true));
        assertNull(bd.cargarRecetaPorCita(otra.getIdCita()));
    }

    @Test void pdfContieneIdentificacionPosologiaYFirmaInclusoEnMultiplesPaginas() throws Exception {
        Receta r=recetaCompleta(); r.setCurp(paciente.getCurp()); r.setIdReceta(123);
        Path ordinaria=Paths.get("target","receta-ordinaria-prueba.pdf");
        new ControladorPDF().generarReceta(ordinaria.toFile(),r);
        PdfReader unaPagina=new PdfReader(ordinaria.toString());
        try { assertEquals(1,unaPagina.getNumberOfPages()); }
        finally { unaPagina.close(); }
        // Enough medications to exercise continuation headers and signature placement.
        for(int i=0;i<5;i++) r.getMedicamentos().add(r.getMedicamentos().get(0));
        Path salida=Paths.get("target","receta-normativa-prueba.pdf");
        new ControladorPDF().generarReceta(salida.toFile(),r);
        PdfReader pdf=new PdfReader(salida.toString());
        try {
            assertTrue(pdf.getNumberOfPages()>1);
            StringBuilder texto=new StringBuilder();
            for(int i=1;i<=pdf.getNumberOfPages();i++) {
                String pagina=PdfTextExtractor.getTextFromPage(pdf,i);
                assertTrue(pagina.contains("Ana Prueba Ejemplo")); assertTrue(pagina.contains("Página "+i)); texto.append(pagina);
            }
            for(String esperado:new String[]{r.getNombreMedico(),r.getCedulaMedico(),r.getInstitucionTitulo(),"Oral","Cada 24 horas","10 días","1 caja con 10 tabletas","Firma autógrafa","05/11/2026 10:35"})
                assertTrue(texto.toString().contains(esperado),esperado);
        } finally { pdf.close(); }
        Expediente e=new Expediente(); e.setDatosPaciente(paciente); e.setIdExpediente(123); e.setFechaApertura(new Date());
        e.setNombreEstablecimiento(r.getNombreEstablecimiento()); e.setDomicilioEstablecimiento(r.getDomicilioEstablecimiento());
        Consulta c=nota(r); c.setIdConsulta(1); e.getConsultas().add(c);
        new ControladorPDF().generarExpediente(Paths.get("target","expediente-normativo-prueba.pdf").toFile(),e);
        assertTrue(TextoExpediente.historia(e).contains("BORRADOR"));
    }

    @Test void guardaTodosLosApartadosDeHistoriaSinRellenarRegistrosAntiguos() throws Exception {
        Expediente e=new Expediente(); e.setDatosPaciente(paciente); e.setFechaApertura(new Date());
        e.setTipoEstablecimiento("Consultorio"); e.setNombreEstablecimiento("Consultorio de prueba");
        e.setDomicilioEstablecimiento("Calle Consultorio 123, Ciudad de México");
        e.setInstitucionEstablecimiento("Institución de prueba"); e.setPropietario("Propietario de prueba"); e.setGrupoEtnico("No refiere");
        e.setAntecedentesFamiliares("Antecedentes familiares de prueba"); e.setAntecedentesPersonales("Antecedentes patológicos de prueba");
        e.setAntecedentesNoPatologicos("Antecedentes no patológicos de prueba"); e.setConsumoSustancias("Consumo de sustancias documentado");
        e.setPadecimientoActual("Padecimiento y tratamientos previos"); e.setInterrogatorioSistemas("Interrogatorio por sistemas de prueba");
        e.setExploracionFisica("Exploración por regiones de prueba"); e.setSignosHistoria("120/80 mmHg; 72 latidos por minuto; 18 respiraciones por minuto; 36.5 °C; 70 kg; 170 cm");
        e.setResultadosEstudios("Sin estudios previos"); e.setDiagnostico("Diagnóstico de prueba"); e.setPronostico("Pronóstico documentado");
        e.setIndicacionTerapeutica("Terapéutica documentada"); e.setAutorHistoria("María Médica Ejemplo"); e.setCedulaHistoria("12345678"); e.setFechaHoraHistoria("2026-11-05T11:45");
        assertTrue(ValidacionClinica.historia(e).isEmpty()); assertTrue(bd.insertarExpediente(e));
        BaseDatos.cerrarConexion(); Expediente cargado=bd.cargarExpedientePorCurp(paciente.getCurp());
        assertEquals(e.getSignosHistoria(),cargado.getSignosHistoria()); assertEquals(e.getAutorHistoria(),cargado.getAutorHistoria());
        assertEquals(e.getInterrogatorioSistemas(),cargado.getInterrogatorioSistemas());
        assertEquals(e.getConsumoSustancias(),cargado.getConsumoSustancias()); assertTrue(ValidacionClinica.historia(cargado).isEmpty());
        cargado.setPronostico("Pronóstico revisado"); assertTrue(bd.actualizarExpediente(cargado));
        assertEquals("Pronóstico revisado",bd.cargarExpedientePorCurp(paciente.getCurp()).getPronostico());
        try(Statement s=BaseDatos.getConexion().createStatement(); ResultSet rs=s.executeQuery("SELECT pronostico FROM Version_Expediente WHERE id_expediente="+e.getIdExpediente())) {
            assertTrue(rs.next()); assertEquals(e.getPronostico(),rs.getString(1));
        }
        new ControladorPDF().generarExpediente(Paths.get("target","historia-completa-prueba.pdf").toFile(),cargado);
    }

    @AfterAll static void cerrar() { BaseDatos.cerrarConexion(); System.clearProperty("gestor.db.url"); }
}
