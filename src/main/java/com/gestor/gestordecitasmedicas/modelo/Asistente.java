package com.gestor.gestordecitasmedicas.modelo;

public class Asistente extends Personal {

    private int idAsistente;
    private String contrasena;

    public Asistente() {
    }

    public Asistente(int idAsistente) {
        this.idAsistente = idAsistente;
    }

    public Asistente(String nombre, String apellidoPaterno, String apellidoMaterno, String contrasena) {
        setNombre(nombre);
        setApellidoPaterno(apellidoPaterno);
        setApellidoMaterno(apellidoMaterno);
        this.contrasena = contrasena;
    }

    public int getIdAsistente() {
        return idAsistente;
    }

    public void setIdAsistente(int idAsistente) {
        this.idAsistente = idAsistente;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
