package com.gestor.gestordecitasmedicas.modelo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;

public class BaseDatos {
    private String contraseña;
    private static final String URL = "jdbc:sqlite:citas_medicas.db";
    private static Connection conexion = null;

    public static Connection getConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                // 1. Forzar la carga del driver explícitamente
                Class.forName("org.sqlite.JDBC");

                // 2. Establecer la conexión
                conexion = DriverManager.getConnection(URL);
                System.out.println("Conexión a la base de datos SQLite establecida con éxito.");

                // Habilitar llaves foráneas
                try (Statement stmt = conexion.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }
            }
            // 3. Capturar también ClassNotFoundException
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error de conexión a la base de datos: " + e.getMessage());
        }
        return conexion;
    }

    // Método para cerrar la conexión al salir del sistema
    public static void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                System.out.println("Conexión cerrada de forma segura.");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }
    // Métodos para gestionar asistentes
    public boolean insertarAsistente(Asistente asistente) {
        return false;
    }

    public boolean eliminarAsistente(int idAsistente) {
        return false;
    }

    public boolean actualizarAsistente(Asistente asistente) {
        return false;
    }

    public ArrayList<Asistente> cargarAsistentes() {
        return new ArrayList<>();
    }

    // Métodos para gestionar pacientes
    public boolean insertarPaciente(Paciente paciente) {
        return false;
    }

    public boolean eliminarPaciente(String idPaciente) {
        return false;
    }

    public boolean actualizarPaciente(Paciente paciente) {
        return false;
    }

    public HashMap<String, Paciente> cargarPacientes() {
        return new HashMap<>();
    }

    // Métodos para gestionar citas
    public boolean insertarCita(Cita cita) {
        return false;
    }

    public boolean eliminarCita(int idCita) {
        return false;
    }

    public boolean actualizarCita(Cita cita) {
        return false;
    }

    public HashMap<Integer, Cita> cargarCitas() {
        return new HashMap<>();
    }

    // Métodos para gestionar expedientes
    public boolean insertarExpediente(Expediente expediente) {
        return false;
    }

    public boolean eliminarExpediente(String idExpediente) {
        return false;
    }

    public boolean actualizarExpediente(Expediente expediente) {
        return false;
    }

    // Métodos para gestionar recetas
    public boolean insertarReceta(Receta receta) {
        return false;
    }

    public boolean eliminarReceta(int idReceta) {
        return false;
    }

    public HashMap<Integer, Receta> cargarRecetas() {
        return new HashMap<>();
    }

    // Métodos para gestionar medicamentos
    public boolean insertarMedicamento(Medicamento medicamento) {
        return false;
    }

    public ArrayList<Medicamento> cargarMedicamentos() {
        return new ArrayList<>();
    }

    // Método para convertir horas
    public void convertirHoras(LocalTime hora) {
        // Implementar la conversión necesaria
    }
}
