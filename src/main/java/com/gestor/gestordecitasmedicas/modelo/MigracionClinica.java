package com.gestor.gestordecitasmedicas.modelo;

import java.sql.*;
import java.util.List;

/** Historial de cambios de los registros clínicos: cada edición o baja archiva la versión anterior. */
final class MigracionClinica {
    private MigracionClinica() { }
    static void aplicar(Connection con) throws SQLException {
        for (String tabla : new String[]{"Expediente", "Consulta", "Receta", "Detalle_receta", "Signos_vitales"}) {
            conservarVersiones(con, tabla);
        }
    }

    /** Conserva el valor anterior incluso al reemplazar detalles o ejecutar una baja en cascada. */
    private static void conservarVersiones(Connection con, String tabla) throws SQLException {
        List<String> columnas = MigracionAgenda.columnas(con, tabla);
        String nombres = String.join(", ", columnas);
        String anteriores = columnas.stream().map(c -> "OLD." + c).collect(java.util.stream.Collectors.joining(", "));
        // Guardar sin cambios no debe llenar el historial de copias idénticas.
        String cambio = columnas.stream().map(c -> "OLD." + c + " IS NOT NEW." + c)
                .collect(java.util.stream.Collectors.joining(" OR "));
        try (Statement s = con.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS Version_" + tabla + " AS SELECT *, '' AS archivado_en, '' AS operacion FROM " + tabla + " WHERE 0");
            for (String evento : new String[]{"UPDATE", "DELETE"}) {
                s.execute("CREATE TRIGGER IF NOT EXISTS version_" + tabla + "_" + evento
                        + " BEFORE " + evento + " ON " + tabla
                        + ("UPDATE".equals(evento) ? " WHEN " + cambio : "")
                        + " BEGIN INSERT INTO Version_" + tabla
                        + " (" + nombres + ", archivado_en, operacion) VALUES (" + anteriores
                        + ", strftime('%Y-%m-%dT%H:%M:%fZ','now'), '" + evento + "'); END");
            }
        }
    }
}
