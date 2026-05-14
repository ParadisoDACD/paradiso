package org.ulpgc.paradiso.tfl.mapper;

public record TflCaptureContext(String sourceOrigin,
                                String sourceDestination,
                                String captureDate,
                                String captureTime,
                                String captureBatchId,
                                String capturedAt) {
}