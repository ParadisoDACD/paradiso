package org.ulpgc.paradiso.ticketmaster.controller;

import org.ulpgc.paradiso.ticketmaster.config.TicketmasterConfig;
import org.ulpgc.paradiso.ticketmaster.mapper.TicketmasterCaptureContext;
import org.ulpgc.paradiso.ticketmaster.feeder.EventFeeder;
import org.ulpgc.paradiso.ticketmaster.mapper.TicketmasterEventMapper;
import org.ulpgc.paradiso.ticketmaster.messaging.EventPublisher;
import org.ulpgc.paradiso.ticketmaster.messaging.TicketmasterBrokerEventSerializer;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterEvent;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class TicketmasterController {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .withZone(ZoneOffset.UTC);

    private final TicketmasterConfig config;
    private final EventFeeder feeder;
    private final TicketmasterEventMapper mapper;
    private final EventPublisher publisher;
    private final TicketmasterBrokerEventSerializer serializer;

    public TicketmasterController(TicketmasterConfig config,
                                  EventFeeder feeder,
                                  TicketmasterEventMapper mapper,
                                  EventPublisher publisher,
                                  TicketmasterBrokerEventSerializer serializer) {
        this.config = config;
        this.feeder = feeder;
        this.mapper = mapper;
        this.publisher = publisher;
        this.serializer = serializer;
    }

    public void executeCapture() {
        CaptureContext context = newCaptureContext();

        printCaptureStart(context);

        int totalPublished = publishConfiguredSearches(context);

        printCaptureSummary(totalPublished);
    }

    private CaptureContext newCaptureContext() {
        String capturedAt = Instant.now().toString();
        String batchId = UUID.randomUUID().toString();

        String startDateTime = ISO_UTC.format(ZonedDateTime.now(ZoneOffset.UTC));
        String endDateTime = ISO_UTC.format(
                ZonedDateTime.now(ZoneOffset.UTC).plusDays(config.getLookaheadDays())
        );

        return new CaptureContext(batchId, capturedAt, startDateTime, endDateTime);
    }

    private void printCaptureStart(CaptureContext context) {
        System.out.println("\n[Ticketmaster] ======== Iniciando captura ========");
        System.out.println("[Ticketmaster] Lote: " + context.batchId());
        System.out.println("[Ticketmaster] Ventana: "
                + context.startDateTime() + " -> " + context.endDateTime());
    }

    private int publishConfiguredSearches(CaptureContext context) {
        int totalPublished = 0;

        for (String country : config.getCountries()) {
            totalPublished += publishCountrySearches(context, country.trim());
        }

        return totalPublished;
    }

    private int publishCountrySearches(CaptureContext context, String country) {
        int totalPublished = 0;

        for (String city : config.getCities()) {
            totalPublished += publishCitySearches(context, country, city.trim());
        }

        return totalPublished;
    }

    private int publishCitySearches(CaptureContext context, String country, String city) {
        int totalPublished = 0;

        for (String category : config.getCategories()) {
            totalPublished += publishCategorySearch(context, country, city, category.trim());
        }

        return totalPublished;
    }

    private int publishCategorySearch(CaptureContext context,
                                      String country,
                                      String city,
                                      String category) {
        try {
            List<TicketmasterEvent> events = fetchEvents(context, country, city, category);
            publishEvents(events);
            printCategorySummary(country, city, category, events.size());
            return events.size();
        } catch (Exception exception) {
            printCaptureError(city, category, exception);
            return 0;
        }
    }

    private List<TicketmasterEvent> fetchEvents(CaptureContext context,
                                                String country,
                                                String city,
                                                String category) throws Exception {
        String rawJson = feeder.fetchRawEvents(
                country,
                city,
                category,
                context.startDateTime(),
                context.endDateTime(),
                0,
                50
        );

        TicketmasterCaptureContext mapperContext = new TicketmasterCaptureContext(
                country,
                city,
                category,
                context.batchId(),
                context.capturedAt()
        );

        return mapper.map(rawJson, mapperContext);
    }

    private void publishEvents(List<TicketmasterEvent> events) throws Exception {
        for (TicketmasterEvent event : events) {
            publisher.publish(serializer.serialize(event));
        }
    }

    private void printCategorySummary(String country, String city, String category, int publishedCount) {
        System.out.printf("  [%s/%s/%s] -> %d eventos publicados%n",
                country,
                city,
                category,
                publishedCount);
    }

    private void printCaptureError(String city, String category, Exception exception) {
        System.err.println("[Ticketmaster] Error captura ["
                + city + "/" + category + "]: " + exception.getMessage());
    }

    private void printCaptureSummary(int totalPublished) {
        System.out.println("[Ticketmaster] Total publicado: " + totalPublished
                + " eventos en topic '" + config.getTopicName() + "'");
    }

    private record CaptureContext(String batchId,
                                  String capturedAt,
                                  String startDateTime,
                                  String endDateTime) {
    }
}