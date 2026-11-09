package com.gestor.gestordecitasmedicas.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigracionAgendaTest {
    @TempDir Path temporal;

    @Test
    void reconstruyeBloqueosAntiguosYConservaElHorario() throws Exception {
        String url = "jdbc:sqlite:" + temporal.resolve("migracion.db");
        try (Connection con = DriverManager.getConnection(url)) {
            try (Statement s = con.createStatement()) {
                s.execute("CREATE TABLE Horario (" +
                        "id_horario INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "dia INTEGER, hora_inicio TIME, hora_fin TIME, estado BOOLEAN)");
                s.execute("CREATE TABLE Horario_bloqueado (" +
                        "id_horario INTEGER PRIMARY KEY, fecha_inicio TIMESTAMP, fecha_fin TIMESTAMP)");
                s.execute("INSERT INTO Horario (dia, hora_inicio, hora_fin, estado) " +
                        "VALUES (1, '09:00:00', '13:00:00', 1)");
                s.execute("INSERT INTO Horario_bloqueado (id_horario, fecha_inicio, fecha_fin) " +
                        "VALUES (1, '2026-12-01 00:00:00', '2026-12-02 00:00:00')");
            }

            MigracionAgenda.aplicar(con);

            Set<String> columnas = columnas(con, "Horario_bloqueado");
            assertTrue(columnas.contains("id_bloqueo"));
            assertTrue(columnas.contains("motivo"));
            assertFalse(columnas.contains("id_horario"));

            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Horario")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                         "SELECT motivo FROM Horario_bloqueado")) {
                assertTrue(rs.next());
                assertEquals("Bloqueo migrado", rs.getString(1));
                assertFalse(rs.next());
            }
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                         "SELECT COUNT(*) FROM Version_esquema WHERE clave = 'horario_inicial'")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    private static Set<String> columnas(Connection con, String tabla) throws Exception {
        Set<String> columnas = new HashSet<>();
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                columnas.add(rs.getString("name"));
            }
        }
        return columnas;
    }
}
