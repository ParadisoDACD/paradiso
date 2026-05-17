package org.ulpgc.paradiso.tfl.controller;

import org.ulpgc.paradiso.tfl.config.TflConfig;
import org.ulpgc.paradiso.tfl.feeder.TflJourneyRequest;
import org.ulpgc.paradiso.tfl.feeder.JourneyFeeder;
import org.ulpgc.paradiso.tfl.mapper.TflCaptureContext;
import org.ulpgc.paradiso.tfl.feeder.TflVenueResolver;
import org.ulpgc.paradiso.tfl.mapper.TflJourneyMapper;
import org.ulpgc.paradiso.tfl.messaging.EventPublisher;
import org.ulpgc.paradiso.tfl.messaging.TflBrokerEventSerializer;
import org.ulpgc.paradiso.tfl.model.TflJourney;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TflController {

    private static final DateTimeFormatter TFL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TflConfig config;
    private final JourneyFeeder feeder;
    private final TflJourneyMapper mapper;
    private final EventPublisher publisher;
    private final TflBrokerEventSerializer serializer;

    public TflController(TflConfig config,
                         JourneyFeeder feeder,
                         TflJourneyMapper mapper,
                         EventPublisher publisher,
                         TflBrokerEventSerializer serializer) {
        this.config = config;
        this.feeder = feeder;
        this.mapper = mapper;
        this.publisher = publisher;
        this.serializer = serializer;
    }

    public void executeCapture() {
        CaptureContext context = newCaptureContext();
        printCaptureStart(context);
        CaptureStats stats = publishConfiguredJourneys(context);
        printCaptureSummary(stats);
    }

    private CaptureContext newCaptureContext() {
        LocalDate baseDate = LocalDate.now();
        int startDayOffset = config.getCaptureStartDayOffset();
        int daysAhead = config.getCaptureDaysAhead();
        LocalDate firstCaptureDate = baseDate.plusDays(startDayOffset);
        LocalDate lastCaptureDate = baseDate.plusDays(startDayOffset + daysAhead - 1);
        return new CaptureContext(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                config.getRoutePairs(),
                config.getCaptureTimes(),
                startDayOffset,
                daysAhead,
                baseDate,
                firstCaptureDate,
                lastCaptureDate
        );
    }

    private void printCaptureStart(CaptureContext context) {
        System.out.println("\n[TfL] ======== Iniciando captura ========");
        System.out.println("[TfL] Lote: " + context.batchId());
        System.out.println("[TfL] Rutas configuradas: " + context.routePairs().size());
        System.out.println("[TfL] Horas por dia: " + context.captureTimes());
        System.out.println("[TfL] Ventana: desde D+" + context.startDayOffset()
                + " hasta D+" + (context.startDayOffset() + context.daysAhead() - 1)
                + " (" + context.firstCaptureDate() + " a " + context.lastCaptureDate() + ")");
    }

    private CaptureStats publishConfiguredJourneys(CaptureContext context) {
        CaptureStats stats = new CaptureStats();
        for (int dayOffset = context.startDayOffset();
             dayOffset < context.startDayOffset() + context.daysAhead();
             dayOffset++) {
            CaptureDay day = captureDay(context, dayOffset);
            if (!publishDayJourneys(context, day, stats)) {
                return stats;
            }
        }
        return stats;
    }

    private CaptureDay captureDay(CaptureContext context, int dayOffset) {
        LocalDate date = context.baseDate().plusDays(dayOffset);
        return new CaptureDay(date.format(TFL_DATE_FORMAT), date.toString());
    }

    private boolean publishDayJourneys(CaptureContext context, CaptureDay day, CaptureStats stats) {
        for (String[] route : context.routePairs()) {
            if (!publishRouteJourneys(context, day, route, stats)) {
                return false;
            }
        }
        return true;
    }

    private boolean publishRouteJourneys(CaptureContext context,
                                         CaptureDay day,
                                         String[] route,
                                         CaptureStats stats) {
        String originName = route[0];
        String destinationName = route[1];
        Optional<ResolvedRoute> resolvedRoute = resolveRoute(originName, destinationName);
        if (resolvedRoute.isEmpty()) {
            return true;
        }
        for (String captureTime : context.captureTimes()) {
            if (!publishRouteTime(context, day, resolvedRoute.get(), captureTime.trim(), stats)) {
                return false;
            }
        }
        return true;
    }

    private Optional<ResolvedRoute> resolveRoute(String originName, String destinationName) {
        try {
            return Optional.of(new ResolvedRoute(
                    originName, destinationName,
                    TflVenueResolver.resolve(originName),
                    TflVenueResolver.resolve(destinationName)
            ));
        } catch (IllegalArgumentException exception) {
            System.err.println("  [TfL] " + exception.getMessage() + " - ruta omitida.");
            return Optional.empty();
        }
    }

    private boolean publishRouteTime(CaptureContext context,
                                     CaptureDay day,
                                     ResolvedRoute route,
                                     String captureTime,
                                     CaptureStats stats) {
        try {
            return captureAndPublish(context, day, route, captureTime, stats);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("[TfL] Captura interrumpida.");
            return false;
        } catch (Exception exception) {
            printRouteError(day, route, captureTime, exception);
            return pauseBetweenRequests();
        }
    }

    private boolean captureAndPublish(CaptureContext context,
                                      CaptureDay day,
                                      ResolvedRoute route,
                                      String captureTime,
                                      CaptureStats stats) throws Exception {
        stats.registerRequest();
        List<TflJourney> journeys = fetchJourneys(context, day, route, captureTime);
        publishJourneys(journeys);
        stats.registerPublished(journeys.size());
        printRouteSummary(day, route, captureTime, journeys.size());
        return pauseBetweenRequests();
    }

    private List<TflJourney> fetchJourneys(CaptureContext context,
                                           CaptureDay day,
                                           ResolvedRoute route,
                                           String captureTime) throws Exception {
        TflJourneyRequest request = new TflJourneyRequest(
                route.fromNaptan(), route.toNaptan(), day.tflDate(), captureTime
        );
        String rawJson = feeder.fetchRawJourneys(request);
        TflCaptureContext mapperContext = new TflCaptureContext(
                route.originName(), route.destinationName(),
                day.isoDate(), captureTime, context.batchId(), context.capturedAt()
        );
        return mapper.map(rawJson, mapperContext);
    }

    private void publishJourneys(List<TflJourney> journeys) throws Exception {
        for (TflJourney journey : journeys) {
            publisher.publish(serializer.serialize(journey));
        }
    }

    private void printRouteSummary(CaptureDay day, ResolvedRoute route, String captureTime, int publishedCount) {
        System.out.printf("  [%s -> %s] %s %s -> %d itinerarios publicados%n",
                route.originName(), route.destinationName(),
                day.isoDate(), captureTime, publishedCount);
    }

    private void printRouteError(CaptureDay day, ResolvedRoute route, String captureTime, Exception exception) {
        System.err.println("  [TfL] Error ["
                + route.originName() + " -> " + route.destinationName() + "] "
                + day.isoDate() + " " + captureTime + ": " + exception.getMessage());
    }

    private boolean pauseBetweenRequests() {
        try {
            Thread.sleep(config.getRequestSleepMillis());
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("[TfL] Captura interrumpida durante la espera entre requests.");
            return false;
        }
    }

    private void printCaptureSummary(CaptureStats stats) {
        System.out.println("[TfL] Total de requests: " + stats.totalRequests());
        System.out.println("[TfL] Total publicado: " + stats.totalPublished()
                + " itinerarios en topic '" + config.getTopicName() + "'");
    }

    private record CaptureContext(String batchId,
                                  String capturedAt,
                                  List<String[]> routePairs,
                                  List<String> captureTimes,
                                  int startDayOffset,
                                  int daysAhead,
                                  LocalDate baseDate,
                                  LocalDate firstCaptureDate,
                                  LocalDate lastCaptureDate) {
    }

    private record CaptureDay(String tflDate, String isoDate) {
    }

    private record ResolvedRoute(String originName,
                                 String destinationName,
                                 String fromNaptan,
                                 String toNaptan) {
    }

    private static final class CaptureStats {

        private int totalPublished;
        private int totalRequests;

        private void registerRequest() {
            totalRequests++;
        }

        private void registerPublished(int publishedCount) {
            totalPublished += publishedCount;
        }

        private int totalPublished() {
            return totalPublished;
        }

        private int totalRequests() {
            return totalRequests;
        }
    }
}