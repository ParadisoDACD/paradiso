package org.ulpgc.paradiso.businessunit.datamart;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatamartTest {

    private ConcertRecord concert(String id, String name, String venueName, String ts) {
        return new ConcertRecord(
                id,
                name,
                "",
                "",
                "",
                "London",
                "GB",
                venueName,
                "",
                "2026-06-01",
                "20:00",
                "2026-06-01T19:00:00Z",
                "music",
                ts
        );
    }

    private TransportRecord transport(String key,
                                      String hash,
                                      String dest,
                                      String srcDest,
                                      Integer dur,
                                      String ts) {
        return new TransportRecord(
                key,
                hash,
                "King's Cross",
                dest,
                "2026-06-01T09:00",
                "2026-06-01T09:30",
                dur,
                2,
                "tube",
                "2026-06-01",
                "0900",
                "KingsCross",
                srcDest,
                ts
        );
    }

    private ConcertRoutePlanRecord plan(String planId,
                                        String eventId,
                                        String artistName,
                                        String originKey,
                                        String destinationStopKey,
                                        String journeyKey,
                                        Integer durationMinutes,
                                        Double score) {
        return new ConcertRoutePlanRecord(
                planId,
                eventId,
                artistName,
                artistName + " Live",
                "music",
                "The O2",
                "the_o2",
                "2026-06-01",
                "20:00",
                "2026-06-01T20:00:00",
                originKey,
                originKey + " Station",
                destinationStopKey,
                destinationStopKey + " Stop",
                journeyKey,
                "2026-06-01T18:30:00",
                "2026-06-01T19:10:00",
                durationMinutes,
                2,
                "tube",
                score,
                "EXACT_VENUE_STOP",
                "2026-05-14T09:00:00Z"
        );
    }

    @Test
    void upsertOriginAndRetrieveCatalog() {
        Datamart dm = new Datamart();

        dm.upsertOrigin(new OriginRecord(
                "Victoria",
                "Victoria Station",
                "940GZZLUVIC",
                "Central London",
                true
        ));

        assertEquals(1, dm.originCount());
        assertTrue(dm.originByKey("Victoria").isPresent());
        assertEquals("Victoria Station", dm.origins().getFirst().originName());
    }

    @Test
    void upsertTransportIndexesByOriginAndDestination() {
        Datamart dm = new Datamart();

        dm.upsertTransport(transport("k1", "hash1", "North Greenwich", "O2Arena", 27, "ts"));

        assertEquals(1, dm.transportsByOriginAndDestination("KingsCross", "O2Arena").size());
        assertTrue(dm.transportsByOriginAndDestination("Victoria", "O2Arena").isEmpty());
    }

    @Test
    void upsertPlansIndexesByEventArtistOriginAndDestination() {
        Datamart dm = new Datamart();

        ConcertRoutePlanRecord plan1 = plan("p1", "event1", "Coldplay", "Victoria", "WembleyPark", "journey1", 34, 0.91);
        ConcertRoutePlanRecord plan2 = plan("p2", "event2", "Coldplay", "KingsCross", "O2Arena", "journey2", 27, 0.95);

        dm.upsertPlans(List.of(plan1, plan2));

        assertEquals(2, dm.planCount());
        assertEquals(1, dm.plansByEventId("event1").size());
        assertEquals(2, dm.plansByArtist("coldplay").size());
        assertEquals(1, dm.plansByOrigin("Victoria").size());
        assertEquals(1, dm.plansByArtistAndOrigin("Coldplay", "Victoria").size());
        assertEquals(1, dm.plansByOriginAndDestination("Victoria", "WembleyPark").size());
    }

    @Test
    void upsertPlansReplacesExistingPlanAcrossIndexes() {
        Datamart dm = new Datamart();

        ConcertRoutePlanRecord original = plan("p1", "event1", "Coldplay", "Victoria", "WembleyPark", "journey1", 34, 0.91);
        ConcertRoutePlanRecord updated = plan("p1", "event1", "Coldplay", "Stratford", "WembleyPark", "journey1", 30, 0.94);

        dm.upsertPlans(List.of(original));
        dm.upsertPlans(List.of(updated));

        assertEquals(1, dm.planCount());
        assertTrue(dm.plansByOrigin("Victoria").isEmpty());
        assertEquals(1, dm.plansByOrigin("Stratford").size());
        assertEquals(30, dm.plansByOrigin("Stratford").getFirst().durationMinutes());
    }

    @Test
    void replacePlansForEventRemovesObsoletePlans() {
        Datamart dm = new Datamart();

        ConcertRoutePlanRecord oldPlan1 = plan("p1", "event1", "Coldplay", "Victoria", "WembleyPark", "journey1", 34, 0.91);
        ConcertRoutePlanRecord oldPlan2 = plan("p2", "event1", "Coldplay", "KingsCross", "WembleyPark", "journey2", 31, 0.90);
        ConcertRoutePlanRecord unrelated = plan("p3", "event2", "Muse", "Victoria", "O2Arena", "journey3", 28, 0.88);
        ConcertRoutePlanRecord replacement = plan("p4", "event1", "Coldplay", "Stratford", "WembleyPark", "journey4", 25, 0.97);

        dm.upsertPlans(List.of(oldPlan1, oldPlan2, unrelated));
        dm.replacePlansForEvent("event1", List.of(replacement));

        assertEquals(2, dm.planCount());
        assertEquals(1, dm.plansByEventId("event1").size());
        assertEquals("p4", dm.plansByEventId("event1").getFirst().planId());
        assertEquals(1, dm.plansByEventId("event2").size());
    }

    @Test
    void ignoresPlansWithBlankId() {
        Datamart dm = new Datamart();

        dm.upsertPlans(List.of(plan("", "event1", "Coldplay", "Victoria", "WembleyPark", "journey1", 34, 0.91)));

        assertEquals(0, dm.planCount());
    }

    @Test
    void upsertConcertAndRetrieveById() {
        Datamart dm = new Datamart();

        dm.upsertConcert(concert("id1", "ZAZ", "O2 Academy Brixton", "2026-05-05T10:00:00Z"));

        assertTrue(dm.concertById("id1").isPresent());
        assertEquals("ZAZ", dm.concertById("id1").get().name());
    }

    @Test
    void upsertConcertReplacesExisting() {
        Datamart dm = new Datamart();

        dm.upsertConcert(concert("id1", "ZAZ v1", "Venue A", "2026-05-05T09:00:00Z"));
        dm.upsertConcert(concert("id1", "ZAZ v2", "Venue B", "2026-05-05T10:00:00Z"));

        assertEquals(1, dm.concertCount());
        assertEquals("ZAZ v2", dm.concertById("id1").get().name());
    }

    @Test
    void ignoresConcertWithNullId() {
        Datamart dm = new Datamart();

        dm.upsertConcert(new ConcertRecord(
                null,
                "ZAZ",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "ts"
        ));

        assertEquals(0, dm.concertCount());
    }

    @Test
    void ignoresConcertWithBlankId() {
        Datamart dm = new Datamart();

        dm.upsertConcert(concert("", "ZAZ", "Venue", "ts"));

        assertEquals(0, dm.concertCount());
    }

    @Test
    void concertByIdReturnsEmptyIfNotFound() {
        Datamart dm = new Datamart();

        assertTrue(dm.concertById("nonexistent").isEmpty());
    }

    @Test
    void concertsAreSortedByDateTimeIso() {
        Datamart dm = new Datamart();

        dm.upsertConcert(concert("id2", "B Concert", "Venue", "2026-06-02T19:00:00Z"));
        dm.upsertConcert(concert("id1", "A Concert", "Venue", "2026-06-01T19:00:00Z"));

        var concerts = dm.concerts();

        assertEquals("id1", concerts.get(0).externalEventId());
        assertEquals("id2", concerts.get(1).externalEventId());
    }

    @Test
    void upsertTransportAndRetrieve() {
        Datamart dm = new Datamart();

        dm.upsertTransport(transport("k1", "hash1", "North Greenwich", "O2Arena", 27, "ts"));

        assertEquals(1, dm.transportCount());
    }

    @Test
    void upsertTransportDeduplicatesByKey() {
        Datamart dm = new Datamart();

        TransportRecord r = transport("k1", "hash1", "North Greenwich", "O2Arena", 27, "ts");

        dm.upsertTransport(r);
        dm.upsertTransport(r);

        assertEquals(1, dm.transportCount());
    }

    @Test
    void upsertTransportAllowsDifferentKeys() {
        Datamart dm = new Datamart();

        dm.upsertTransport(transport("k1", "h1", "North Greenwich", "O2Arena", 27, "ts"));
        dm.upsertTransport(transport("k2", "h1", "Brixton", "BrixtonAcademy", 20, "ts"));

        assertEquals(2, dm.transportCount());
    }

    @Test
    void ignoresTransportWithBlankKey() {
        Datamart dm = new Datamart();

        dm.upsertTransport(new TransportRecord(
                "",
                "h1",
                "Origin",
                "Dest",
                "",
                "",
                20,
                1,
                "tube",
                "",
                "",
                "",
                "",
                "ts"
        ));

        assertEquals(0, dm.transportCount());
    }

    @Test
    void updatesLastProcessedAtWithLatestTimestamp() {
        Datamart dm = new Datamart();

        dm.upsertConcert(concert("id1", "Concert", "Venue", "2026-05-05T09:00:00Z"));
        dm.upsertConcert(concert("id2", "Concert", "Venue", "2026-05-06T10:00:00Z"));

        assertEquals("2026-05-06T10:00:00Z", dm.lastProcessedAt());
    }

    @Test
    void keepsLatestTimestampWhenOlderEventArrivesLater() {
        Datamart dm = new Datamart();

        dm.upsertConcert(concert("id1", "Concert", "Venue", "2026-05-06T10:00:00Z"));
        dm.upsertConcert(concert("id2", "Concert", "Venue", "2026-05-05T09:00:00Z"));

        assertEquals("2026-05-06T10:00:00Z", dm.lastProcessedAt());
    }

    @Test
    void handlesInvalidTimestampGracefully() {
        Datamart dm = new Datamart();

        assertDoesNotThrow(() ->
                dm.upsertConcert(concert("id1", "Concert", "Venue", "not-a-timestamp"))
        );

        assertEquals(1, dm.concertCount());
        assertTrue(dm.concertById("id1").isPresent());
        assertEquals("", dm.lastProcessedAt());
    }
}