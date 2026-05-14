package org.ulpgc.paradiso.ticketmaster.mapper;

public record TicketmasterCaptureContext(String sourceCountry,
                                         String sourceCity,
                                         String sourceCategory,
                                         String captureBatchId,
                                         String capturedAt) {
}
