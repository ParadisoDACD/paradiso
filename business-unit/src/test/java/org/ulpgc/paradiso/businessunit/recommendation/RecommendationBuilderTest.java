package org.ulpgc.paradiso.businessunit.recommendation;

import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRoutePlanRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;
import org.ulpgc.paradiso.businessunit.venue.VenueNormalizer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationBuilderTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-14T09:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void buildPlansForConcertUsesVenueMappingAndMatchingTransportRoutes() {
        Datamart datamart = new Datamart();
        datamart.upsertTransport(transport("route-o2", "Victoria", "O2Arena", "North Greenwich", 31));
        datamart.upsertTransport(transport("route-wembley", "Victoria", "WembleyPark", "Wembley Park", 42));

        RecommendationBuilder builder = builder(datamart);

        List<ConcertRoutePlanRecord> plans = builder.buildPlansForConcert(concert("event1", "Coldplay", "The O2"));

        ConcertRoutePlanRecord plan = plans.get(0);

        assertEquals(1, plans.size());
        assertEquals("event1", plans.getFirst().eventId());
        assertEquals("Victoria", plans.getFirst().originKey());
        assertEquals("O2Arena", plans.getFirst().destinationStopKey());
        assertEquals("route-o2", plans.getFirst().journeyKey());
        assertEquals(MatchType.EXACT_VENUE_STOP, plan.matchType());
    }

    @Test
    void buildPlansForConcertSortsRoutesByScore() {
        Datamart datamart = new Datamart();
        datamart.upsertTransport(transport("slow", "Victoria", "O2Arena", "North Greenwich", 55));
        datamart.upsertTransport(transport("fast", "KingsCross", "O2Arena", "North Greenwich", 25));

        RecommendationBuilder builder = builder(datamart);

        List<ConcertRoutePlanRecord> plans = builder.buildPlansForConcert(concert("event1", "Coldplay", "The O2"));

        assertEquals(2, plans.size());
        assertEquals("fast", plans.getFirst().journeyKey());
        assertTrue(plans.getFirst().score() > plans.get(1).score());
    }

    @Test
    void buildPlansForConcertReturnsEmptyForUnknownVenue() {
        Datamart datamart = new Datamart();
        datamart.upsertTransport(transport("route-o2", "Victoria", "O2Arena", "North Greenwich", 31));

        RecommendationBuilder builder = builder(datamart);

        assertTrue(builder.buildPlansForConcert(concert("event1", "Unknown Artist", "Unknown Venue")).isEmpty());
    }

    @Test
    void buildPlansForTransportFindsConcertsMappedToItsDestination() {
        Datamart datamart = new Datamart();
        datamart.upsertConcert(concert("event-o2", "Coldplay", "The O2"));
        datamart.upsertConcert(concert("event-wembley", "Muse", "Wembley Stadium"));

        RecommendationBuilder builder = builder(datamart);

        List<ConcertRoutePlanRecord> plans = builder.buildPlansForTransport(
                transport("route-o2", "Victoria", "O2Arena", "North Greenwich", 31)
        );

        assertEquals(1, plans.size());
        assertEquals("event-o2", plans.getFirst().eventId());
        assertEquals("route-o2", plans.getFirst().journeyKey());
    }

    @Test
    void rebuildAllStoresGeneratedPlansInDatamart() {
        Datamart datamart = new Datamart();
        datamart.upsertConcert(concert("event-o2", "Coldplay", "The O2"));
        datamart.upsertTransport(transport("route-o2", "Victoria", "O2Arena", "North Greenwich", 31));

        RecommendationBuilder builder = builder(datamart);
        builder.rebuildAll();

        assertEquals(1, datamart.planCount());
        assertEquals("event-o2", datamart.plansByEventId("event-o2").getFirst().eventId());
    }

    @Test
    void doesNotBuildPlanWhenConcertAndRouteDatesAreDifferent() {
        Datamart datamart = new Datamart();
        datamart.upsertTransport(new TransportRecord(
                "route-o2",
                "route-o2-hash",
                "Victoria",
                "North Greenwich",
                "2026-06-02T18:30:00",
                "2026-06-02T19:10:00",
                31,
                2,
                "tube",
                "2026-06-02",
                "1830",
                "Victoria",
                "O2Arena",
                "2026-05-14T09:00:00Z"
        ));

        RecommendationBuilder builder = builder(datamart);

        assertTrue(builder.buildPlansForConcert(concert("event-o2", "Coldplay", "The O2")).isEmpty());
    }

    private RecommendationBuilder builder(Datamart datamart) {
        return new RecommendationBuilder(
                datamart,
                new VenueNormalizer(),
                new RouteScoringService(),
                FIXED_CLOCK
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