package org.ulpgc.paradiso.businessunit.cli;

import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRoutePlanRecord;
import org.ulpgc.paradiso.businessunit.datamart.OriginRecord;
import org.ulpgc.paradiso.businessunit.utils.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class CliFormatter {

    private static final Locale SPANISH = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter DATE_IN = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_OUT =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", SPANISH);
    private static final DateTimeFormatter TIME_IN = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_OUT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DT_IN = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public String concertSummary(ConcertRecord concert) {
        String time = formatTime(concert.localTime());
        String timePart = time.isBlank() ? "" : "  ·  " + time + "h";
        return StringUtils.safe(concert.name())
                + "  ·  " + StringUtils.safe(concert.venueName())
                + "  ·  " + formatDate(concert.localDate()) + timePart;
    }

    public String concertDetail(ConcertRecord concert) {
        return "Concierto: " + StringUtils.safe(concert.name()) + "\n"
                + "Recinto:     " + StringUtils.safe(concert.venueName())
                + ", " + StringUtils.safe(concert.city()) + "\n"
                + "Fecha:     " + formatDate(concert.localDate()) + concertTimePart(concert) + "\n"
                + genreLine(concert)
                + urlLine(concert);
    }

    public String originSummary(OriginRecord origin) {
        String name = originName(origin);
        String area = originArea(origin);
        return name + area;
    }

    private String originArea(OriginRecord origin) {
        String area = StringUtils.safe(origin.area());
        if (area.isBlank() || area.equalsIgnoreCase("Londres")) return "";
        return "  (zona: " + area + ")";
    }

    public String routeDetail(ConcertRoutePlanRecord plan, int position) {
        return routeHeader(plan, position) + routeBody(plan);
    }

    private String routeHeader(ConcertRoutePlanRecord plan, int position) {
        return "  ── Ruta " + position + " ─────────────────────────────────────\n"
                + "  Puntuación: " + scoreBar(plan.score()) + "\n"
                + "  Desde:    " + StringUtils.safe(plan.originName()) + "\n"
                + "  Hasta:    " + StringUtils.safe(plan.destinationStopName())
                + "  (más cercana a " + StringUtils.safe(plan.venueName()) + ")\n";
    }

    private String routeBody(ConcertRoutePlanRecord plan) {
        return "  Sale:     " + formatIsoDateTime(plan.departureTime()) + "h\n"
                + "  Llega:    " + formatIsoDateTime(plan.arrivalTime()) + "h\n"
                + "  Duración: " + formatDuration(plan.durationMinutes()) + "\n"
                + "  Modo:     " + formatMode(plan.firstLegMode()) + legsInfo(plan) + "\n"
                + matchTypeLine(plan);
    }

    private String originName(OriginRecord origin) {
        String name = StringUtils.safe(origin.originName());
        return name.isBlank() ? StringUtils.safe(origin.originKey()) : name;
    }

    private String concertTimePart(ConcertRecord concert) {
        String time = formatTime(concert.localTime());
        return time.isBlank() ? "" : "  a las " + time + "h";
    }

    private String genreLine(ConcertRecord concert) {
        return StringUtils.safe(concert.genre()).isBlank() ? "" : "Género:    " + concert.genre() + "\n";
    }

    private String urlLine(ConcertRecord concert) {
        return StringUtils.safe(concert.eventUrl()).isBlank() ? "" : "URL:       " + concert.eventUrl() + "\n";
    }

    private String legsInfo(ConcertRoutePlanRecord plan) {
        if (plan.numberOfLegs() == null || plan.numberOfLegs() <= 1) return "";
        return "  (" + plan.numberOfLegs() + " tramos)";
    }

    private String matchTypeLine(ConcertRoutePlanRecord plan) {
        if (plan.matchType() == null) return "";
        String label = switch (plan.matchType()) {
            case EXACT_VENUE_STOP -> "Parada exacta al recinto";
            case ALIAS_MATCH -> "Parada aproximada al recinto";
        };
        return "  Coincidencia: " + label + "\n";
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isBlank()) return "fecha por confirmar";
        return parsedDate(raw.length() > 10 ? raw.substring(0, 10) : raw);
    }

    private String parsedDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_IN).format(DATE_OUT);
        } catch (DateTimeParseException e) {
            return dateStr;
        }
    }

    private String formatTime(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return parsedTime(raw.length() == 5 ? raw + ":00" : raw);
    }

    private String parsedTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr, TIME_IN).format(TIME_OUT);
        } catch (DateTimeParseException e) {
            return timeStr.length() >= 5 ? timeStr.substring(0, 5) : timeStr;
        }
    }

    private String formatIsoDateTime(String raw) {
        if (raw == null || raw.isBlank()) return "–";
        return parsedDateTime(raw.length() > 19 ? raw.substring(0, 19) : raw);
    }

    private String parsedDateTime(String dtStr) {
        try {
            return LocalDateTime.parse(dtStr, DT_IN).format(TIME_OUT);
        } catch (DateTimeParseException e) {
            return dtStr.length() >= 16 ? dtStr.substring(11, 16) : dtStr;
        }
    }

    private String formatDuration(Integer minutes) {
        if (minutes == null || minutes <= 0) return "–";
        if (minutes < 60) return minutes + " min";
        int hours = minutes / 60;
        int remaining = minutes % 60;
        return remaining == 0 ? hours + " h" : hours + " h " + remaining + " min";
    }

    private String formatMode(String mode) {
        if (mode == null || mode.isBlank()) return "transporte público";
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "tube" -> "Metro (Tube)";
            case "bus" -> "Guagua";
            case "overground" -> "Overground";
            case "dlr" -> "DLR";
            case "elizabeth" -> "Elizabeth line";
            case "walking" -> "A pie";
            case "national-rail", "national rail" -> "Tren nacional";
            default -> mode;
        };
    }

    private String scoreBar(Double score) {
        if (score == null) return "sin datos";
        int filled = scoreStars(score);
        return "★".repeat(filled) + "☆".repeat(5 - filled)
                + String.format(Locale.ROOT, "  (%.2f / 1.00)", score);
    }

    private int scoreStars(double score) {
        int stars = (int) Math.round(score * 5);
        return Math.max(0, Math.min(5, stars));
    }
}