package com.gestor.gestordecitasmedicas.modelo;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;

public class BaseDatos {
    private String URL;
    private String contraseña;
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
