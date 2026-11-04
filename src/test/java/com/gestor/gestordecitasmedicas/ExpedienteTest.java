package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.controlador.ControladorPDF;
import com.gestor.gestordecitasmedicas.controlador.TextoExpediente;
import com.gestor.gestordecitasmedicas.modelo.*;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
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
        cita.setFechaCita(new Date());
        assertTrue(bd.insertarCita(cita));
        try (PreparedStatement ps = BaseDatos.getConexion().prepareStatement(
                "INSERT INTO Consulta(id_cita,id_expediente,fecha,motivo,padecimiento,observaciones) VALUES(?,?,?,?,?,?)")) {
            ps.setInt(1, cita.getIdCita()); ps.setInt(2, e.getIdExpediente());
            ps.setTimestamp(3, Timestamp.valueOf("2026-11-04 09:30:00"));
            ps.setString(4, motivo); ps.setString(5, "Dolor de cabeza"); ps.setString(6, "Mejoría clínica");
            ps.executeUpdate();
        }
        Receta r = new Receta();
        r.setIdCita(cita.getIdCita()); r.setFecha(new Date()); r.setDiagnostico(motivo);
        Medicamento m = new Medicamento();
        m.setNombreMedicamento(medicamento); m.setDosis("Una tableta"); m.setConcentracion("500 mg");
        m.setFrecuencia("Cada 8 horas"); m.setDuradcion("3 días");
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
        FutureTask<Void> verificar = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("expediente-panel.fxml"));
            VBox vista = loader.load();
            Scene scene = new Scene(vista, 1368, 900);
            scene.getStylesheets().add(getClass().getResource("cita.css").toExternalForm());
            vista.applyCss(); vista.layout();
            ExpedienteController controlador = loader.getController();
            controlador.mostrarPaciente(paciente);
            Dialog<ButtonType> cancelado = controlador.crearEditor();
            VBox campos = (VBox) cancelado.getDialogPane().getContent();
            ((TextArea) campos.getChildren().get(5)).setText("No guardar esta alergia");
            ((Button) cancelado.getDialogPane().lookupButton(ButtonType.CANCEL)).fire();
            assertNull(bd.cargarExpedientePorCurp(paciente.getCurp()).getAlergias());
            Dialog<ButtonType> editor = controlador.crearEditor();
            campos = (VBox) editor.getDialogPane().getContent();
            ((TextArea) campos.getChildren().get(1)).setText("Diabetes familiar");
            ((TextArea) campos.getChildren().get(3)).setText("Cirugía previa");
            ((TextArea) campos.getChildren().get(5)).setText("Penicilina");
            ((TextArea) campos.getChildren().get(7)).setText("Seguimiento");
            editor.getDialogPane().applyCss();
            editor.getDialogPane().resize(620, editor.getDialogPane().prefHeight(620));
            editor.getDialogPane().layout();
            ImageIO.write(SwingFXUtils.fromFXImage(editor.getDialogPane().snapshot(null, null), null), "png",
                    Paths.get("target", "expediente-editor-prueba.png").toFile());
            ButtonType guardar = editor.getDialogPane().getButtonTypes().stream()
                    .filter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE).findFirst().get();
            ((Button) editor.getDialogPane().lookupButton(guardar)).fire();
            controlador.refrescar();
            assertEquals("Penicilina", ((Label) loader.getNamespace().get("alergias")).getText());
            assertTrue(((Label) loader.getNamespace().get("antecedentes")).getText().contains("Cirugía previa"));
            assertTrue(((Label) loader.getNamespace().get("historialConsultas")).getText().contains("Control local"));
            // Las pestañas no seleccionadas aún no tienen skin; el namespace permite verificar su contenido.
            assertTrue(((Label) loader.getNamespace().get("historialRecetas")).getText().contains("Paracetamol"));
            assertTrue(((Label) loader.getNamespace().get("historialSignos")).getText().contains("120/80"));
            vista.applyCss(); vista.layout();
            ImageIO.write(SwingFXUtils.fromFXImage(vista.snapshot(null, null), null), "png",
                    Paths.get("target", "expediente-vista-prueba.png").toFile());
            assertTrue(bd.eliminarExpediente(String.valueOf(expediente.getIdExpediente())));
            controlador.mostrarPaciente(paciente);
            Dialog<ButtonType> nuevo = controlador.crearEditor();
            ((TextArea) ((VBox) nuevo.getDialogPane().getContent()).getChildren().get(5)).setText("Alergia recuperada");
            ButtonType crear = nuevo.getDialogPane().getButtonTypes().stream()
                    .filter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE).findFirst().get();
            ((Button) nuevo.getDialogPane().lookupButton(crear)).fire();
            assertEquals("Alergia recuperada", bd.cargarExpedientePorCurp(paciente.getCurp()).getAlergias());
            return null;
        });
        Platform.runLater(verificar);
        verificar.get(30, TimeUnit.SECONDS);
    }

    @AfterAll static void cerrar() {
        BaseDatos.cerrarConexion();
        System.clearProperty("gestor.db.url");
        if (javafxIniciado) Platform.exit();
    }
}
