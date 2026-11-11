package com.gestor.gestordecitasmedicas.modelo;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;

public class Paciente extends Usuario {

    private String curp;
    private Date fechaNacimiento;
    private String sexo;
    private String domicilio;
    private String telefono;
    private String correo;

    public Paciente(String curp, Date fechaNacimiento, String sexo, String domicilio, String telefono, String correo) {
        this.curp = curp;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.domicilio = domicilio;
        this.telefono = telefono;
        this.correo = correo;
    }

    public String getCurp() {
        return curp;
    }

    public void setCurp(String curp) {
        this.curp = curp;
    }

    public Date getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(Date fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    /** Nombre completo para mostrar; si no hay paciente o no tiene nombre, usa la CURP. */
    public static String nombreParaMostrar(Paciente paciente, String curp) {
        if (paciente == null) {
            return curp == null ? "Paciente no registrado" : curp;
        }
        String nombre = (limpio(paciente.getNombre()) + " " + limpio(paciente.getApellidoPaterno())
                + " " + limpio(paciente.getApellidoMaterno())).trim();
        return nombre.isEmpty() ? (curp == null ? "Paciente sin nombre" : curp) : nombre;
    }

    /** Edad en años cumplidos, o "--" si no hay paciente o fecha de nacimiento. */
    public static String edadTexto(Paciente paciente) {
        if (paciente == null || paciente.getFechaNacimiento() == null) {
            return "--";
        }
        LocalDate nacimiento = paciente.getFechaNacimiento().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return String.valueOf(Period.between(nacimiento, LocalDate.now()).getYears());
    }

    private static String limpio(String texto) {
        return texto == null ? "" : texto.trim();
    }

    /** Reglas de captura del paciente; devuelve el mensaje del primer error o null si los datos son válidos. */
    public static String validar(String nombre, String apellidoPaterno, String apellidoMaterno,
                                  String curp, LocalDate fechaNacimiento, String telefono,
                                  String correo, String domicilio) {
        if (nombre.isEmpty() || apellidoPaterno.isEmpty()) {
            return "El nombre y el apellido paterno son obligatorios.";
        }
        if (nombre.length() > 20 || apellidoPaterno.length() > 20 || apellidoMaterno.length() > 20) {
            return "El nombre y los apellidos no deben exceder 20 caracteres.";
        }
        if (!curp.matches("[A-Z0-9]{18}")) {
            return "La CURP debe tener 18 caracteres alfanuméricos.";
        }
        if (fechaNacimiento == null) {
            return "La fecha de nacimiento es obligatoria.";
        }
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            return "La fecha de nacimiento no puede ser futura.";
        }
        if (!telefono.isEmpty() && !telefono.matches("\\d{10}")) {
            return "El teléfono debe contener 10 dígitos.";
        }
        if (!correo.isEmpty() && !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "El correo electrónico no tiene un formato válido.";
        }
        if (correo.length() > 50) {
            return "El correo electrónico no debe exceder 50 caracteres.";
        }
        if (domicilio.length() > 100) {
            return "El domicilio no debe exceder 100 caracteres.";
        }
        return null;
    }

    // ---- Lógica de negocio: delegan la persistencia a BaseDatos ----

    /** Registra un paciente nuevo; falla si la CURP ya existe. */
    public boolean registrar() {
        return new BaseDatos().insertarPaciente(this);
    }

    public boolean actualizar() {
        return new BaseDatos().actualizarPaciente(this);
    }

    public boolean eliminar() {
        return new BaseDatos().eliminarPaciente(curp);
    }

    /** Todos los pacientes, indexados por CURP. */
    public static HashMap<String, Paciente> todos() {
        return new BaseDatos().cargarPacientes();
    }

    public static Paciente buscar(String curp) {
        return new BaseDatos().cargarPacientePorCurp(curp);
    }
}
