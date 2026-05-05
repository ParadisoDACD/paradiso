package org.ulpgc.paradiso.ticketmaster.messaging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterEvent;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TicketmasterBrokerEventSerializerTest {

    private final TicketmasterBrokerEventSerializer serializer =
            new TicketmasterBrokerEventSerializer("ticketmaster-module");

    private TicketmasterEvent event(String dateTimeIso, String capturedAt) {
        TicketmasterEvent event = new TicketmasterEvent();
        event.setDateTimeIso(dateTimeIso);
        event.setCapturedAt(capturedAt);
        event.setName("Test Concert");
        return event;
    }

    @Test
    void serializeWithDateTimeIsoUsesDateTimeIsoAsTs() {
        TicketmasterEvent event = event("2026-06-15T20:00:00Z", "2026-05-05T10:00:00Z");

        JsonObject root = JsonParser.parseString(serializer.serialize(event)).getAsJsonObject();

        assertEquals("2026-06-15T20:00:00Z", root.get("ts").getAsString());
    }

    @Test
    void serializeWithoutDateTimeIsoUsesCapturedAtAsFallback() {
        TicketmasterEvent event = event(null, "2026-05-05T10:00:00Z");

        JsonObject root = JsonParser.parseString(serializer.serialize(event)).getAsJsonObject();

        assertEquals("2026-05-05T10:00:00Z", root.get("ts").getAsString());
    }

    @Test
    void serializeWithBlankDateTimeIsoUsesCapturedAtAsFallback() {
        TicketmasterEvent event = event("   ", "2026-05-05T10:00:00Z");

        JsonObject root = JsonParser.parseString(serializer.serialize(event)).getAsJsonObject();

        assertEquals("2026-05-05T10:00:00Z", root.get("ts").getAsString());
    }

    @Test
    void serializeIncludesConfiguredSourceSystem() {
        TicketmasterEvent event = event("2026-06-15T20:00:00Z", "2026-05-05T10:00:00Z");

        JsonObject root = JsonParser.parseString(serializer.serialize(event)).getAsJsonObject();

        assertEquals("ticketmaster-module", root.get("ss").getAsString());
    }

    @Test
    void serializeIncludesPayload() {
        TicketmasterEvent event = event("2026-06-15T20:00:00Z", "2026-05-05T10:00:00Z");

        JsonObject root = JsonParser.parseString(serializer.serialize(event)).getAsJsonObject();

        assertTrue(root.has("payload"));
        assertTrue(root.get("payload").isJsonObject());
    }

    @Test
    void serializedTsIsParseableAsInstant() {
        TicketmasterEvent event = event("2026-06-15T20:00:00Z", "2026-05-05T10:00:00Z");

        JsonObject root = JsonParser.parseString(serializer.serialize(event)).getAsJsonObject();
        String ts = root.get("ts").getAsString();

        assertDoesNotThrow(() -> Instant.parse(ts));
    }
}