package org.ulpgc.paradiso.businessunit.datamart;

public record ConcertRecord(
        String externalEventId,
        String name,
        String classificationName,
        String segment,
        String genre,
        String city,
        String countryCode,
        String venueName,
        String eventUrl,
        String localDate,
        String localTime,
        String dateTimeIso,
        String sourceCategory,
        String capturedAt
) {}