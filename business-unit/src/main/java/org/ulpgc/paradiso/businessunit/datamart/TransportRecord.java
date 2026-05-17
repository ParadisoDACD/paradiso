package org.ulpgc.paradiso.businessunit.datamart;

public record TransportRecord(
        String journeyKey,
        String journeyHash,
        String originName,
        String destinationName,
        String startDateTime,
        String arrivalDateTime,
        Integer durationMinutes,
        Integer numberOfLegs,
        String firstLegMode,
        String captureDate,
        String captureTime,
        String sourceOrigin,
        String sourceDestination,
        String capturedAt
) {}