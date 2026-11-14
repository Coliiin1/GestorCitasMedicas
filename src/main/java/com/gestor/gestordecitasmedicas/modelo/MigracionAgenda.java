package com.gestor.gestordecitasmedicas.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Siembra el horario semanal inicial la primera vez que se crea la base. */
final class MigracionAgenda {
    private static final String MARCADOR_HORARIO = "esquema.horario_inicial";

    private MigracionAgenda() {
    }

    static void aplicar(Connection con) throws SQLException {
        // El marcador evita resembrar si después se borran todos los tramos a propósito.
        if (existeMarcador(con)) {
            return;
        }

        if (contar(con, "Horario") == 0) {
            for (DiaSemana dia : new DiaSemana[]{DiaSemana.LUNES, DiaSemana.MARTES, DiaSemana.MIERCOLES,
                    DiaSemana.JUEVES, DiaSemana.VIERNES}) {
                insertarTramoPorDefecto(con, dia, "09:00:00", "13:00:00");
                insertarTramoPorDefecto(con, dia, "16:00:00", "18:00:00");
            }
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT OR REPLACE INTO Configuracion (clave, valor) VALUES (?, ?)")) {
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
                "SELECT COUNT(*) FROM Configuracion WHERE clave = ?")) {
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

    /** Nombres de las columnas de la tabla, en el orden en que están definidas. */
    static List<String> columnas(Connection con, String tabla) throws SQLException {
        List<String> columnas = new ArrayList<>();
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                columnas.add(rs.getString("name"));
            }
        }
        return columnas;
    }
}
