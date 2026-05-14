package org.ulpgc.paradiso.businessunit.service;

import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;
import org.ulpgc.paradiso.businessunit.recommendation.RecommendationBuilder;
import org.ulpgc.paradiso.businessunit.recommendation.RouteScoringService;
import org.ulpgc.paradiso.businessunit.venue.VenueNormalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessIngestionServiceTest {

    @Test
    void ingestConcertStoresConcertAndBuildsPlansFromExistingRoutes() {
        Datamart datamart = new Datamart();
        BusinessIngestionService ingestionService = ingestionService(datamart);

        datamart.upsertTransport(transport("route-o2", "Victoria", "O2Arena", "North Greenwich", 31));

        ingestionService.ingestConcert(concert("event-o2", "Coldplay", "The O2"));

        assertEquals(1, datamart.concertCount());
        assertEquals(1, datamart.planCount());
        assertEquals("route-o2", datamart.plansByEventId("event-o2").getFirst().journeyKey());
    }

    @Test
    void ingestTransportStoresTransportAndBuildsPlansForExistingConcerts() {
        Datamart datamart = new Datamart();
        BusinessIngestionService ingestionService = ingestionService(datamart);

        datamart.upsertConcert(concert("event-o2", "Coldplay", "The O2"));

        ingestionService.ingestTransport(transport("route-o2", "Victoria", "O2Arena", "North Greenwich", 31));

        assertEquals(1, datamart.transportCount());
        assertEquals(1, datamart.planCount());
        assertEquals("event-o2", datamart.plansByEventId("event-o2").getFirst().eventId());
    }

    @Test
    void ingestConcertReplacesObsoletePlansForUpdatedEvent() {
        Datamart datamart = new Datamart();
        BusinessIngestionService ingestionService = ingestionService(datamart);

        datamart.upsertTransport(transport("route-o2", "Victoria", "O2Arena", "North Greenwich", 31));

        ingestionService.ingestConcert(concert("event-o2", "Coldplay", "The O2"));
        assertEquals(1, datamart.planCount());

        ingestionService.ingestConcert(concert("event-o2", "Coldplay", "Unknown Venue"));

        assertEquals(1, datamart.concertCount());
        assertEquals(0, datamart.planCount());
        assertTrue(datamart.plansByEventId("event-o2").isEmpty());
    }

    @Test
    void rebuildRecommendationsBuildsPlansFromCurrentDatamartState() {
        Datamart datamart = new Datamart();
        BusinessIngestionService ingestionService = ingestionService(datamart);

        datamart.upsertConcert(concert("event-o2", "Coldplay", "The O2"));
        datamart.upsertTransport(transport("route-o2", "Victoria", "O2Arena", "North Greenwich", 31));

        ingestionService.rebuildRecommendations();

        assertEquals(1, datamart.planCount());
        assertEquals("event-o2::route-o2", datamart.plans().getFirst().planId());
    }

    private BusinessIngestionService ingestionService(Datamart datamart) {
        return new BusinessIngestionService(
                datamart,
                new RecommendationBuilder(
                        datamart,
                        new VenueNormalizer(),
                        new RouteScoringService()
                )
        );
    }

    private ConcertRecord concert(String id, String name, String venueName) {
        return new ConcertRecord(
                id,
                name,
                "Music",
                "Music",
                "Rock",
                "London",
                "GB",
                venueName,
                "",
                "2026-06-01",
                "20:00:00",
                "2026-06-01T20:00:00",
                "music",
                "2026-05-14T09:00:00Z"
        );
    }

    private TransportRecord transport(String key,
                                      String originKey,
                                      String destinationKey,
                                      String destinationName,
                                      Integer durationMinutes) {
        return new TransportRecord(
                key,
                key + "-hash",
                originKey,
                destinationName,
                "2026-06-01T18:30:00",
                "2026-06-01T19:10:00",
                durationMinutes,
                2,
                "tube",
                "2026-06-01",
                "1830",
                originKey,
                destinationKey,
                "2026-05-14T09:00:00Z"
        );
    }
}