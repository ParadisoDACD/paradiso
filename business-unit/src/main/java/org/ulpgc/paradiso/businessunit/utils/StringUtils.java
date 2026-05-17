package org.ulpgc.paradiso.businessunit.utils;

import java.util.Locale;

public final class StringUtils {

    private StringUtils() {}

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}