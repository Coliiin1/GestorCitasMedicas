package com.gestor.gestordecitasmedicas.modelo;

public class Administrador extends Personal {

    private int idAdministrador;
    private String cedulaProfesional;
    private String institucionAcademica;
    private String domicilio;
    private String profesion;
    private String especialidad;
    private String registroEspecialidad;
    private String nombreEstablecimiento;
    private String domicilioEstablecimiento;

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

    public int getIdAdministrador() {
        return idAdministrador;
    }

    public void setIdAdministrador(int idAdministrador) {
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

    public static Administrador buscar(int idAdministrador) {
        return new BaseDatos().obtenerAdministrador(idAdministrador);
    }

    /** Guarda los datos personales y del consultorio. */
    public boolean actualizar() {
        return new BaseDatos().actualizarAdministrador(this);
    }

    /** true si {@code nuevoId} no lo usa otra cuenta distinta de {@code idActual}. */
    public static boolean usuarioDisponible(int nuevoId, int idActual) {
        return new BaseDatos().idUsuarioLibre(nuevoId, idActual);
    }

    /** Cambia el usuario (id) y, si {@code nuevaContrasena} no está vacía, la contraseña. */
    public static boolean cambiarCredenciales(int idActual, int nuevoId, String nuevaContrasena) {
        return new BaseDatos().actualizarCredencialesAdmin(idActual, nuevoId, nuevaContrasena);
    }
}
