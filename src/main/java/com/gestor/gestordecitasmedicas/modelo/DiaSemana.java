package com.gestor.gestordecitasmedicas.modelo;

public enum DiaSemana {
    LUNES,
    MARTES,
    MIERCOLES,
    JUEVES,
    VIERNES,
    SABADO,
    DOMINGO;

    public int getNumero() {
        return ordinal() + 1;
    }

    public static DiaSemana desdeNumero(int numero) {
        if (numero < 1 || numero > values().length) {
            return null;
        }
        return values()[numero - 1];
    }

    public static DiaSemana desdeDayOfWeek(java.time.DayOfWeek dayOfWeek) {
        return dayOfWeek == null ? null : desdeNumero(dayOfWeek.getValue());
    }

    public String getEtiqueta() {
        switch (this) {
            case LUNES: return "Lunes";
            case MARTES: return "Martes";
            case MIERCOLES: return "Miércoles";
            case JUEVES: return "Jueves";
            case VIERNES: return "Viernes";
            case SABADO: return "Sábado";
            default: return "Domingo";
        }
    }

    public String getEtiquetaCorta() {
        switch (this) {
            case LUNES: return "Lun";
            case MARTES: return "Mar";
            case MIERCOLES: return "Mié";
            case JUEVES: return "Jue";
            case VIERNES: return "Vie";
            case SABADO: return "Sáb";
            default: return "Dom";
        }
    }
}
