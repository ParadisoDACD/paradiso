package org.ulpgc.paradiso.businessunit.loader;
import org.ulpgc.paradiso.businessunit.recommendation.RecommendationBuilder;
import org.ulpgc.paradiso.businessunit.recommendation.RouteScoringService;
import org.ulpgc.paradiso.businessunit.service.BusinessIngestionService;
import org.ulpgc.paradiso.businessunit.venue.VenueNormalizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.event.BusinessEventProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventStoreLoaderTest {

    @TempDir
    Path tempDir;

    private Datamart datamart;
    private BusinessEventProcessor processor;

    private static final String TM_LINE =
            "{\"ts\":\"2026-05-05T17:30:00Z\",\"ss\":\"ticketmaster-module\","
                    + "\"payload\":{\"externalEventId\":\"c001\",\"name\":\"ZAZ\","
                    + "\"city\":\"London\",\"venueName\":\"O2 Academy Brixton\"}}";

    private static final String TFL_LINE =
            "{\"ts\":\"2026-05-05T10:46:09Z\",\"ss\":\"tfl-module\","
                    + "\"payload\":{\"journeyHash\":\"h1\",\"originName\":\"King's Cross\","
                    + "\"destinationName\":\"North Greenwich\",\"durationMinutes\":27,"
                    + "\"captureDate\":\"2026-05-05\",\"captureTime\":\"0900\","
                    + "\"startDateTime\":\"2026-05-05T09:00\","
                    + "\"arrivalDateTime\":\"2026-05-05T09:27\"}}";

    @BeforeEach
    void setUp() {
        datamart = new Datamart();
        processor = processorFor(datamart);
    }

    private BusinessEventProcessor processorFor(Datamart datamart) {
        VenueNormalizer venueNormalizer = new VenueNormalizer();
        RouteScoringService scoringService = new RouteScoringService();
        RecommendationBuilder recommendationBuilder = new RecommendationBuilder(
                datamart,
                venueNormalizer,
                scoringService
        );
        BusinessIngestionService ingestionService = new BusinessIngestionService(
                datamart,
                recommendationBuilder
        );
        return new BusinessEventProcessor(ingestionService);
    }

    private void createEventsFile(String topic,
                                  String sourceSystem,
                                  String filename,
                                  String... lines) throws IOException {
        Path dir = tempDir.resolve(topic).resolve(sourceSystem);
        Files.createDirectories(dir);

        Path file = dir.resolve(filename);
        Files.writeString(file, String.join("\n", lines) + "\n");
    }

    @Test
    void loadsTicketmasterEventsFromFile() throws IOException {
        createEventsFile("TicketmasterEvent", "ticketmaster-module", "20260505.events", TM_LINE);

        EventStoreLoader loader = new EventStoreLoader(
                tempDir.toString(),
                List.of("TicketmasterEvent"),
                processor
        );

        int loaded = loader.loadAll();

        assertEquals(1, loaded);
        assertEquals(1, datamart.concertCount());
        assertTrue(datamart.concertById("c001").isPresent());
    }

    @Test
    void loadsTflEventsFromFile() throws IOException {
        createEventsFile("TflJourney", "tfl-module", "20260505.events", TFL_LINE);

        EventStoreLoader loader = new EventStoreLoader(
                tempDir.toString(),
                List.of("TflJourney"),
                processor
        );

        int loaded = loader.loadAll();

        assertEquals(1, loaded);
        assertEquals(1, datamart.transportCount());
    }

    @Test
    void loadsBothTopics() throws IOException {
        createEventsFile("TicketmasterEvent", "ticketmaster-module", "20260505.events", TM_LINE);
        createEventsFile("TflJourney", "tfl-module", "20260505.events", TFL_LINE);

        EventStoreLoader loader = new EventStoreLoader(
                tempDir.toString(),
                List.of("TicketmasterEvent", "TflJourney"),
                processor
        );

        int loaded = loader.loadAll();

        assertEquals(2, loaded);
        assertEquals(1, datamart.concertCount());
        assertEquals(1, datamart.transportCount());
    }

    @Test
    void loadsMultipleFilesInChronologicalOrder() throws IOException {
        createEventsFile(
                "TicketmasterEvent",
                "ticketmaster-module",
                "20260504.events",
                "{\"ts\":\"2026-05-04T10:00:00Z\",\"ss\":\"tm\","
                        + "\"payload\":{\"externalEventId\":\"old\",\"name\":\"Old Event\"}}"
        );

        createEventsFile(
                "TicketmasterEvent",
                "ticketmaster-module",
                "20260505.events",
                "{\"ts\":\"2026-05-05T10:00:00Z\",\"ss\":\"tm\","
                        + "\"payload\":{\"externalEventId\":\"new\",\"name\":\"New Event\"}}"
        );

        EventStoreLoader loader = new EventStoreLoader(
                tempDir.toString(),
                List.of("TicketmasterEvent"),
                processor
        );

        int loaded = loader.loadAll();

        assertEquals(2, loaded);
        assertEquals(2, datamart.concertCount());
    }

    @Test
    void loadsMultipleLinesFromSingleFile() throws IOException {
        String secondLine =
                "{\"ts\":\"2026-05-05T18:00:00Z\",\"ss\":\"ticketmaster-module\","
                        + "\"payload\":{\"externalEventId\":\"c002\",\"name\":\"Chase and Status\"}}";

        createEventsFile(
                "TicketmasterEvent",
                "ticketmaster-module",
                "20260505.events",
                TM_LINE,
                secondLine
        );

        EventStoreLoader loader = new EventStoreLoader(
                tempDir.toString(),
                List.of("TicketmasterEvent"),
                processor
        );

        int loaded = loader.loadAll();

        assertEquals(2, loaded);
        assertEquals(2, datamart.concertCount());
    }

    @Test
    void doesNotFailWhenEventstoreDirectoryMissing() {
        EventStoreLoader loader = new EventStoreLoader(
                "/ruta/que/no/existe",
                List.of("TicketmasterEvent"),
                processor
        );

        int loaded = assertDoesNotThrow(loader::loadAll);

        assertEquals(0, loaded);
        assertEquals(0, datamart.concertCount());
    }

    @Test
    void doesNotFailWhenTopicDirectoryMissing() {
        EventStoreLoader loader = new EventStoreLoader(
                tempDir.toString(),
                List.of("TicketmasterEvent"),
                processor
        );

        int loaded = assertDoesNotThrow(loader::loadAll);

        assertEquals(0, loaded);
        assertEquals(0, datamart.concertCount());
    }

    @Test
    void skipsBlankLines() throws IOException {
        createEventsFile(
                "TicketmasterEvent",
                "ticketmaster-module",
                "20260505.events",
                "",
                "   ",
                TM_LINE,
                ""
        );

        EventStoreLoader loader = new EventStoreLoader(
                tempDir.toString(),
                List.of("TicketmasterEvent"),
                processor
        );

        int loaded = loader.loadAll();

        assertEquals(1, loaded);
        assertEquals(1, datamart.concertCount());
    }

    @Test
    void skipsInvalidJsonLinesWithoutException() throws IOException {
        createEventsFile(
                "TicketmasterEvent",
                "ticketmaster-module",
                "20260505.events",
                "esto no es json",
                TM_LINE
        );

        EventStoreLoader loader = new EventStoreLoader(
                tempDir.toString(),
                List.of("TicketmasterEvent"),
                processor
        );

        int loaded = assertDoesNotThrow(loader::loadAll);

        assertEquals(2, loaded);
        assertEquals(1, datamart.concertCount());
    }

    @Test
    void returnsCorrectCountIncludingInvalidLines() throws IOException {
        createEventsFile(
                "TicketmasterEvent",
                "ticketmaster-module",
                "20260505.events",
                "invalid",
                TM_LINE
        );

        EventStoreLoader loader = new EventStoreLoader(
                tempDir.toString(),
                List.of("TicketmasterEvent"),
                processor
        );

        int loaded = loader.loadAll();

        assertEquals(2, loaded);
    }
}