package com.gestor.gestordecitasmedicas.modelo;

public class Administrador extends Personal {

    private String idAdministrador;
    private String cedulaProfesional;
    private String institucionAcademica;
    private String domicilio;
    private String profesion;
    private String especialidad;
    private String registroEspecialidad;
    private String nombreEstablecimiento;
    private String domicilioEstablecimiento;
    private String contrasena;

    public String getContrasena() { return contrasena; }
    public void setContrasena(String v) { this.contrasena = v; }

    public String getProfesion() { return profesion; }
    public void setProfesion(String v) { this.profesion = v; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String v) { this.especialidad = v; }
    public String getRegistroEspecialidad() { return registroEspecialidad; }
    public void setRegistroEspecialidad(String v) { this.registroEspecialidad = v; }
    public String getNombreEstablecimiento() { return nombreEstablecimiento; }
    public void setNombreEstablecimiento(String v) { this.nombreEstablecimiento = v; }
    public String getDomicilioEstablecimiento() { return domicilioEstablecimiento; }
    public void setDomicilioEstablecimiento(String v) { this.domicilioEstablecimiento = v; }

    public String getIdAdministrador() {
        return idAdministrador;
    }

    public void setIdAdministrador(String idAdministrador) {
        this.idAdministrador = idAdministrador;
    }

    public String getCedulaProfesional() {
        return cedulaProfesional;
    }

    public void setCedulaProfesional(String cedulaProfesional) {
        this.cedulaProfesional = cedulaProfesional;
    }

    public String getInstitucionAcademica() {
        return institucionAcademica;
    }

    public void setInstitucionAcademica(String institucionAcademica) {
        this.institucionAcademica = institucionAcademica;
    }

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }

    // ---- Lógica de negocio: delegan la persistencia a BaseDatos ----

    /** Administrador con ese id de acceso; {@code null} busca el primero (consultorio de un solo médico). */
    public static Administrador buscar(String idAdministrador) {
        return new BaseDatos().obtenerAdministrador(idAdministrador);
    }

    /** Guarda los datos personales y del consultorio. */
    public boolean actualizar() {
        return new BaseDatos().actualizarAdministrador(this);
    }

    /** Crea la cuenta (el id de acceso se genera aquí); valida como {@link Asistente#validar}. */
    public boolean registrar() {
        String n = getNombre() == null ? "" : getNombre().trim();
        String ap = getApellidoPaterno() == null ? "" : getApellidoPaterno().trim();
        String am = getApellidoMaterno() == null ? "" : getApellidoMaterno().trim();
        if (n.isEmpty() || ap.isEmpty() || n.length() > 20 || ap.length() > 20 || am.length() > 20
                || contrasena == null || contrasena.isEmpty()) {
            return false;
        }
        setNombre(n);
        setApellidoPaterno(ap);
        setApellidoMaterno(am);
        return new BaseDatos().insertarAdministrador(this);
    }

    /**
     * Crea el administrador con el archivo que deja el instalador (líneas UTF-8: nombre, apellido paterno,
     * apellido materno, contraseña) y lo borra. Devuelve el id generado, o null si ya había administrador
     * o no hay archivo. Si los datos no sirven, conserva el archivo y lanza IllegalStateException.
     */
    public static String crearDesdeInstalador(java.nio.file.Path archivo) throws java.io.IOException {
        if (!java.nio.file.Files.exists(archivo)) return null;
        if (buscar(null) != null) {
            java.nio.file.Files.delete(archivo);
            return null;
        }
        java.util.List<String> l = java.nio.file.Files.readAllLines(archivo, java.nio.charset.StandardCharsets.UTF_8);
        if (!l.isEmpty() && l.get(0).startsWith("﻿")) l.set(0, l.get(0).substring(1));
        Administrador a = new Administrador();
        if (l.size() >= 4) {
            a.setNombre(l.get(0));
            a.setApellidoPaterno(l.get(1));
            a.setApellidoMaterno(l.get(2));
            a.setContrasena(l.get(3));
        }
        if (!a.registrar()) {
            throw new IllegalStateException("No se pudo crear el administrador con los datos del instalador.");
        }
        java.nio.file.Files.delete(archivo);
        return a.getIdAdministrador();
    }

    /** El id de acceso se genera al crear la cuenta y no cambia; solo la contraseña es editable. */
    public static boolean cambiarContrasena(String idAdministrador, String nuevaContrasena) {
        return new BaseDatos().actualizarContrasenaAdmin(idAdministrador, nuevaContrasena);
    }
}
