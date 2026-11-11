package com.gestor.gestordecitasmedicas.modelo;

/** Personal del consultorio (administrador o asistente) que inicia sesión en el sistema. */
public abstract class Personal extends Usuario {

    /** Valida usuario y contraseña; devuelve el rol ("admin" o "asis") o null si no coinciden. */
    public static String autenticar(int idUsuario, String contrasena) {
        return new BaseDatos().autenticar(idUsuario, contrasena);
    }
}
