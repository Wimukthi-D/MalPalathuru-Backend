package org.wimukthi.malpalathurubackend.service;

import org.wimukthi.malpalathurubackend.enums.Language;

import java.util.Locale;

final class GameTextNormalizer {

    private GameTextNormalizer() {
    }

    static String normalizeRoomCode(String roomCode) {
        return roomCode == null ? "" : roomCode.trim().toUpperCase(Locale.ROOT);
    }

    static String sanitizeAnswer(String value) {
        return value == null ? "" : value.trim();
    }

    static String normalizeAnswer(Language language, String value) {
        String sanitized = sanitizeAnswer(value);
        if (language == Language.ENGLISH) {
            return sanitized.toLowerCase(Locale.ROOT);
        }
        return sanitized;
    }

    static String displayLetter(Language language, String letter) {
        String sanitized = sanitizeAnswer(letter);
        if (language == Language.ENGLISH) {
            return sanitized.toUpperCase(Locale.ROOT);
        }
        return sanitized;
    }

    static String normalizeLetter(Language language, String letter) {
        String display = displayLetter(language, letter);
        if (language == Language.ENGLISH) {
            return display.toLowerCase(Locale.ROOT);
        }
        return display;
    }
}
