package com.gestor.gestordecitasmedicas.icons;

import org.kordamp.ikonli.AbstractIkonHandler;
import org.kordamp.ikonli.Ikon;

import java.io.InputStream;
import java.net.URL;

/**
 * Manejador de Ikonli para la fuente Flaticon UIcons (regular-rounded).
 * Permite usar iconos "fi-rr-*" mediante FontIcon.
 */
public class FlaticonIconsIkonHandler extends AbstractIkonHandler {
    private static final String FONT_RESOURCE =
        "/com/gestor/gestordecitasmedicas/fonts/uicons-regular-rounded.ttf";

    @Override
    public boolean supports(String description) {
        return description != null && description.startsWith("fi-rr-");
    }

    @Override
    public Ikon resolve(String description) {
        return FlaticonIcons.findByDescription(description);
    }

    @Override
    public URL getFontResource() {
        return getClass().getResource(FONT_RESOURCE);
    }

    @Override
    public InputStream getFontResourceAsStream() {
        return getClass().getResourceAsStream(FONT_RESOURCE);
    }

    @Override
    public String getFontFamily() {
        return "uicons-regular-rounded";
    }
}
