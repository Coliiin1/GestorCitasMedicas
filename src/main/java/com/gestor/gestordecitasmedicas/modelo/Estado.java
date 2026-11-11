package com.gestor.gestordecitasmedicas.modelo;

public enum Estado {
    AGENDADA,
    ATENDIDA,
    CANCELADA;

    /** Texto para mostrar; una cita sin estado se considera agendada. */
    public static String etiqueta(Estado estado) {
        if (estado == ATENDIDA) return "Atendida";
        if (estado == CANCELADA) return "Cancelada";
        return "Agendada";
    }
}
