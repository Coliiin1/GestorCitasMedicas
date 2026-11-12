package com.gestor.gestordecitasmedicas.controlador;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificadorTest {

    @AfterEach
    void limpiar() {
        Notificador.limpiar();
    }

    @Test
    void historialGuardaLoMasRecientePrimeroConTopeYCuentaNoLeidas() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 11, 9, 0);
        for (int i = 0; i < Notificador.MAX_HISTORIAL + 5; i++) {
            Notificador.registrar(Notificador.Tipo.EXITO, "Aviso " + i, null, t.plusMinutes(i));
        }
        assertEquals(Notificador.MAX_HISTORIAL, Notificador.historial.size());
        assertEquals("Aviso " + (Notificador.MAX_HISTORIAL + 4), Notificador.historial.get(0).titulo);
        assertEquals("", Notificador.historial.get(0).mensaje);
        assertEquals(Notificador.MAX_HISTORIAL, Notificador.noLeidas.get());

        Notificador.marcarTodoLeido();
        assertEquals(0, Notificador.noLeidas.get());
    }

    @Test
    void horaRelativa() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 11, 10, 30);
        assertEquals("Ahora", Notificador.horaRelativa(ahora.minusSeconds(20), ahora));
        assertEquals("Hace 15 min", Notificador.horaRelativa(ahora.minusMinutes(15), ahora));
        assertEquals("8:05", Notificador.horaRelativa(LocalDateTime.of(2026, 11, 11, 8, 5), ahora));
        assertTrue(Notificador.horaRelativa(LocalDateTime.of(2026, 11, 10, 18, 0), ahora).startsWith("mar"));
    }

    @Test
    void tonoDuraUnPulsoPorRepeticionYNoSatura() {
        byte[] uno = Notificador.Sonido.tono(1);
        byte[] dos = Notificador.Sonido.tono(2);
        assertEquals(uno.length * 2, dos.length);
        int max = 0;
        for (int i = 0; i < uno.length; i += 2) {
            max = Math.max(max, Math.abs((short) ((uno[i] & 0xFF) | (uno[i + 1] << 8))));
        }
        assertTrue(max > 0 && max < Short.MAX_VALUE * 0.4, "pico " + max);
    }
}
