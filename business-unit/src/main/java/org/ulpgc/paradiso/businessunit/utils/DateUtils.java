package org.ulpgc.paradiso.businessunit.utils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public final class DateUtils {

    private DateUtils() {}

    public static Optional<LocalDate> parseDatePrefix(String value) {
        String safeValue = StringUtils.safe(value);
        if (safeValue.length() < 10) {
            return Optional.empty();
        }
        return parseDate(safeValue.substring(0, 10));
    }

    public static Optional<LocalDate> parseDate(String value) {
        String safeValue = StringUtils.safe(value);
        if (safeValue.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(safeValue));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }
}