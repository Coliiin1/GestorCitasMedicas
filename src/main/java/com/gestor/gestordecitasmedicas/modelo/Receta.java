package com.gestor.gestordecitasmedicas.modelo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Receta {

    private int idReceta;
    private int idCita;
    private String idAdministrador;
    private String curp;
    private Date fecha;
    private String diagnostico;
    private ArrayList<Medicamento> medicamentos = new ArrayList<>();
    private SignosVitales signosVitales;

    public int getIdReceta() {
        return idReceta;
    }

    public void setIdReceta(int idReceta) {
        this.idReceta = idReceta;
    }

    public int getIdCita() {
        return idCita;
    }

    public void setIdCita(int idCita) {
        this.idCita = idCita;
    }

    public String getIdAdministrador() {
        return idAdministrador;
    }

    public void setIdAdministrador(String idAdministrador) {
        this.idAdministrador = idAdministrador;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getCurp() {
        return curp;
    }

    public void setCurp(String curp) {
        this.curp = curp;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public ArrayList<Medicamento> getMedicamentos() {
        return medicamentos;
    }

    public void setMedicamentos(ArrayList<Medicamento> medicamentos) {
        this.medicamentos = medicamentos;
    }

    public SignosVitales getSignosVitales() {
        return signosVitales;
    }

    public void setSignosVitales(SignosVitales signosVitales) {
        this.signosVitales = signosVitales;
    }

    private String nombreMedico;
    public String getNombreMedico() { return nombreMedico; }
    public void setNombreMedico(String valor) { this.nombreMedico = valor; }

    private String cedulaMedico;
    public String getCedulaMedico() { return cedulaMedico; }
    public void setCedulaMedico(String valor) { this.cedulaMedico = valor; }

    private String profesion;
    public String getProfesion() { return profesion; }
    public void setProfesion(String valor) { this.profesion = valor; }

    private String institucionTitulo;
    public String getInstitucionTitulo() { return institucionTitulo; }
    public void setInstitucionTitulo(String valor) { this.institucionTitulo = valor; }

    private String domicilioMedico;
    public String getDomicilioMedico() { return domicilioMedico; }
    public void setDomicilioMedico(String valor) { this.domicilioMedico = valor; }

    private String especialidad;
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String valor) { this.especialidad = valor; }

    private String registroEspecialidad;
    public String getRegistroEspecialidad() { return registroEspecialidad; }
    public void setRegistroEspecialidad(String valor) { this.registroEspecialidad = valor; }

    private String nombreEstablecimiento;
    public String getNombreEstablecimiento() { return nombreEstablecimiento; }
    public void setNombreEstablecimiento(String valor) { this.nombreEstablecimiento = valor; }

    private String domicilioEstablecimiento;
    public String getDomicilioEstablecimiento() { return domicilioEstablecimiento; }
    public void setDomicilioEstablecimiento(String valor) { this.domicilioEstablecimiento = valor; }

    private String nombrePaciente;
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String valor) { this.nombrePaciente = valor; }

    private String edadPaciente;
    public String getEdadPaciente() { return edadPaciente; }
    public void setEdadPaciente(String valor) { this.edadPaciente = valor; }

    private String sexoPaciente;
    public String getSexoPaciente() { return sexoPaciente; }
    public void setSexoPaciente(String valor) { this.sexoPaciente = valor; }

    private String indicaciones;
    public String getIndicaciones() { return indicaciones; }
    public void setIndicaciones(String valor) { this.indicaciones = valor; }

    // ---- Lógica de negocio: delegan la persistencia a BaseDatos ----

    /** Receta más reciente de la cita con medicamentos y signos vitales, o null si no tiene. */
    public static Receta deCita(int idCita) {
        return new BaseDatos().cargarRecetaPorCita(idCita);
    }

    /** Todas las recetas, indexadas por id. */
    public static HashMap<Integer, Receta> todas() {
        return new BaseDatos().cargarRecetas();
    }

    /** Copia en la receta los datos del médico prescriptor (cédula, especialidad, establecimiento...). */
    public void cargarDatosPrescriptor() {
        new BaseDatos().cargarDatosPrescriptor(this);
    }
}
