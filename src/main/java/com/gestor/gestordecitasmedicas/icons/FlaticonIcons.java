package com.gestor.gestordecitasmedicas.icons;

import org.kordamp.ikonli.Ikon;

/**
 * Iconos de Flaticon UIcons (estilo regular-rounded) usados en la interfaz.
 * Los códigos corresponden a los codepoints de la fuente uicons-regular-rounded.ttf.
 */
public enum FlaticonIcons implements Ikon {
    HOME("fi-rr-home", 0xE866),
    CLOCK("fi-rr-clock", 0xE3EA),
    CALENDAR("fi-rr-calendar", 0xE2A1),
    USERS("fi-rr-users", 0xF0DF),
    NOTES_MEDICAL("fi-rr-notes-medical", 0xEB00),
    USER_MD("fi-rr-user-md", 0xF0C2),
    PAUSE("fi-rr-pause", 0xEB6A),
    CROSS("fi-rr-cross", 0xE4CD),
    FOLDER_OPEN("fi-rr-folder-open", 0xE728),
    ARROW_RIGHT("fi-rr-arrow-right", 0xE0CC),
    PLUS("fi-rr-plus", 0xEC37),
    ANGLE_SMALL_LEFT("fi-rr-angle-small-left", 0xE07D),
    ANGLE_SMALL_RIGHT("fi-rr-angle-small-right", 0xE07E),
    CIRCLE("fi-rr-circle", 0xE387),
    PLAY("fi-rr-play", 0xEC28),
    PRINT("fi-rr-print", 0xEC6B),
    FILE_PDF("fi-rr-file-pdf", 0xE6B5),
    USER_ADD("fi-rr-user-add", 0xF0A8),
    SEARCH("fi-rr-search", 0xED83),
    TIME_PAST("fi-rr-time-past", 0xEFCA),
    CHECK("fi-rr-check", 0xE35F),
    TRASH("fi-rr-trash", 0xF026),
    ARROW_LEFT("fi-rr-arrow-left", 0xE0C6),
    EYE("fi-rr-eye", 0xE62C),
    EYE_CROSSED("fi-rr-eye-crossed", 0xE62F),
    TRIANGLE_WARNING("fi-rr-triangle-warning", 0xF047),
    ENVELOPE("fi-rr-envelope", 0xE5FE),
    LOCK("fi-rr-lock", 0xE9C5),
    SETTINGS("fi-rr-settings", 0xEDA3);

    private final String description;
    private final int code;

    FlaticonIcons(String description, int code) {
        this.description = description;
        this.code = code;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getCode() {
        return code;
    }

    public static FlaticonIcons findByDescription(String description) {
        for (FlaticonIcons icon : values()) {
            if (icon.getDescription().equals(description)) {
                return icon;
            }
        }
        throw new IllegalArgumentException("Icon description '" + description + "' is invalid");
    }
}
