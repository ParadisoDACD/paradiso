package org.ulpgc.paradiso.businessunit.recommendation;

import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;
import org.ulpgc.paradiso.businessunit.utils.StringUtils;
import org.ulpgc.paradiso.businessunit.venue.VenueStopMapping;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class RouteScoringService {

    private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");

    public double score(ConcertRecord concert, TransportRecord transport, VenueStopMapping mapping) {
        double score = 1.0;

        score -= durationPenalty(transport.durationMinutes());
        score -= legsPenalty(transport.numberOfLegs());
        score -= arrivalPenalty(concert, transport);

        if (isExactStopMatch(transport, mapping)) {
            score += 0.20;
        }

        return round(clamp(score));
    }

    private double durationPenalty(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return 0.15;
        }

        return Math.min(durationMinutes, 180) * 0.004;
    }

    private double legsPenalty(Integer numberOfLegs) {
        if (numberOfLegs == null || numberOfLegs <= 0) {
            return 0.05;
        }

        return Math.min(numberOfLegs, 8) * 0.03;
    }

    private double arrivalPenalty(ConcertRecord concert, TransportRecord transport) {
        Optional<LocalDateTime> eventDateTime = eventDateTime(concert);
        Optional<LocalDateTime> arrivalDateTime = parseDateTime(transport.arrivalDateTime());

        if (eventDateTime.isEmpty() || arrivalDateTime.isEmpty()) {
            return 0.0;
        }

        long minutesBeforeEvent = Duration.between(arrivalDateTime.get(), eventDateTime.get()).toMinutes();

        if (minutesBeforeEvent < 0) {
            return Math.min(Math.abs(minutesBeforeEvent) * 0.01, 0.60);
        }

        return Math.min(minutesBeforeEvent, 180) * 0.0015;
    }

    private boolean isExactStopMatch(TransportRecord transport, VenueStopMapping mapping) {
        return StringUtils.safe(transport.sourceDestination())
                .equalsIgnoreCase(StringUtils.safe(mapping.nearestStopKey()));
    }

    private Optional<LocalDateTime> eventDateTime(ConcertRecord concert) {
        Optional<LocalDateTime> fromIso = parseDateTime(concert.dateTimeIso());
        if (fromIso.isPresent()) {
            return fromIso;
        }

        String date = StringUtils.safe(concert.localDate());
        String time = StringUtils.safe(concert.localTime());

        if (date.isBlank()) {
            return Optional.empty();
        }

        if (time.isBlank()) {
            time = "00:00:00";
        }

        if (time.length() == 5) {
            time = time + ":00";
        }

        return parseDateTime(date + "T" + time);
    }

    private Optional<LocalDateTime> parseDateTime(String value) {
        String safeValue = StringUtils.safe(value);

        if (safeValue.isBlank()) {
            return Optional.empty();
        }

        return tryParseInstant(safeValue)
                .or(() -> tryParseOffsetDateTime(safeValue))
                .or(() -> tryParseLocalDateTime(safeValue));
    }

    private Optional<LocalDateTime> tryParseInstant(String value) {
        try {
            return Optional.of(Instant.parse(value).atZone(LONDON_ZONE).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    private Optional<LocalDateTime> tryParseOffsetDateTime(String value) {
        try {
            return Optional.of(OffsetDateTime.parse(value).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    private Optional<LocalDateTime> tryParseLocalDateTime(String value) {
        try {
            return Optional.of(LocalDateTime.parse(value));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}