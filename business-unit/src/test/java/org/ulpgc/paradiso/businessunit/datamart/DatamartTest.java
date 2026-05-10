package org.ulpgc.paradiso.businessunit.datamart;

import org.junit.jupiter.api.Test;

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