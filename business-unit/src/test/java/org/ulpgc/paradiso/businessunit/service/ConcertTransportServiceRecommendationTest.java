package org.ulpgc.paradiso.businessunit.service;

import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRoutePlanRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConcertTransportServiceRecommendationTest {

    @Test
    void recommendationsReturnsPrecomputedPlans() {
        Datamart datamart = new Datamart();
        datamart.upsertPlans(List.of(
                plan("p1", "event1", "Coldplay", "Victoria", "The O2", "the_o2", "O2Arena", "2026-06-01")
        ));

        ConcertTransportService service = new ConcertTransportService(datamart);

        assertEquals(1, service.recommendations(RecommendationFilter.empty()).size());
    }

    @Test
    void recommendationsCanFilterByArtist() {
        Datamart datamart = new Datamart();
        datamart.upsertPlans(List.of(
                plan("p1", "event1", "Coldplay", "Victoria", "The O2", "the_o2", "O2Arena", "2026-06-01"),
                plan("p2", "event2", "Muse", "Victoria", "Wembley Stadium", "wembley", "WembleyPark", "2026-06-01")
        ));

        ConcertTransportService service = new ConcertTransportService(datamart);

        List<ConcertRoutePlanRecord> results = service.recommendations(new RecommendationFilter(
                null,
                "cold",
                null,
                null,
                null,
                null
        ));

        assertEquals(1, results.size());
        assertEquals("Coldplay", results.getFirst().artistName());
    }

    @Test
    void recommendationsCanFilterByArtistAndOriginUsingIndexes() {
        Datamart datamart = new Datamart();
        datamart.upsertPlans(List.of(
                plan("p1", "event1", "Coldplay", "Victoria", "The O2", "the_o2", "O2Arena", "2026-06-01"),
                plan("p2", "event1", "Coldplay", "KingsCross", "The O2", "the_o2", "O2Arena", "2026-06-01")
        ));

        ConcertTransportService service = new ConcertTransportService(datamart);

        List<ConcertRoutePlanRecord> results = service.recommendationsByArtistAndOrigin("Coldplay", "Victoria");

        assertEquals(1, results.size());
        assertEquals("Victoria", results.getFirst().originKey());
    }

    @Test
    void recommendationsCanFilterByEventAndOrigin() {
        Datamart datamart = new Datamart();
        datamart.upsertPlans(List.of(
                plan("p1", "event1", "Coldplay", "Victoria", "The O2", "the_o2", "O2Arena", "2026-06-01"),
                plan("p2", "event1", "Coldplay", "KingsCross", "The O2", "the_o2", "O2Arena", "2026-06-01"),
                plan("p3", "event2", "Muse", "Victoria", "Wembley Stadium", "wembley", "WembleyPark", "2026-06-01")
        ));

        ConcertTransportService service = new ConcertTransportService(datamart);

        List<ConcertRoutePlanRecord> results = service.recommendationsByEventAndOrigin("event1", "KingsCross");

        assertEquals(1, results.size());
        assertEquals("event1", results.getFirst().eventId());
        assertEquals("KingsCross", results.getFirst().originKey());
    }

    @Test
    void recommendationsCanFilterByVenue() {
        Datamart datamart = new Datamart();
        datamart.upsertPlans(List.of(
                plan("p1", "event1", "Coldplay", "Victoria", "The O2", "the_o2", "O2Arena", "2026-06-01"),
                plan("p2", "event2", "Muse", "Victoria", "Wembley Stadium", "wembley", "WembleyPark", "2026-06-01")
        ));

        ConcertTransportService service = new ConcertTransportService(datamart);

        List<ConcertRoutePlanRecord> results = service.recommendations(new RecommendationFilter(
                null,
                null,
                null,
                "wembley",
                null,
                null
        ));

        assertEquals(1, results.size());
        assertEquals("WembleyPark", results.getFirst().destinationStopKey());
    }

    @Test
    void recommendationsCanFilterByDateRange() {
        Datamart datamart = new Datamart();
        datamart.upsertPlans(List.of(
                plan("p1", "event1", "Coldplay", "Victoria", "The O2", "the_o2", "O2Arena", "2026-06-01"),
                plan("p2", "event2", "Muse", "Victoria", "Wembley Stadium", "wembley", "WembleyPark", "2026-07-15")
        ));

        ConcertTransportService service = new ConcertTransportService(datamart);

        List<ConcertRoutePlanRecord> results = service.recommendations(new RecommendationFilter(
                null,
                null,
                null,
                null,
                "2026-07-01",
                "2026-07-31"
        ));

        assertEquals(1, results.size());
        assertEquals("event2", results.getFirst().eventId());
    }

    @Test
    void venueMappingsReturnsConfiguredMappings() {
        ConcertTransportService service = new ConcertTransportService(new Datamart());

        assertFalse(service.venueMappings().isEmpty());
    }

    private ConcertRoutePlanRecord plan(String planId,
                                        String eventId,
                                        String artist,
                                        String originKey,
                                        String venueName,
                                        String venueKey,
                                        String destinationStopKey,
                                        String eventDate) {
        return new ConcertRoutePlanRecord(
                planId,
                eventId,
                artist,
                artist + " Live",
                "Rock",
                venueName,
                venueKey,
                eventDate,
                "20:00:00",
                eventDate + "T20:00:00",
                originKey,
                originKey + " Station",
                destinationStopKey,
                destinationStopKey + " Station",
                "journey-" + planId,
                eventDate + "T18:30:00",
                eventDate + "T19:10:00",
                31,
                2,
                "tube",
                0.91,
                "EXACT_VENUE_STOP",
                "2026-05-14T09:00:00Z"
        );
    }
}