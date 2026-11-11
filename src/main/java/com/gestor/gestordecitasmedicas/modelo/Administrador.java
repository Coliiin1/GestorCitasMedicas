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
}
