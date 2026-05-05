package org.ulpgc.paradiso.eventstorebuilder.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonLinesEventFileStoreTest {

    @TempDir
    Path tempDir;

    private JsonLinesEventFileStore store;

    @BeforeEach
    void setUp() {
        store = new JsonLinesEventFileStore(tempDir.toString());
    }

    private String json(String ts, String ss, String name) {
        return String.format(
                "{\"ts\":\"%s\",\"ss\":\"%s\",\"payload\":{\"name\":\"%s\"}}",
                ts, ss, name
        );
    }

    private Path file(String topic, String ss, String yyyymmdd) {
        return tempDir.resolve(topic).resolve(ss).resolve(yyyymmdd + ".events");
    }

    @Test
    void appendValidEventCreatesExpectedFile() throws Exception {
        store.append("TicketmasterEvent",
                json("2026-06-15T20:00:00Z", "ticketmaster-module", "Concert A"));

        assertTrue(Files.exists(file("TicketmasterEvent", "ticketmaster-module", "20260615")));
    }

    @Test
    void appendValidEventWritesOriginalJsonLine() throws Exception {
        String event = json("2026-06-15T20:00:00Z", "ticketmaster-module", "Concert A");

        store.append("TicketmasterEvent", event);

        List<String> lines = Files.readAllLines(
                file("TicketmasterEvent", "ticketmaster-module", "20260615"));

        assertEquals(1, lines.size());
        assertEquals(event, lines.get(0));
    }

    @Test
    void appendSeveralEventsInSameDayAppendsLines() throws Exception {
        String event1 = json("2026-06-15T20:00:00Z", "ticketmaster-module", "Concert A");
        String event2 = json("2026-06-15T21:00:00Z", "ticketmaster-module", "Concert B");

        store.append("TicketmasterEvent", event1);
        store.append("TicketmasterEvent", event2);

        List<String> lines = Files.readAllLines(
                file("TicketmasterEvent", "ticketmaster-module", "20260615"));

        assertEquals(2, lines.size());
        assertEquals(event1, lines.get(0));
        assertEquals(event2, lines.get(1));
    }

    @Test
    void appendEventsFromDifferentDaysCreatesDifferentFiles() throws Exception {
        store.append("TicketmasterEvent",
                json("2026-06-15T20:00:00Z", "ticketmaster-module", "Concert A"));
        store.append("TicketmasterEvent",
                json("2026-06-16T20:00:00Z", "ticketmaster-module", "Concert B"));

        assertTrue(Files.exists(file("TicketmasterEvent", "ticketmaster-module", "20260615")));
        assertTrue(Files.exists(file("TicketmasterEvent", "ticketmaster-module", "20260616")));
    }

    @Test
    void appendDifferentTopicsCreatesDifferentDirectories() throws Exception {
        store.append("TicketmasterEvent",
                json("2026-06-15T20:00:00Z", "ticketmaster-module", "Concert A"));
        store.append("TflJourney",
                json("2026-06-15T10:00:00Z", "tfl-module", "Journey A"));

        assertTrue(Files.exists(file("TicketmasterEvent", "ticketmaster-module", "20260615")));
        assertTrue(Files.exists(file("TflJourney", "tfl-module", "20260615")));
    }

    @Test
    void appendEventWithoutTsThrowsException() {
        String event = "{\"ss\":\"ticketmaster-module\",\"payload\":{\"name\":\"No ts\"}}";

        assertThrows(IllegalArgumentException.class,
                () -> store.append("TicketmasterEvent", event));
    }

    @Test
    void appendEventWithoutSsThrowsException() {
        String event = "{\"ts\":\"2026-06-15T20:00:00Z\",\"payload\":{\"name\":\"No ss\"}}";

        assertThrows(IllegalArgumentException.class,
                () -> store.append("TicketmasterEvent", event));
    }

    @Test
    void appendEventWithInvalidTsThrowsException() {
        String event = "{\"ts\":\"2026-06-15T20:00:00\",\"ss\":\"ticketmaster-module\",\"payload\":{}}";

        assertThrows(Exception.class,
                () -> store.append("TicketmasterEvent", event));
    }

    @Test
    void appendSanitizesTopicAndSourceSystem() throws Exception {
        store.append("Topic con espacios!",
                json("2026-06-15T20:00:00Z", "mi modulo/v2", "Test"));

        Path sanitizedDirectory = tempDir
                .resolve("Topic_con_espacios_")
                .resolve("mi_modulo_v2");

        assertTrue(Files.exists(sanitizedDirectory));
    }

    @Test
    void appendAfterCreatingNewStoreInstanceKeepsAppending() throws Exception {
        String event1 = json("2026-06-15T20:00:00Z", "ticketmaster-module", "Concert A");
        String event2 = json("2026-06-15T21:00:00Z", "ticketmaster-module", "Concert B");

        store.append("TicketmasterEvent", event1);

        JsonLinesEventFileStore anotherStore = new JsonLinesEventFileStore(tempDir.toString());
        anotherStore.append("TicketmasterEvent", event2);

        List<String> lines = Files.readAllLines(
                file("TicketmasterEvent", "ticketmaster-module", "20260615"));

        assertEquals(2, lines.size());
        assertEquals(event1, lines.get(0));
        assertEquals(event2, lines.get(1));
    }
}