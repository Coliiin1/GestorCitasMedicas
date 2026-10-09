package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.Paciente;

import java.util.HashMap;

public class Main {
    //Los warnings que  se  producen son debido a que se cargan las librerias de JavaFx pero NO  se usan, no es ningun error :D
    public HashMap<String, Paciente> pacientes = new HashMap<>();
    public HashMap<Integer, Cita> citas = new HashMap<>();

    public static void main(String[] args) {
    }
}
