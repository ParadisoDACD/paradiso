package org.ulpgc.paradiso.businessunit.datamart;

import org.ulpgc.paradiso.businessunit.recommendation.MatchType;

public record ConcertRoutePlanRecord(
        String planId,
        String eventId,
        String artistName,
        String eventName,
        String genre,
        String venueName,
        String venueKey,
        String eventDate,
        String eventTime,
        String eventDateTime,
        String originKey,
        String originName,
        String destinationStopKey,
        String destinationStopName,
        String journeyKey,
        String departureTime,
        String arrivalTime,
        Integer durationMinutes,
        Integer numberOfLegs,
        String firstLegMode,
        Double score,
        MatchType matchType,
        String computedAt
) {}