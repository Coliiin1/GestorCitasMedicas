package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.modelo.Paciente;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaRegistroPacienteTest {

    @Test
    void altaDesdeAgendaAbreFormularioYRecuerdaPaciente() throws Exception {
        MainControllerStub main = new MainControllerStub();
        AgendaController agenda = new AgendaController();
        agenda.setMainController(main);

        Method abrir = AgendaController.class.getDeclaredMethod("abrirRegistroPaciente");
        abrir.setAccessible(true);
        abrir.invoke(agenda);

        assertEquals("paciente-editar-panel.fxml", main.destino);
        assertEquals("agenda-panel.fxml", main.editor.panelRegreso);
        assertTrue((Boolean) leer(agenda, "altaPendiente"));
        assertNotNull(main.editor.alRegistrar);
        assertNotNull(main.editor.alCancelar);

        Paciente paciente = new Paciente("AGEN000101HDFXXX01", new Date(), "Femenino", "", "", "");
        main.editor.alRegistrar.accept(paciente);
        assertEquals(paciente.getCurp(), leer(agenda, "curpPendienteAlta"));

        main.editor.alCancelar.run();
        assertFalse((Boolean) leer(agenda, "altaPendiente"));
        assertNull(leer(agenda, "curpPendienteAlta"));
    }

    private static Object leer(Object objeto, String campo) throws Exception {
        Field field = objeto.getClass().getDeclaredField(campo);
        field.setAccessible(true);
        return field.get(objeto);
    }

    private static final class MainControllerStub extends MainController {
        private String destino;
        private final EditorStub editor = new EditorStub();

        @Override
        public void ir(String fxml) {
            destino = fxml;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T controlador(String fxml) {
            return (T) editor;
        }
    }

    private static final class EditorStub extends PacienteEditarController {
        private String panelRegreso;
        private Consumer<Paciente> alRegistrar;
        private Runnable alCancelar;

        @Override
        public void modoNuevo(String panelRegreso, Consumer<Paciente> alRegistrar, Runnable alCancelar) {
            this.panelRegreso = panelRegreso;
            this.alRegistrar = alRegistrar;
            this.alCancelar = alCancelar;
        }
    }
}
