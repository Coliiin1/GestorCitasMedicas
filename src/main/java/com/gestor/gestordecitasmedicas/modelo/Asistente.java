package com.gestor.gestordecitasmedicas.modelo;

import java.util.ArrayList;

public class Asistente extends Personal {

    private String idAsistente;
    private String contrasena;

    public Asistente() {
    }

    public Asistente(String idAsistente) {
        this.idAsistente = idAsistente;
    }

    public Asistente(String nombre, String apellidoPaterno, String apellidoMaterno, String contrasena) {
        setNombre(nombre);
        setApellidoPaterno(apellidoPaterno);
        setApellidoMaterno(apellidoMaterno);
        this.contrasena = contrasena;
    }

    public String getIdAsistente() {
        return idAsistente;
    }

    public void setIdAsistente(String idAsistente) {
        this.idAsistente = idAsistente;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    /**
     * Reglas de captura del asistente; devuelve el mensaje del primer error o null si es válido.
     * La contraseña solo es obligatoria al registrar (al editar, vacía conserva la actual).
     */
    public static String validar(String nombre, String apellidoPaterno, String apellidoMaterno,
                                 String contrasena, boolean esNuevo) {
        if (nombre.isEmpty() || apellidoPaterno.isEmpty()) {
            return "El nombre y el apellido paterno son obligatorios.";
        }
        if (nombre.length() > 20 || apellidoPaterno.length() > 20 || apellidoMaterno.length() > 20) {
            return "El nombre y los apellidos no deben exceder 20 caracteres.";
        }
        if (contrasena.isEmpty() && esNuevo) {
            return "La contraseña es obligatoria para registrar un asistente.";
        }
        return null;
    }

    // ---- Lógica de negocio: delegan la persistencia a BaseDatos ----

    public boolean registrar() {
        return new BaseDatos().insertarAsistente(this);
    }

    public boolean actualizar() {
        return new BaseDatos().actualizarAsistente(this);
    }

    public boolean eliminar() {
        return new BaseDatos().eliminarAsistente(idAsistente);
    }

    public static ArrayList<Asistente> todos() {
        return new BaseDatos().cargarAsistentes();
    }
}
