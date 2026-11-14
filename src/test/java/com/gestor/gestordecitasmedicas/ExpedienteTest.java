package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.*;
import com.gestor.gestordecitasmedicas.servicio.ControladorPDF;
import com.gestor.gestordecitasmedicas.servicio.TextoExpediente;
import com.gestor.gestordecitasmedicas.modelo.*;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Date;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ExpedienteTest {
    @TempDir static Path temporal;
    private final BaseDatos bd = new BaseDatos();
    private Paciente paciente;
    private Expediente expediente;
    private static boolean javafxIniciado;
    private static int turnos;

    @BeforeAll static void configurar() {
        System.setProperty("gestor.db.url", "jdbc:sqlite:" + temporal.resolve("pruebas.db"));
    }

    @BeforeEach void preparar() throws Exception {
        Connection con = BaseDatos.getConexion();
        // Impide que una configuración incorrecta borre datos de la base local.
        assertTrue(con.getMetaData().getURL().contains(temporal.toString()));
        try (Statement s = con.createStatement()) { s.executeUpdate("DELETE FROM Paciente"); }
        paciente = paciente("PRUE000101HDFXXX01", "Prueba", "Calle de prueba 123");
        expediente = expediente(paciente);
    }

    private Paciente paciente(String curp, String nombre, String domicilio) {
        Paciente p = new Paciente(curp, java.sql.Date.valueOf("2000-01-01"), "Femenino", domicilio,
                "5555555555", "prueba@example.com");
        p.setNombre(nombre);
        p.setApellidoPaterno("Paciente");
        p.setApellidoMaterno("Local");
        assertTrue(bd.insertarPaciente(p));
        return p;
    }

    private Expediente expediente(Paciente p) {
        Expediente e = new Expediente();
        e.setDatosPaciente(p);
        e.setFechaApertura(new Date(1730764800000L));
        assertTrue(bd.insertarExpediente(e));
        return e;
    }

    @Test void cambiosPersistenAlCerrarYReabrirConexion() {
        Expediente e = bd.cargarExpedientePorCurp(paciente.getCurp());
        assertNotNull(e.getDatosPaciente());
        e.setAlergias("Penicilina");
        e.setAntecedentesFamiliares("Diabetes familiar");
        e.setAntecedentesPersonales("Cirugía previa");
        e.setObservaciones("Seguimiento en dos semanas");
        assertTrue(bd.actualizarExpediente(e));
        BaseDatos.cerrarConexion();
        Expediente guardado = bd.cargarExpedientePorCurp(paciente.getCurp());
        assertEquals(e.getIdExpediente(), guardado.getIdExpediente());
        assertEquals(e.getFechaApertura(), guardado.getFechaApertura());
        assertEquals("Penicilina", guardado.getAlergias());
        assertEquals("Diabetes familiar", guardado.getAntecedentesFamiliares());
        assertEquals("Cirugía previa", guardado.getAntecedentesPersonales());
        assertEquals("Seguimiento en dos semanas", guardado.getObservaciones());
        assertEquals(paciente.getCurp(), guardado.getDatosPaciente().getCurp());
        guardado.setAlergias("");
        assertTrue(bd.actualizarExpediente(guardado));
        assertEquals("", bd.cargarExpedientePorCurp(paciente.getCurp()).getAlergias());
    }

    private void historia(Paciente p, Expediente e, String motivo, String medicamento) throws Exception {
        Cita cita = new Cita();
        cita.setCurp(p.getCurp());
        // Cada turno admite una sola cita vigente: las de prueba no deben coincidir en el mismo instante.
        cita.setFechaCita(new Date(System.currentTimeMillis() + 60_000L * ++turnos));
        assertTrue(bd.insertarCita(cita));
        try (PreparedStatement ps = BaseDatos.getConexion().prepareStatement(
                "INSERT INTO Consulta(id_cita,fecha,motivo,padecimiento,observaciones) VALUES(?,?,?,?,?)")) {
            ps.setInt(1, cita.getIdCita());
            ps.setTimestamp(2, Timestamp.valueOf("2026-11-04 09:30:00"));
            ps.setString(3, motivo); ps.setString(4, "Dolor de cabeza"); ps.setString(5, "Mejoría clínica");
            ps.executeUpdate();
        }
        Receta r = new Receta();
        r.setIdCita(cita.getIdCita()); r.setFecha(new Date()); r.setDiagnostico(motivo);
        Medicamento m = new Medicamento();
        m.setNombreMedicamento(medicamento); m.setDosis("Una tableta"); m.setConcentracion("500 mg");
        m.setFrecuencia("Cada 8 horas"); m.setDuracion("3 días");
        r.getMedicamentos().add(m);
        r.setSignosVitales(new SignosVitales("120/80", 72, 18, 36.5f, 98, 70.5f, 170));
        assertTrue(bd.insertarReceta(r));
    }

    @Test void historialAislaLosDatosDeCadaPaciente() throws Exception {
        historia(paciente, expediente, "Control local", "Paracetamol");
        Paciente otro = paciente("OTRO000101HDFXXX02", "Otro", "Dirección ajena");
        historia(otro, expediente(otro), "Consulta ajena", "Medicamento ajeno");
        Expediente e = bd.cargarExpedientePorCurp(paciente.getCurp());
        assertEquals(1, e.getConsultas().size()); assertEquals(1, e.getRecetas().size());
        assertEquals("Control local", e.getConsultas().get(0).getMotivo());
        assertTrue(TextoExpediente.recetas(e).contains("Paracetamol"));
        assertFalse(TextoExpediente.recetas(e).contains("ajeno"));
        assertTrue(TextoExpediente.signos(e).contains("120/80"));
        assertEquals(2, bd.cargarRecetas().size());
    }

    @Test void expedienteVacioYRecuperacionDeExpedienteFaltante() {
        Expediente e = bd.cargarExpedientePorCurp(paciente.getCurp());
        assertTrue(e.getConsultas().isEmpty()); assertTrue(e.getRecetas().isEmpty());
        assertTrue(TextoExpediente.consultas(e).contains("No hay consultas"));
        assertTrue(bd.eliminarExpediente(String.valueOf(e.getIdExpediente())));
        assertNull(bd.cargarExpedientePorCurp(paciente.getCurp()));
        expediente(paciente);
        assertNotNull(bd.cargarExpedientePorCurp(paciente.getCurp()));
        assertNull(bd.cargarExpedientePorCurp("NO_EXISTE"));
    }

    @Test void consultasAceptanFechaSqliteDeTextoYLosErroresNoParecenHistorialVacio() throws Exception {
        historia(paciente, expediente, "Fecha de texto", "Paracetamol");
        try (PreparedStatement ps = BaseDatos.getConexion().prepareStatement("UPDATE Consulta SET fecha = ?")) {
            ps.setString(1, "2026-11-04"); ps.executeUpdate();
            assertEquals(java.time.LocalDateTime.of(2026, 11, 4, 0, 0),
                    bd.cargarExpedientePorCurp(paciente.getCurp()).getConsultas().get(0).getFechaConsulta());
            ps.setString(1, "2026-11-04 09:30:00"); ps.executeUpdate();
            assertEquals(9, bd.cargarExpedientePorCurp(paciente.getCurp()).getConsultas().get(0).getFechaConsulta().getHour());
            ps.setString(1, "fecha inválida"); ps.executeUpdate();
            assertThrows(IllegalStateException.class, () -> bd.cargarExpedientePorCurp(paciente.getCurp()));
        }
    }

    @Test void pdfIncluyeDatosClinicosEHistorial() throws Exception {
        historia(paciente, expediente, "Control local", "Paracetamol");
        expediente.setAlergias("Penicilina");
        expediente.setAntecedentesFamiliares("Diabetes familiar");
        assertTrue(bd.actualizarExpediente(expediente));
        Path salida = Paths.get("target", "expediente-prueba.pdf");
        new ControladorPDF().generarExpediente(salida.toFile(), bd.cargarExpedientePorCurp(paciente.getCurp()));
        PdfReader reader = new PdfReader(salida.toString());
        try {
            StringBuilder texto = new StringBuilder();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) texto.append(PdfTextExtractor.getTextFromPage(reader, i));
            for (String esperado : new String[]{paciente.getCurp(), "Calle de prueba 123", "Penicilina",
                    "Diabetes familiar", "Control local", "Paracetamol", "120/80", "Cada 8 horas"}) {
                assertTrue(texto.toString().contains(esperado), esperado);
            }
        } finally { reader.close(); }
    }

    @Test void pantallaCargaHistorialYElEditorGuardaOCancela() throws Exception {
        historia(paciente, expediente, "Control local", "Paracetamol");
        Platform.startup(() -> { });
        javafxIniciado = true;
        java.util.concurrent.CompletableFuture<Boolean> confirmacionOculta = new java.util.concurrent.CompletableFuture<>();
        FutureTask<Void> verificar = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("expediente-panel.fxml"));
            VBox vista = loader.load();
            Scene scene = new Scene(vista, 1368, 900);
            scene.getStylesheets().add(getClass().getResource("cita.css").toExternalForm());
            vista.applyCss(); vista.layout();
            ExpedienteController controlador = loader.getController();
            controlador.mostrarPaciente(paciente);
            Button editar = (Button) loader.getNamespace().get("btnEditar");
            FXMLLoader cargadorEditor = new FXMLLoader(getClass().getResource("expediente-editar-panel.fxml"));
            VBox vistaEditor = cargadorEditor.load();
            ExpedienteEditarController editorControlador = cargadorEditor.getController();
            PrincipalPrueba principal = new PrincipalPrueba(controlador, editorControlador);
            editorControlador.setMainController(principal);
            controlador.setMainController(principal);
            Label confirmacion = (Label) loader.getNamespace().get("estadoEdicion");
            VBox campos = (VBox) cargadorEditor.getNamespace().get("camposEditor");
            Scene escenaEditor = new Scene(vistaEditor, 1368, 900);
            escenaEditor.getStylesheets().add(getClass().getResource("cita.css").toExternalForm());

            editar.fire();
            assertEquals("expediente-editar-panel.fxml", principal.destino);
            ((TextArea) campos.lookup("#alergiasEditor")).setText("No guardar esta alergia");
            java.lang.reflect.Method cancelarEditor = ExpedienteEditarController.class.getDeclaredMethod("onCancelar");
            cancelarEditor.setAccessible(true);
            cancelarEditor.invoke(editorControlador);
            assertEquals("expediente-panel.fxml", principal.destino);
            assertNull(bd.cargarExpedientePorCurp(paciente.getCurp()).getAlergias());
            java.lang.reflect.Method guardarEditor = ExpedienteEditarController.class.getDeclaredMethod("onGuardar");
            guardarEditor.setAccessible(true);
            editar.fire();
            ((TextArea) campos.lookup("#familiares")).setText("Diabetes familiar");
            ((TextArea) campos.lookup("#personales")).setText("Cirugía previa");
            ((TextArea) campos.lookup("#alergiasEditor")).setText("Penicilina");
            ((TextArea) campos.lookup("#observaciones")).setText("Seguimiento");
            javafx.scene.control.TextField fecha = (javafx.scene.control.TextField) campos.lookup("#fechaHoraHistoria");
            String fechaValida = fecha.getText();
            fecha.setText("fecha inválida"); guardarEditor.invoke(editorControlador);
            assertEquals("expediente-editar-panel.fxml", principal.destino);
            assertTrue(((Label) cargadorEditor.getNamespace().get("errorEdicion")).isVisible());
            assertFalse(confirmacion.isVisible());
            assertNull(bd.cargarExpedientePorCurp(paciente.getCurp()).getAlergias());
            fecha.setText(fechaValida);
            vistaEditor.applyCss(); vistaEditor.layout();
            assertEquals(5, campos.lookupAll(".section-label").size());
            assertEquals(24, campos.lookupAll(".field-label").size());
            assertEquals("Tipo de Establecimiento", ((Label) campos.lookupAll(".field-label").iterator().next()).getText());
            assertEquals("Ej. Faringitis aguda.", ((javafx.scene.control.TextInputControl) campos.lookup("#diagnostico")).getPromptText());
            ImageIO.write(SwingFXUtils.fromFXImage(vistaEditor.snapshot(null, null), null), "png",
                    Paths.get("target", "expediente-editor-prueba.png").toFile());
            guardarEditor.invoke(editorControlador);
            assertEquals("expediente-panel.fxml", principal.destino);
            assertTrue(confirmacion.isVisible());
            assertTrue(confirmacion.isManaged());
            assertEquals("Expediente actualizado correctamente.", confirmacion.getText());
            assertEquals("Penicilina", ((Label) loader.getNamespace().get("alergias")).getText());
            assertTrue(((Label) loader.getNamespace().get("antecedentes")).getText().contains("Cirugía previa"));
            assertFalse(((javafx.scene.layout.VBox) loader.getNamespace().get("listaConsultas")).getChildren().isEmpty());
            // Las pestañas no seleccionadas aún no tienen skin; el namespace permite verificar su contenido.
            assertNull(loader.getNamespace().get("historialRecetas"));
            assertTrue(((Label) loader.getNamespace().get("historialSignos")).getText().contains("120/80"));
            vista.applyCss(); vista.layout();
            ImageIO.write(SwingFXUtils.fromFXImage(vista.snapshot(null, null), null), "png",
                    Paths.get("target", "expediente-vista-prueba.png").toFile());
            assertTrue(bd.eliminarExpediente(String.valueOf(expediente.getIdExpediente())));
            controlador.mostrarPaciente(paciente);
            editar.fire();
            ((TextArea) campos.lookup("#alergiasEditor")).setText("Alergia recuperada");
            guardarEditor.invoke(editorControlador);
            assertEquals("Alergia recuperada", bd.cargarExpedientePorCurp(paciente.getCurp()).getAlergias());
            verificarCapturaConsulta();
            confirmacion.visibleProperty().addListener((obs, anterior, visible) -> {
                if (!visible) Platform.runLater(() -> confirmacionOculta.complete(!confirmacion.isManaged()));
            });
            return null;
        });
        Platform.runLater(verificar);
        verificar.get(30, TimeUnit.SECONDS);
        assertTrue(confirmacionOculta.get(7, TimeUnit.SECONDS), "La confirmación debe ocultarse y liberar su espacio después de cinco segundos.");
    }

    /** MainController mínimo: registra el destino y entrega los controladores ya cargados. */
    private static final class PrincipalPrueba extends MainController {
        private final ExpedienteController expediente;
        private final ExpedienteEditarController editor;
        private String destino;

        PrincipalPrueba(ExpedienteController expediente, ExpedienteEditarController editor) {
            this.expediente = expediente;
            this.editor = editor;
        }

        @Override public void ir(String fxml) {
            destino = fxml;
            if ("expediente-panel.fxml".equals(fxml)) expediente.refrescar();
        }

        @Override @SuppressWarnings("unchecked")
        public <T> T controlador(String fxml) {
            return (T) ("expediente-editar-panel.fxml".equals(fxml) ? editor : expediente);
        }
    }

    private void verificarCapturaConsulta() throws Exception {
        try (Statement s = BaseDatos.getConexion().createStatement()) {
            s.executeUpdate("UPDATE Administrador SET nombre='María', apellido_paterno='Médica', apellido_materno='Ejemplo', "
                    + "cedula='12345678', profesion='Médica cirujana', \"institución\"='Universidad de prueba', "
                    + "domicilio='Calle Consultorio 123, Ciudad de México', nombre_establecimiento='Consultorio de prueba', "
                    + "domicilio_establecimiento='Calle Consultorio 123, Ciudad de México'");
        }
        Cita nueva = new Cita(); nueva.setCurp(paciente.getCurp()); nueva.setEstado(Estado.AGENDADA); nueva.setFechaCita(new Date());
        assertTrue(bd.insertarCita(nueva));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("consulta-panel.fxml"));
        VBox vista = loader.load();
        Scene escena = new Scene(vista, 1368, 900);
        escena.getStylesheets().add(getClass().getResource("cita.css").toExternalForm());
        ConsultaController controlador = loader.getController();
        controlador.cargarConsulta(nueva, new SignosVitales("120/80",72,18,36.5f,98,70f,170));
        for (String id : new String[]{"txtNombrePaciente", "txtEdadPaciente", "txtSexoPaciente"}) {
            javafx.scene.control.TextField campo = (javafx.scene.control.TextField) loader.getNamespace().get(id);
            assertFalse(campo.isEditable());
            assertFalse(campo.getText().isEmpty());
        }
        assertEquals(TextoExpediente.nombre(paciente), ((javafx.scene.control.TextField) loader.getNamespace().get("txtNombrePaciente")).getText());
        assertFalse(loader.getNamespace().containsKey("txtNombreMedico"));
        assertFalse(loader.getNamespace().containsKey("txtNombreEstablecimiento"));
        String[][] campos = {{"txtDiagnostico","Diagnóstico independiente"}, {"txtPadecimiento","Padecimiento actual"},
                {"txtEvolucion","Evolución independiente"}, {"txtExploracion","Exploración física"}, {"txtEstudios","Sin estudios solicitados"},
                {"txtIndicaciones","Indicaciones independientes"}};
        for(String[] campo:campos) {
            javafx.scene.control.TextInputControl entrada=(javafx.scene.control.TextInputControl) loader.getNamespace().get(campo[0]);
            assertNotNull(entrada,campo[0]); entrada.setText(campo[1]);
        }
        java.lang.reflect.Field filas=ConsultaController.class.getDeclaredField("filas"); filas.setAccessible(true);
        Object fila=((java.util.List<?>)filas.get(controlador)).get(0);
        for(String[] campo:new String[][]{{"nombre","Medicamento de prueba"},{"dosis","1 tableta"},{"duracion","10 días"},{"cantidad","10 tabletas"}}) {
            java.lang.reflect.Field f=fila.getClass().getDeclaredField(campo[0]); f.setAccessible(true);
            ((javafx.scene.control.TextField)f.get(fila)).setText(campo[1]);
        }
        java.lang.reflect.Field grupo=fila.getClass().getDeclaredField("grupo"); grupo.setAccessible(true);
        @SuppressWarnings("unchecked") javafx.scene.control.ComboBox<String> selector=(javafx.scene.control.ComboBox<String>)grupo.get(fila);
        selector.setValue("IV");
        java.lang.reflect.Method guardar=ConsultaController.class.getDeclaredMethod("guardarAtencion",boolean.class,boolean.class); guardar.setAccessible(true);
        assertEquals(true,guardar.invoke(controlador,false,true));
        assertEquals(Estado.AGENDADA,bd.cargarCitas().get(nueva.getIdCita()).getEstado());
        assertEquals("Diagnóstico independiente",bd.cargarRecetaPorCita(nueva.getIdCita()).getDiagnostico());
        assertEquals("Evolución independiente",bd.cargarConsultaPorCita(nueva.getIdCita()).getEvolucion());
        assertEquals("María Médica Ejemplo", bd.cargarRecetaPorCita(nueva.getIdCita()).getNombreMedico());
        assertEquals("12345678", bd.cargarRecetaPorCita(nueva.getIdCita()).getCedulaMedico());
        assertEquals("Consultorio de prueba", bd.cargarRecetaPorCita(nueva.getIdCita()).getNombreEstablecimiento());
        try (Statement s = BaseDatos.getConexion().createStatement()) {
            s.executeUpdate("UPDATE Administrador SET nombre='Nombre cambiado después'");
        }
        controlador.cargarConsulta(nueva, null);
        assertEquals("Indicaciones independientes",((TextArea)loader.getNamespace().get("txtIndicaciones")).getText());
        assertEquals(true,guardar.invoke(controlador,true,false));
        assertEquals(Estado.ATENDIDA,bd.cargarCitas().get(nueva.getIdCita()).getEstado());
        assertEquals("María Médica Ejemplo", bd.cargarRecetaPorCita(nueva.getIdCita()).getNombreMedico());
        vista.applyCss(); vista.layout();
        javafx.scene.control.TextField ficha = (javafx.scene.control.TextField) loader.getNamespace().get("txtNombrePaciente");
        javafx.scene.control.TextField diagnostico = (javafx.scene.control.TextField) loader.getNamespace().get("txtDiagnostico");
        assertTrue(ficha.localToScene(ficha.getBoundsInLocal()).getMinY() < diagnostico.localToScene(diagnostico.getBoundsInLocal()).getMinY());
        ImageIO.write(SwingFXUtils.fromFXImage(vista.snapshot(null,null),null),"png",Paths.get("target","consulta-normativa-prueba.png").toFile());
    }

    @AfterAll static void cerrar() {
        BaseDatos.cerrarConexion();
        System.clearProperty("gestor.db.url");
        if (javafxIniciado) Platform.exit();
    }
}
