package org.ulpgc.paradiso.businessunit.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;

import static org.junit.jupiter.api.Assertions.*;

class BusinessEventProcessorTest {

    private Datamart datamart;
    private BusinessEventProcessor processor;

    private static final String TM_FULL = """
            {"ts":"2026-05-05T17:30:00Z","ss":"ticketmaster-module","payload":{
            "externalEventId":"abc123","name":"ZAZ","classificationName":"music",
            "segment":"Music","genre":"Rock","city":"London","countryCode":"GB",
            "venueName":"O2 Academy Brixton","eventUrl":"https://ticketmaster.co.uk/x",
            "localDate":"2026-05-05","localTime":"18:30:00",
            "dateTimeIso":"2026-05-05T17:30:00Z","sourceCategory":"music"}}
            """;

    private static final String TFL_FULL = """
            {"ts":"2026-05-05T10:46:09Z","ss":"tfl-module","payload":{
            "journeyHash":"hash1","originName":"King's Cross","destinationName":"North Greenwich",
            "startDateTime":"2026-05-05T09:00","arrivalDateTime":"2026-05-05T09:27",
            "durationMinutes":27,"numberOfLegs":2,"firstLegMode":"tube",
            "captureDate":"2026-05-05","captureTime":"0900",
            "sourceOrigin":"KingsCross","sourceDestination":"O2Arena"}}
            """;

    @BeforeEach
    void setUp() {
        datamart = new Datamart();
        processor = new BusinessEventProcessor(datamart);
    }

    @Test
    void processesConcertIntoDatamart() {
        processor.process("TicketmasterEvent", TM_FULL);

        assertEquals(1, datamart.concertCount());
    }

    @Test
    void concertHasCorrectFields() {
        processor.process("TicketmasterEvent", TM_FULL);

        var concert = datamart.concertById("abc123");

        assertTrue(concert.isPresent());
        assertEquals("ZAZ", concert.get().name());
        assertEquals("London", concert.get().city());
        assertEquals("O2 Academy Brixton", concert.get().venueName());
        assertEquals("Rock", concert.get().genre());
        assertEquals("2026-05-05T17:30:00Z", concert.get().dateTimeIso());
    }

    @Test
    void usesCityFieldNotVenueCity() {
        String json = """
                {"ts":"2026-05-05T10:00:00Z","ss":"ss","payload":{
                "externalEventId":"x1","city":"London","venueCity":"WRONG_FIELD"}}
                """;

        processor.process("TicketmasterEvent", json);

        assertEquals("London", datamart.concertById("x1").get().city());
    }

    @Test
    void ignoresConcertWithoutExternalEventId() {
        String json = """
                {"ts":"2026-05-05T10:00:00Z","ss":"ss","payload":{"name":"ZAZ","city":"London"}}
                """;

        processor.process("TicketmasterEvent", json);

        assertEquals(0, datamart.concertCount());
    }

    @Test
    void processesTransportIntoDatamart() {
        processor.process("TflJourney", TFL_FULL);

        assertEquals(1, datamart.transportCount());
    }

    @Test
    void transportHasCorrectFields() {
        processor.process("TflJourney", TFL_FULL);

        var transports = datamart.transports();

        assertFalse(transports.isEmpty());

        var t = transports.get(0);

        assertEquals("North Greenwich", t.destinationName());
        assertEquals(27, t.durationMinutes());
        assertEquals("tube", t.firstLegMode());
        assertEquals("O2Arena", t.sourceDestination());
    }

    @Test
    void transportJourneyKeyIsComposite() {
        processor.process("TflJourney", TFL_FULL);

        var t = datamart.transports().get(0);

        assertTrue(t.journeyKey().contains("hash1"));
        assertTrue(t.journeyKey().contains("2026-05-05"));
    }

    @Test
    void ignoresInvalidJsonWithoutException() {
        assertDoesNotThrow(() -> processor.process("TicketmasterEvent", "not valid json"));

        assertEquals(0, datamart.concertCount());
    }

    @Test
    void ignoresEventWithoutPayload() {
        String json = "{\"ts\":\"2026-05-05T10:00:00Z\",\"ss\":\"ss\"}";

        assertDoesNotThrow(() -> processor.process("TicketmasterEvent", json));

        assertEquals(0, datamart.concertCount());
    }

    @Test
    void ignoresEventWithNullPayload() {
        String json = "{\"ts\":\"2026-05-05T10:00:00Z\",\"ss\":\"ss\",\"payload\":null}";

        assertDoesNotThrow(() -> processor.process("TicketmasterEvent", json));

        assertEquals(0, datamart.concertCount());
    }

    @Test
    void ignoresEmptyLine() {
        assertDoesNotThrow(() -> processor.process("TicketmasterEvent", ""));

        assertEquals(0, datamart.concertCount());
    }

    @Test
    void ignoresUnknownTopic() {
        assertDoesNotThrow(() -> processor.process("UnknownTopic", TM_FULL));

        assertEquals(0, datamart.concertCount());
        assertEquals(0, datamart.transportCount());
    }
}