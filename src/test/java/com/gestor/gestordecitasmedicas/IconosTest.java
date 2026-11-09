package com.gestor.gestordecitasmedicas;

import com.gestor.gestordecitasmedicas.icons.FlaticonIcons;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Todo literal "fi-rr-*" usado en fxml o código debe existir en FlaticonIcons; si no, el icono no se dibuja. */
class IconosTest {
    private static final Pattern LITERAL = Pattern.compile("\"(fi-rr-[a-z0-9-]+)\"");

    @Test
    void todosLosLiteralesEstanEnElEnum() throws IOException {
        List<String> faltantes = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(Paths.get("src", "main"))) {
            for (Path archivo : (Iterable<Path>) archivos.filter(p -> p.toString().endsWith(".fxml")
                    || (p.toString().endsWith(".java") && !p.endsWith("FlaticonIcons.java")))::iterator) {
                Matcher m = LITERAL.matcher(new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8));
                while (m.find()) {
                    try {
                        FlaticonIcons.findByDescription(m.group(1));
                    } catch (IllegalArgumentException ex) {
                        faltantes.add(m.group(1) + " en " + archivo);
                    }
                }
            }
        }
        assertTrue(faltantes.isEmpty(), "Iconos fuera de FlaticonIcons: " + faltantes);
    }

    @Test
    void laFuenteYElServicioEstanEnElClasspath() {
        assertNotNull(getClass().getResource("/com/gestor/gestordecitasmedicas/fonts/uicons-regular-rounded.ttf"));
        assertNotNull(getClass().getResource("/META-INF/services/org.kordamp.ikonli.IkonHandler"));
    }
}
