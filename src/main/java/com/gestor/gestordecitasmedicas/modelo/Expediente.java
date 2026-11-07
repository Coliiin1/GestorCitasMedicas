package com.gestor.gestordecitasmedicas.modelo;

import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;

public class Expediente {

    private int idExpediente;
    private Paciente datosPaciente;
    private Date fechaApertura;
    private String antecedentesFamiliares;
    private String antecedentesPersonales;
    private String alergias;
    private String observaciones;
    private HashMap<Integer, Receta> recetas = new HashMap<>();
    private ArrayList<Consulta> consultas = new ArrayList<>();

    public ArrayList<Consulta> getConsultas() {
        return consultas;
    }

    public void setConsultas(ArrayList<Consulta> consultas) {
        this.consultas = consultas;
    }

    public int getIdExpediente() {
        return idExpediente;
    }

    public void setIdExpediente(int idExpediente) {
        this.idExpediente = idExpediente;
    }

    public Date getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(Date fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public String getAntecedentesFamiliares() {
        return antecedentesFamiliares;
    }

    public void setAntecedentesFamiliares(String antecedentesFamiliares) {
        this.antecedentesFamiliares = antecedentesFamiliares;
    }

    public String getAntecedentesPersonales() {
        return antecedentesPersonales;
    }

    public void setAntecedentesPersonales(String antecedentesPersonales) {
        this.antecedentesPersonales = antecedentesPersonales;
    }

    public String getAlergias() {
        return alergias;
    }

    public void setAlergias(String alergias) {
        this.alergias = alergias;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Paciente getDatosPaciente() {
        return datosPaciente;
    }

    public void setDatosPaciente(Paciente datosPaciente) {
        this.datosPaciente = datosPaciente;
    }

    public HashMap<Integer, Receta> getRecetas() {
        return recetas;
    }

    public void setRecetas(HashMap<Integer, Receta> recetas) {
        this.recetas = recetas;
    }

    private String tipoEstablecimiento;
    public String getTipoEstablecimiento() { return tipoEstablecimiento; }
    public void setTipoEstablecimiento(String valor) { this.tipoEstablecimiento = valor; }

    private String nombreEstablecimiento;
    public String getNombreEstablecimiento() { return nombreEstablecimiento; }
    public void setNombreEstablecimiento(String valor) { this.nombreEstablecimiento = valor; }

    private String domicilioEstablecimiento;
    public String getDomicilioEstablecimiento() { return domicilioEstablecimiento; }
    public void setDomicilioEstablecimiento(String valor) { this.domicilioEstablecimiento = valor; }

    private String institucionEstablecimiento;
    public String getInstitucionEstablecimiento() { return institucionEstablecimiento; }
    public void setInstitucionEstablecimiento(String valor) { this.institucionEstablecimiento = valor; }

    private String propietario;
    public String getPropietario() { return propietario; }
    public void setPropietario(String valor) { this.propietario = valor; }

    private String grupoEtnico;
    public String getGrupoEtnico() { return grupoEtnico; }
    public void setGrupoEtnico(String valor) { this.grupoEtnico = valor; }

    private String antecedentesNoPatologicos;
    public String getAntecedentesNoPatologicos() { return antecedentesNoPatologicos; }
    public void setAntecedentesNoPatologicos(String valor) { this.antecedentesNoPatologicos = valor; }

    private String consumoSustancias;
    public String getConsumoSustancias() { return consumoSustancias; }
    public void setConsumoSustancias(String valor) { this.consumoSustancias = valor; }

    private String padecimientoActual;
    public String getPadecimientoActual() { return padecimientoActual; }
    public void setPadecimientoActual(String valor) { this.padecimientoActual = valor; }

    private String interrogatorioSistemas;
    public String getInterrogatorioSistemas() { return interrogatorioSistemas; }
    public void setInterrogatorioSistemas(String valor) { this.interrogatorioSistemas = valor; }

    private String exploracionFisica;
    public String getExploracionFisica() { return exploracionFisica; }
    public void setExploracionFisica(String valor) { this.exploracionFisica = valor; }

    private String resultadosEstudios;
    public String getResultadosEstudios() { return resultadosEstudios; }
    public void setResultadosEstudios(String valor) { this.resultadosEstudios = valor; }

    private String diagnostico;
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String valor) { this.diagnostico = valor; }

    private String pronostico;
    public String getPronostico() { return pronostico; }
    public void setPronostico(String valor) { this.pronostico = valor; }

    private String indicacionTerapeutica;
    public String getIndicacionTerapeutica() { return indicacionTerapeutica; }
    public void setIndicacionTerapeutica(String valor) { this.indicacionTerapeutica = valor; }

    private String autorHistoria;
    public String getAutorHistoria() { return autorHistoria; }
    public void setAutorHistoria(String valor) { this.autorHistoria = valor; }

    private String cedulaHistoria;
    public String getCedulaHistoria() { return cedulaHistoria; }
    public void setCedulaHistoria(String valor) { this.cedulaHistoria = valor; }

    private String fechaHoraHistoria;
    public String getFechaHoraHistoria() { return fechaHoraHistoria; }
    public void setFechaHoraHistoria(String valor) { this.fechaHoraHistoria = valor; }
    private String signosHistoria;
    public String getSignosHistoria() { return signosHistoria; }
    public void setSignosHistoria(String valor) { this.signosHistoria = valor; }
}
