package com.gestor.gestordecitasmedicas.modelo;

import java.util.ArrayList;

public class Administrador extends Personal {

    private int idAdministrador;
    private String cedulaProfesional;
    private String institucionAcademica;
    private String domicilio;
    private ArrayList<Asistente> asistentes = new ArrayList<>();
    private ArrayList<Expediente> expedientes = new ArrayList<>();

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

    public ArrayList<Asistente> getAsistentes() {
        return asistentes;
    }

    public void setAsistentes(ArrayList<Asistente> asistentes) {
        this.asistentes = asistentes;
    }

    public ArrayList<Expediente> getExpedientes() {
        return expedientes;
    }

    public void setExpedientes(ArrayList<Expediente> expedientes) {
        this.expedientes = expedientes;
    }

    public boolean registrarAsistente(Asistente asistente) {
        return asistentes.add(asistente);
    }

    public boolean eliminarAsistente(Asistente asistente) {
        return asistentes.remove(asistente);
    }

    public boolean editarAsistente(Asistente asistente) {
        return false;
    }

    public Estado iniciarCita(Cita cita) {
        return null;
    }

    public boolean generarReceta(Cita cita) {
        return false;
    }

    public boolean registrarExpediente() {
        return false;
    }

    public boolean actualizarExpediente() {
        return false;
    }

    public boolean eliminarExpediente() {
        return false;
    }

    public boolean registrarPaciente() {
        return false;
    }

    public boolean actualizarPaciente() {
        return false;
    }

    public boolean eliminarPaciente() {
        return false;
    }

    public Paciente consultarDatosPaciente() {
        return null;
    }

    @Override
    public Cita[] listadoSemanal() {
        return new Cita[0];
    }

    @Override
    public boolean cancelarCita() {
        return false;
    }

    @Override
    public boolean agendarCita() {
        return false;
    }

    @Override
    public boolean reprogramarCita() {
        return false;
    }

    @Override
    public boolean validarHorario() {
        return false;
    }
}
