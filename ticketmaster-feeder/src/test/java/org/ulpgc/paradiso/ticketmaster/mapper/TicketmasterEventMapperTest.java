package org.ulpgc.paradiso.ticketmaster.mapper;

import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterEvent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TicketmasterEventMapperTest {

    private static final String JSON_COMPLETO = """
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

    private static final String JSON_SIN_EVENTOS = "{}";

    private static final String JSON_PARCIAL = """
        {"_embedded": {"events": [{"id":"TM-002","name":"Sin venue"}]}}""";

    @Test
    void mapeoCompletoDevuelveEventoCorrecto() {
        TicketmasterEventMapper mapper = new TicketmasterEventMapper();

        List<TicketmasterEvent> result = mapper.map(
                JSON_COMPLETO,
                "GB",
                "London",
                "music",
                "batch-1",
                "2026-04-01T00:00:00Z"
        );

        assertEquals(1, result.size());

        TicketmasterEvent ev = result.get(0);

        assertEquals("TM-001", ev.getExternalEventId());
        assertEquals("Rock Night London", ev.getName());
        assertEquals("London", ev.getCity());
        assertEquals("GB", ev.getCountryCode());
        assertEquals("2026-07-15", ev.getLocalDate());
        assertEquals("20:00:00", ev.getLocalTime());
        assertEquals("Music", ev.getSegment());
        assertEquals("Rock", ev.getGenre());
        assertEquals("The O2 Arena", ev.getVenueName());
        assertEquals("batch-1", ev.getCaptureBatchId());
        assertEquals("GB", ev.getSourceCountry());
        assertEquals("music", ev.getSourceCategory());
    }

    @Test
    void jsonSinEmbeddedRetornaListaVacia() {
        TicketmasterEventMapper mapper = new TicketmasterEventMapper();

        List<TicketmasterEvent> result = mapper.map(
                JSON_SIN_EVENTOS,
                "GB",
                "London",
                "music",
                "b",
                "2026-04-01T00:00:00Z"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void eventoParcialNoAbortaLaCaptura() {
        TicketmasterEventMapper mapper = new TicketmasterEventMapper();

        List<TicketmasterEvent> result = mapper.map(
                JSON_PARCIAL,
                "GB",
                "London",
                "music",
                "batch-2",
                "2026-04-01T00:00:00Z"
        );

        assertEquals(1, result.size());
        assertEquals("TM-002", result.get(0).getExternalEventId());
        assertNull(result.get(0).getCity());
        assertNull(result.get(0).getVenueName());
    }
}