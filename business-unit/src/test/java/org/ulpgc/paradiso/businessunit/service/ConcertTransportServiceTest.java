package org.ulpgc.paradiso.businessunit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;

import static org.junit.jupiter.api.Assertions.*;

class ConcertTransportServiceTest {

    private Datamart datamart;
    private ConcertTransportService service;

    @BeforeEach
    void setUp() {
        datamart = new Datamart();
        service = new ConcertTransportService(datamart);
    }

    private ConcertRecord concert(String id, String venueName) {
        return new ConcertRecord(
                id,
                "Concert " + id,
                "music",
                "Music",
                "Rock",
                "London",
                "GB",
                venueName,
                "",
                "2026-06-01",
                "20:00",
                "2026-06-01T19:00:00Z",
                "music",
                "2026-05-05T10:00:00Z"
        );
    }

    private TransportRecord transport(String key,
                                      String destinationName,
                                      String sourceDestination,
                                      Integer durationMinutes) {
        return new TransportRecord(
                key,
                "hash-" + key,
                "King's Cross",
                destinationName,
                "2026-06-01T09:00",
                "2026-06-01T09:30",
                durationMinutes,
                2,
                "tube",
                "2026-06-01",
                "0900",
                "KingsCross",
                sourceDestination,
                "2026-05-05T10:00:00Z"
        );
    }

    @Test
    void returnsNotFoundForUnknownConcertId() {
        ConcertTransportResponse response = service.transportForConcert("nonexistent");

        assertFalse(response.found());
        assertFalse(response.venueMatch());
        assertTrue(response.routes().isEmpty());
        assertNull(response.concert());
    }

    @Test
    void findsTransportByVenueKeywordInDestinationName() {
        datamart.upsertConcert(concert("c1", "O2 Academy Brixton"));
        datamart.upsertTransport(transport("k1", "Brixton Underground Station", "Brixton", 15));
        datamart.upsertTransport(transport("k2", "Victoria Station", "Victoria", 25));

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.found());
        assertTrue(response.venueMatch());
        assertEquals(1, response.routes().size());
        assertTrue(response.routes().get(0).destinationName().contains("Brixton"));
    }

    @Test
    void findsTransportBySourceDestinationField() {
        datamart.upsertConcert(concert("c1", "O2 Arena Greenwich"));
        datamart.upsertTransport(transport("k1", "North Greenwich Underground", "O2Arena", 27));

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.found());
        assertTrue(response.venueMatch());
        assertEquals(1, response.routes().size());
    }

    @Test
    void returnsMultipleMatchesSortedByDuration() {
        datamart.upsertConcert(concert("c1", "Wembley Stadium"));
        datamart.upsertTransport(transport("k1", "Wembley Park Station", "WembleyPark", 35));
        datamart.upsertTransport(transport("k2", "Wembley Central Station", "WembleyCentral", 20));
        datamart.upsertTransport(transport("k3", "Wembley Stadium Station", "Wembley", 10));

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.venueMatch());
        assertEquals(3, response.routes().size());
        assertEquals(10, response.routes().get(0).durationMinutes());
        assertEquals(20, response.routes().get(1).durationMinutes());
        assertEquals(35, response.routes().get(2).durationMinutes());
    }

    @Test
    void handlesNullDurationInSorting() {
        datamart.upsertConcert(concert("c1", "Brixton Academy"));
        datamart.upsertTransport(transport("k1", "Brixton Station", "Brixton", null));
        datamart.upsertTransport(transport("k2", "Brixton Underground", "Brixton2", 15));

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.venueMatch());
        assertEquals(2, response.routes().size());
        assertEquals(15, response.routes().get(0).durationMinutes());
        assertNull(response.routes().get(1).durationMinutes());
    }

    @Test
    void returnsFallbackWhenNoKeywordMatchFound() {
        datamart.upsertConcert(concert("c1", "Some Obscure Venue XYZ"));
        datamart.upsertTransport(transport("k1", "Victoria Station", "Victoria", 20));
        datamart.upsertTransport(transport("k2", "London Bridge Station", "LondonBridge", 30));

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.found());
        assertFalse(response.venueMatch());
        assertEquals(2, response.routes().size());
        assertTrue(response.message().toLowerCase().contains("no se encontraron"));
    }

    @Test
    void returnsFallbackWhenVenueNameIsBlank() {
        datamart.upsertConcert(concert("c1", ""));
        datamart.upsertTransport(transport("k1", "Victoria Station", "Victoria", 20));

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.found());
        assertFalse(response.venueMatch());
        assertEquals(1, response.routes().size());
    }

    @Test
    void stopwordsDoNotGenerateFalseMatches() {
        datamart.upsertConcert(concert("c1", "The Arena London"));
        datamart.upsertTransport(transport("k1", "Victoria Station", "Victoria", 20));

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.found());
        assertFalse(response.venueMatch());
    }

    @Test
    void returnsEmptyRoutesWhenFallbackAndNoTransportsAvailable() {
        datamart.upsertConcert(concert("c1", "Unknown Venue XYZ"));

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.found());
        assertFalse(response.venueMatch());
        assertTrue(response.routes().isEmpty());
    }

    @Test
    void matchedResponseIncludesConcertData() {
        datamart.upsertConcert(concert("c1", "O2 Academy Brixton"));
        datamart.upsertTransport(transport("k1", "Brixton Station", "Brixton", 15));

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertNotNull(response.concert());
        assertEquals("c1", response.concert().externalEventId());
        assertEquals("O2 Academy Brixton", response.concert().venueName());
    }

    @Test
    void responseMessageContainsVenueName() {
        datamart.upsertConcert(concert("c1", "Royal Albert Hall"));
        datamart.upsertTransport(transport("k1", "South Kensington Station", "SouthKensington", 10));

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.message().contains("Royal Albert Hall"));
    }

    @Test
    void searchConcertsByArtistOrEventName() {
        datamart.upsertConcert(concert("c1", "O2 Academy Brixton"));
        datamart.upsertConcert(new ConcertRecord(
                "c2",
                "Tame Impala",
                "music",
                "Music",
                "Rock",
                "London",
                "GB",
                "The O2",
                "",
                "2026-06-02",
                "20:00",
                "2026-06-02T19:00:00Z",
                "music",
                "2026-05-05T10:00:00Z"
        ));

        var results = service.searchConcerts("tame");

        assertEquals(1, results.size());
        assertEquals("Tame Impala", results.get(0).name());
    }

    @Test
    void searchConcertsByVenueName() {
        datamart.upsertConcert(concert("c1", "O2 Academy Brixton"));
        datamart.upsertConcert(concert("c2", "Royal Albert Hall"));

        var results = service.searchConcerts("brixton");

        assertEquals(1, results.size());
        assertEquals("O2 Academy Brixton", results.get(0).venueName());
    }

    @Test
    void searchConcertsReturnsAllWhenQueryIsBlank() {
        datamart.upsertConcert(concert("c1", "O2 Academy Brixton"));
        datamart.upsertConcert(concert("c2", "Royal Albert Hall"));

        var results = service.searchConcerts("   ");

        assertEquals(2, results.size());
    }

    @Test
    void recommendationsForSearchReturnsNotFoundWhenNoConcertMatches() {
        datamart.upsertConcert(concert("c1", "O2 Academy Brixton"));

        ConcertSearchTransportResponse response = service.recommendationsForSearch("artist-not-found");

        assertFalse(response.found());
        assertEquals(0, response.matches());
        assertTrue(response.results().isEmpty());
    }

    @Test
    void recommendationsForSearchReturnsTransportForMatchingConcert() {
        datamart.upsertConcert(new ConcertRecord(
                "c1",
                "Example",
                "music",
                "Music",
                "Rock",
                "London",
                "GB",
                "O2 Academy Brixton",
                "",
                "2026-06-01",
                "20:00",
                "2026-06-01T19:00:00Z",
                "music",
                "2026-05-05T10:00:00Z"
        ));

        datamart.upsertTransport(transport("k1", "Brixton Underground Station", "BrixtonAcademy", 11));
        datamart.upsertTransport(transport("k2", "Victoria Station", "Victoria", 25));

        ConcertSearchTransportResponse response = service.recommendationsForSearch("example");

        assertTrue(response.found());
        assertEquals(1, response.matches());
        assertEquals("Example", response.results().get(0).concert().name());
        assertTrue(response.results().get(0).venueMatch());
        assertEquals(1, response.results().get(0).routes().size());
        assertTrue(response.results().get(0).routes().get(0).destinationName().contains("Brixton"));
    }

    @Test
    void recommendationsForSearchCanReturnFallbackForMatchingConcertWithoutVenueRoute() {
        datamart.upsertConcert(new ConcertRecord(
                "c1",
                "Chase and Status",
                "music",
                "Music",
                "Pop",
                "London",
                "GB",
                "Magazine",
                "",
                "2026-06-01",
                "20:00",
                "2026-06-01T19:00:00Z",
                "music",
                "2026-05-05T10:00:00Z"
        ));

        datamart.upsertTransport(transport("k1", "Victoria Station", "Victoria", 25));

        ConcertSearchTransportResponse response = service.recommendationsForSearch("chase");

        assertTrue(response.found());
        assertEquals(1, response.matches());
        assertFalse(response.results().get(0).venueMatch());
        assertEquals(1, response.results().get(0).routes().size());
    }

    @Test
    void transportForConcertLimitsMatchedRoutesToTenBestOptions() {
        datamart.upsertConcert(new ConcertRecord(
                "c1",
                "Example",
                "music",
                "Music",
                "Rock",
                "London",
                "GB",
                "O2 Academy Brixton",
                "",
                "2026-06-01",
                "20:00",
                "2026-06-01T19:00:00Z",
                "music",
                "2026-05-05T10:00:00Z"
        ));

        for (int i = 1; i <= 12; i++) {
            datamart.upsertTransport(transport(
                    "brixton-" + i,
                    "Brixton Underground Station",
                    "BrixtonAcademy",
                    i
            ));
        }

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.found());
        assertTrue(response.venueMatch());
        assertEquals(10, response.routes().size());
        assertEquals(1, response.routes().get(0).durationMinutes());
        assertEquals(10, response.routes().get(9).durationMinutes());
    }

    @Test
    void transportForConcertLimitsFallbackRoutesToTenBestOptions() {
        datamart.upsertConcert(new ConcertRecord(
                "c1",
                "Chase and Status",
                "music",
                "Music",
                "Pop",
                "London",
                "GB",
                "Magazine",
                "",
                "2026-06-01",
                "20:00",
                "2026-06-01T19:00:00Z",
                "music",
                "2026-05-05T10:00:00Z"
        ));

        for (int i = 1; i <= 12; i++) {
            datamart.upsertTransport(transport(
                    "route-" + i,
                    "Generic Destination " + i,
                    "GenericDestination" + i,
                    i
            ));
        }

        ConcertTransportResponse response = service.transportForConcert("c1");

        assertTrue(response.found());
        assertFalse(response.venueMatch());
        assertEquals(10, response.routes().size());
        assertEquals(1, response.routes().get(0).durationMinutes());
        assertEquals(10, response.routes().get(9).durationMinutes());
    }
}