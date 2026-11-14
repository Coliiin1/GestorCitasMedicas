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

    /** El id de acceso se genera al crear la cuenta y no cambia; solo la contraseña es editable. */
    public static boolean cambiarContrasena(String idAdministrador, String nuevaContrasena) {
        return new BaseDatos().actualizarContrasenaAdmin(idAdministrador, nuevaContrasena);
    }
}
