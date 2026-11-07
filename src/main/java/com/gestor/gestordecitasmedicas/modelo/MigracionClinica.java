package com.gestor.gestordecitasmedicas.modelo;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/** Migración aditiva de los formatos ambulatorios; no modifica datos históricos. */
final class MigracionClinica {
    private MigracionClinica() { }
    static void aplicar(Connection con) throws SQLException {
        agregarColumnas(con, "Administrador", new String[]{"profesion", "especialidad", "registro_especialidad",
                "nombre_establecimiento", "domicilio_establecimiento"});
        agregarColumnas(con, "Expediente", new String[]{"tipo_establecimiento", "nombre_establecimiento", "domicilio_establecimiento", "institucion_establecimiento", "propietario", "grupo_etnico", "antecedentes_no_patologicos", "consumo_sustancias", "padecimiento_actual", "interrogatorio_sistemas", "exploracion_fisica", "resultados_estudios", "diagnostico", "pronostico", "indicacion_terapeutica", "autor_historia", "cedula_historia", "fecha_hora_historia", "signos_historia"});
        agregarColumnas(con, "Consulta", new String[]{"evolucion", "exploracion_fisica", "resultados_estudios", "diagnostico", "pronostico", "indicaciones", "nombre_medico", "cedula_medico", "nombre_paciente", "edad_paciente", "sexo_paciente"});
        agregarColumnas(con, "Receta", new String[]{"nombre_medico", "cedula_medico", "profesion", "institucion_titulo", "domicilio_medico", "especialidad", "registro_especialidad", "nombre_establecimiento", "domicilio_establecimiento", "nombre_paciente", "edad_paciente", "sexo_paciente", "indicaciones"});
        agregarColumnas(con, "Detalle_receta", new String[]{"presentacion", "via_administracion", "denominacion_distintiva", "cantidad", "grupo_regulatorio"});
        for (String tabla : new String[]{"Expediente", "Consulta", "Receta", "Detalle_receta", "Signos_vitales"}) {
            conservarVersiones(con, tabla);
        }
    }
    private static void agregarColumnas(Connection con, String tabla, String[] columnas) throws SQLException {
        Set<String> existentes = new HashSet<>();
        try (Statement s = con.createStatement(); ResultSet rs = s.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) existentes.add(rs.getString("name"));
        }
        try (Statement s = con.createStatement()) {
            for (String columna : columnas) {
                if (!existentes.contains(columna)) s.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " TEXT");
            }
        }
    }

    /** Conserva el valor anterior incluso al reemplazar detalles o ejecutar una baja en cascada. */
    private static void conservarVersiones(Connection con, String tabla) throws SQLException {
        java.util.List<String> columnas = new java.util.ArrayList<>();
        try (Statement s = con.createStatement(); ResultSet rs = s.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) columnas.add(rs.getString("name"));
        }
        String nombres = String.join(", ", columnas);
        String anteriores = columnas.stream().map(c -> "OLD." + c).collect(java.util.stream.Collectors.joining(", "));
        try (Statement s = con.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS Version_" + tabla + " AS SELECT *, '' AS archivado_en, '' AS operacion FROM " + tabla + " WHERE 0");
            for (String evento : new String[]{"UPDATE", "DELETE"}) {
                s.execute("CREATE TRIGGER IF NOT EXISTS version_" + tabla + "_" + evento
                        + " BEFORE " + evento + " ON " + tabla + " BEGIN INSERT INTO Version_" + tabla
                        + " (" + nombres + ", archivado_en, operacion) VALUES (" + anteriores
                        + ", strftime('%Y-%m-%dT%H:%M:%fZ','now'), '" + evento + "'); END");
            }
        }
    }
}
