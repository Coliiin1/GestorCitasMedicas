package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.servicio.CarpetaPaciente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CarpetaPacienteTest {

    @TempDir Path base;

    @Test
    void creaCarpetaYRecetasYLaReutiliza() throws Exception {
        File c = CarpetaPaciente.resolver(base.toFile(), "COLD900101HMCLRN09", "Daniel Colin");
        assertEquals("Daniel Colin", c.getName());
        assertTrue(CarpetaPaciente.recetas(c).isDirectory());
        assertEquals(c, CarpetaPaciente.resolver(base.toFile(), "COLD900101HMCLRN09", "Daniel Colin"));
    }

    @Test
    void homonimoLlevaCurp() throws Exception {
        CarpetaPaciente.resolver(base.toFile(), "AAAA900101HMCLRN01", "Daniel Colin");
        File otro = CarpetaPaciente.resolver(base.toFile(), "BBBB900101HMCLRN02", "Daniel Colin");
        assertEquals("Daniel Colin (BBBB900101HMCLRN02)", otro.getName());
        assertEquals(otro, CarpetaPaciente.resolver(base.toFile(), "BBBB900101HMCLRN02", "Daniel Colin"));
    }

    @Test
    void cambioDeNombreRenombraYConservaContenido() throws Exception {
        File c = CarpetaPaciente.resolver(base.toFile(), "COLD900101HMCLRN09", "Daniel Colin");
        Files.write(new File(CarpetaPaciente.recetas(c), "receta_1.pdf").toPath(), new byte[]{1});
        File nuevo = CarpetaPaciente.resolver(base.toFile(), "COLD900101HMCLRN09", "Daniel Colin Pérez");
        assertEquals("Daniel Colin Pérez", nuevo.getName());
        assertFalse(c.exists());
        assertTrue(new File(nuevo, "recetas/receta_1.pdf").exists());
    }

    @Test
    void saneaCaracteresInvalidos() throws Exception {
        File c = CarpetaPaciente.resolver(base.toFile(), "X1", "Ana: \"X\"/Y. ");
        assertEquals("Ana XY", c.getName());
    }
}
