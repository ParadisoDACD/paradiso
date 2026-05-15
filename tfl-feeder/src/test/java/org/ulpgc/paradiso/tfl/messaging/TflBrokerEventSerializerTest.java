package org.ulpgc.paradiso.tfl.messaging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.tfl.model.TflJourney;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TflBrokerEventSerializerTest {

    private final TflBrokerEventSerializer serializer =
            new TflBrokerEventSerializer("tfl-feeder");

    private TflJourney journey() {
        TflJourney journey = new TflJourney();
        journey.setCapturedAt("2026-05-05T10:00:00Z");
        journey.setStartDateTime("2026-05-05T09:00:00");
        journey.setOriginName("King's Cross St. Pancras Underground Station");
        journey.setDestinationName("North Greenwich Underground Station");
        return journey;
    }

    @Test
    void serializeUsesCapturedAtAsTs() {
        JsonObject root = JsonParser.parseString(serializer.serialize(journey())).getAsJsonObject();

        assertEquals("2026-05-05T10:00:00Z", root.get("ts").getAsString());
    }

    @Test
    void serializeDoesNotUseStartDateTimeAsTs() {
        JsonObject root = JsonParser.parseString(serializer.serialize(journey())).getAsJsonObject();

        assertNotEquals("2026-05-05T09:00:00", root.get("ts").getAsString());
    }

    @Test
    void serializeIncludesConfiguredSourceSystem() {
        JsonObject root = JsonParser.parseString(serializer.serialize(journey())).getAsJsonObject();

        assertEquals("tfl-feeder", root.get("ss").getAsString());
    }

    @Test
    void serializeIncludesPayload() {
        JsonObject root = JsonParser.parseString(serializer.serialize(journey())).getAsJsonObject();

        assertTrue(root.has("payload"));
        assertTrue(root.get("payload").isJsonObject());
    }

    @Test
    void serializedTsIsParseableAsInstant() {
        JsonObject root = JsonParser.parseString(serializer.serialize(journey())).getAsJsonObject();
        String ts = root.get("ts").getAsString();

        assertDoesNotThrow(() -> Instant.parse(ts));
    }
}