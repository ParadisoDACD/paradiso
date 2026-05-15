package org.ulpgc.paradiso.businessunit.service;

import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;

import java.util.List;

public record ConcertTransportResponse(
        boolean found,
        boolean venueMatch,
        String message,
        ConcertRecord concert,
        List<TransportRecord> routes
) {

    public static ConcertTransportResponse notFound(String id) {
        return new ConcertTransportResponse(
                false,
                false,
                "Concierto no encontrado: " + id,
                null,
                List.of()
        );
    }

    public static ConcertTransportResponse matched(ConcertRecord concert,
                                                   List<TransportRecord> routes) {
        return new ConcertTransportResponse(
                true,
                true,
                "Rutas encontradas por coincidencia con el venue '"
                        + concert.venueName() + "'.",
                concert,
                routes
        );
    }

    public static ConcertTransportResponse fallback(ConcertRecord concert,
                                                    List<TransportRecord> routes) {
        return new ConcertTransportResponse(
                true,
                false,
                fallbackMessage(concert, routes),
                concert,
                routes
        );
    }

    private static String fallbackMessage(ConcertRecord concert, List<TransportRecord> routes) {
        String baseMessage = "No se encontraron rutas directas al venue '"
                + concert.venueName()
                + "'. ";

        if (routes.isEmpty()) {
            return baseMessage
                    + "No hay rutas TfL vigentes disponibles. "
                    + "Ejecuta tfl-feeder para refrescar el histórico de transporte.";
        }

        return baseMessage
                + "Se muestran rutas vigentes disponibles según la última captura TfL.";
    }
}