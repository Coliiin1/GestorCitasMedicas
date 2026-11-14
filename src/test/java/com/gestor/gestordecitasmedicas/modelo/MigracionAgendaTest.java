package com.gestor.gestordecitasmedicas.modelo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigracionAgendaTest {
    @TempDir Path temporal;

    @Test
    void siembraElHorarioInicialUnaSolaVez() throws Exception {
        System.setProperty("gestor.db.url", "jdbc:sqlite:" + temporal.resolve("agenda.db"));
        assertEquals(10, new BaseDatos().cargarHorarios().size());

        try (Statement s = BaseDatos.getConexion().createStatement()) {
            s.executeUpdate("DELETE FROM Horario");
        }
        BaseDatos.cerrarConexion();
        assertTrue(new BaseDatos().cargarHorarios().isEmpty());
    }

    @Test
    void rechazaUnaBaseDeEsquemaAnterior() throws Exception {
        String url = "jdbc:sqlite:" + temporal.resolve("esquema-anterior.db");
        try (Connection con = DriverManager.getConnection(url); Statement s = con.createStatement()) {
            s.execute("CREATE TABLE Expediente (id_expediente INTEGER PRIMARY KEY, CURP TEXT)");
        }
        System.setProperty("gestor.db.url", url);
        assertNull(BaseDatos.getConexion());
    }

    @AfterEach
    void cerrar() {
        BaseDatos.cerrarConexion();
        System.clearProperty("gestor.db.url");
    }
}
