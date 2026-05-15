package org.ulpgc.paradiso.ticketmaster.mapper;

import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterEvent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketmasterEventMapperTest {

    private static final String COMPLETE_EVENT_JSON = """
        {
          "_embedded": { "events": [{
            "id": "TM-001",
            "name": "Rock Night London",
            "url": "https://ticketmaster.com/event/TM-001",
            "classifications": [{"segment":{"name":"Music"},"genre":{"name":"Rock"}}],
            "dates": {"start": {
              "localDate":"2026-07-15","localTime":"20:00:00","dateTime":"2026-07-15T19:00:00Z"
            }},
            "_embedded": {"venues": [{
              "name":"The O2 Arena",
              "city":{"name":"London"},
              "country":{"countryCode":"GB"}
            }]}
          }]}
        }""";

    private static final String JSON_WITHOUT_EVENTS = "{}";

    private static final String PARTIAL_EVENT_JSON = """
        {"_embedded": {"events": [{"id":"TM-002","name":"Without venue"}]}}""";

    @Test
    void mapsCompleteJsonToExpectedEvent() {
        TicketmasterEventMapper mapper = new TicketmasterEventMapper();

        List<TicketmasterEvent> result = mapper.map(
                COMPLETE_EVENT_JSON,
                new TicketmasterCaptureContext(
                        "GB",
                        "London",
                        "music",
                        "batch-1",
                        "2026-04-01T00:00:00Z"
                )
        );

        assertEquals(1, result.size());

        TicketmasterEvent event = result.get(0);

        assertEquals("TM-001", event.getExternalEventId());
        assertEquals("Rock Night London", event.getName());
        assertEquals("London", event.getCity());
        assertEquals("GB", event.getCountryCode());
        assertEquals("2026-07-15", event.getLocalDate());
        assertEquals("20:00:00", event.getLocalTime());
        assertEquals("Music", event.getSegment());
        assertEquals("Rock", event.getGenre());
        assertEquals("The O2 Arena", event.getVenueName());
        assertEquals("batch-1", event.getCaptureBatchId());
        assertEquals("GB", event.getSourceCountry());
        assertEquals("music", event.getSourceCategory());
    }

    @Test
    void returnsEmptyListWhenEmbeddedEventsSectionIsMissing() {
        TicketmasterEventMapper mapper = new TicketmasterEventMapper();

        List<TicketmasterEvent> result = mapper.map(
                JSON_WITHOUT_EVENTS,
                new TicketmasterCaptureContext(
                        "GB",
                        "London",
                        "music",
                        "batch-empty",
                        "2026-04-01T00:00:00Z"
                )
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void mapsPartialEventWithoutAbortingCapture() {
        TicketmasterEventMapper mapper = new TicketmasterEventMapper();

        List<TicketmasterEvent> result = mapper.map(
                PARTIAL_EVENT_JSON,
                new TicketmasterCaptureContext(
                        "GB",
                        "London",
                        "music",
                        "batch-2",
                        "2026-04-01T00:00:00Z"
                )
        );

        assertEquals(1, result.size());
        assertEquals("TM-002", result.get(0).getExternalEventId());
        assertNull(result.get(0).getCity());
        assertNull(result.get(0).getVenueName());
    }
}
