package com.gestor.gestordecitasmedicas.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Migración aditiva de horarios y bloqueos. El bloqueo deja de depender de un
 * tramo semanal y pasa a ser una excepción global por rango de fecha/hora.
 */
final class MigracionAgenda {
    private static final String MARCADOR_HORARIO = "horario_inicial";

    private MigracionAgenda() {
    }

    static void aplicar(Connection con) throws SQLException {
        reconstruirBloqueos(con);
        crearIndices(con);
        sembrarHorarioInicial(con);
    }

    private static void reconstruirBloqueos(Connection con) throws SQLException {
        Set<String> columnas = columnas(con, "Horario_bloqueado");
        if (columnas.isEmpty() || columnas.contains("id_bloqueo")) {
            return;
        }

        try (Statement s = con.createStatement()) {
            s.execute("DROP TABLE IF EXISTS Horario_bloqueado_nueva");
            s.execute("CREATE TABLE Horario_bloqueado_nueva (" +
                    "id_bloqueo INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "fecha_inicio TIMESTAMP NOT NULL, " +
                    "fecha_fin TIMESTAMP NOT NULL, " +
                    "motivo TEXT)");
            s.execute("INSERT INTO Horario_bloqueado_nueva (fecha_inicio, fecha_fin, motivo) " +
                    "SELECT fecha_inicio, fecha_fin, 'Bloqueo migrado' FROM Horario_bloqueado " +
                    "WHERE fecha_inicio IS NOT NULL AND fecha_fin IS NOT NULL");
            s.execute("DROP TABLE Horario_bloqueado");
            s.execute("ALTER TABLE Horario_bloqueado_nueva RENAME TO Horario_bloqueado");
        }
    }

    private static void crearIndices(Connection con) throws SQLException {
        try (Statement s = con.createStatement()) {
            s.execute("CREATE INDEX IF NOT EXISTS idx_horario_dia_estado ON Horario(dia, estado)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_bloqueo_rango " +
                    "ON Horario_bloqueado(fecha_inicio, fecha_fin)");
        }
    }

    private static void sembrarHorarioInicial(Connection con) throws SQLException {
        try (Statement s = con.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS Version_esquema (" +
                    "clave TEXT PRIMARY KEY, valor TEXT)");
        }

        if (existeMarcador(con)) {
            return;
        }

        int total = contar(con, "Horario");
        if (total == 0) {
            insertarTramoPorDefecto(con, DiaSemana.LUNES, "09:00:00", "13:00:00");
            insertarTramoPorDefecto(con, DiaSemana.LUNES, "16:00:00", "18:00:00");
            insertarTramoPorDefecto(con, DiaSemana.MARTES, "09:00:00", "13:00:00");
            insertarTramoPorDefecto(con, DiaSemana.MARTES, "16:00:00", "18:00:00");
            insertarTramoPorDefecto(con, DiaSemana.MIERCOLES, "09:00:00", "13:00:00");
            insertarTramoPorDefecto(con, DiaSemana.MIERCOLES, "16:00:00", "18:00:00");
            insertarTramoPorDefecto(con, DiaSemana.JUEVES, "09:00:00", "13:00:00");
            insertarTramoPorDefecto(con, DiaSemana.JUEVES, "16:00:00", "18:00:00");
            insertarTramoPorDefecto(con, DiaSemana.VIERNES, "09:00:00", "13:00:00");
            insertarTramoPorDefecto(con, DiaSemana.VIERNES, "16:00:00", "18:00:00");
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT OR REPLACE INTO Version_esquema (clave, valor) VALUES (?, ?)")) {
            ps.setString(1, MARCADOR_HORARIO);
            ps.setString(2, "1");
            ps.executeUpdate();
        }
    }

    private static void insertarTramoPorDefecto(Connection con, DiaSemana dia, String inicio, String fin)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Horario (dia, hora_inicio, hora_fin, estado) VALUES (?, ?, ?, 1)")) {
            ps.setInt(1, dia.getNumero());
            ps.setString(2, inicio);
            ps.setString(3, fin);
            ps.executeUpdate();
        }
    }

    private static boolean existeMarcador(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM Version_esquema WHERE clave = ?")) {
            ps.setString(1, MARCADOR_HORARIO);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static int contar(Connection con, String tabla) throws SQLException {
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static Set<String> columnas(Connection con, String tabla) throws SQLException {
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
