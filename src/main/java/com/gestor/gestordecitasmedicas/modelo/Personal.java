package com.gestor.gestordecitasmedicas.modelo;

/** Personal del consultorio (administrador o asistente) que inicia sesión en el sistema. */
public abstract class Personal extends Usuario {

    /** Valida usuario y contraseña; devuelve el rol ("admin" o "asis") o null si no coinciden. */
    public static String autenticar(String idUsuario, String contrasena) {
        return new BaseDatos().autenticar(idUsuario, contrasena);
    }

    /**
     * Id de acceso de 10 caracteres: 4 letras del apellido paterno, 2 del materno y 2 del nombre
     * (mayúsculas sin acentos, con X de relleno) más el primer consecutivo que no esté en {@code ocupados}.
     * Devuelve null si los 99 consecutivos están tomados.
     */
    public static String generarUsuario(String nombre, String apellidoPaterno, String apellidoMaterno,
                                        java.util.Collection<String> ocupados) {
        String base = letras(apellidoPaterno, 4) + letras(apellidoMaterno, 2) + letras(nombre, 2);
        for (int n = 1; n <= 99; n++) {
            String id = base + String.format("%02d", n);
            if (!ocupados.contains(id)) {
                return id;
            }
        }
        return null;
    }

    private static String letras(String texto, int cuantas) {
        String limpio = java.text.Normalizer.normalize(texto == null ? "" : texto, java.text.Normalizer.Form.NFD)
                .toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z]", "");
        return (limpio + "XXXX").substring(0, cuantas);
    }
}
