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
                "No se encontraron rutas directas al venue '"
                        + concert.venueName()
                        + "'. Se muestran todas las rutas disponibles en el sistema.",
                concert,
                routes
        );
    }
}