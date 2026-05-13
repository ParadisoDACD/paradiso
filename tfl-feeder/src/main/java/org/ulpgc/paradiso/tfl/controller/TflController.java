package org.ulpgc.paradiso.tfl.controller;

import org.ulpgc.paradiso.tfl.config.TflConfig;
import org.ulpgc.paradiso.tfl.feeder.JourneyFeeder;
import org.ulpgc.paradiso.tfl.feeder.TflVenueResolver;
import org.ulpgc.paradiso.tfl.mapper.TflJourneyMapper;
import org.ulpgc.paradiso.tfl.messaging.EventPublisher;
import org.ulpgc.paradiso.tfl.messaging.TflBrokerEventSerializer;
import org.ulpgc.paradiso.tfl.model.TflJourney;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class TflController {

    private static final DateTimeFormatter TFL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

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
        String batchId = UUID.randomUUID().toString();
        String capturedAt = Instant.now().toString();
        List<String[]> routePairs = config.getRoutePairs();
        List<String> captureTimes = config.getCaptureTimes();
        int startDayOffset = config.getCaptureStartDayOffset();
        int daysAhead = config.getCaptureDaysAhead();
        LocalDate baseDate = LocalDate.now();

        LocalDate firstCaptureDate = baseDate.plusDays(startDayOffset);
        LocalDate lastCaptureDate = baseDate.plusDays(startDayOffset + daysAhead - 1);

        System.out.println("\n[TfL] ======== Iniciando captura ========");
        System.out.println("[TfL] Lote: " + batchId);
        System.out.println("[TfL] Rutas configuradas: " + routePairs.size());
        System.out.println("[TfL] Horas por dia: " + captureTimes);
        System.out.println("[TfL] Ventana: desde D+" + startDayOffset
                + " hasta D+" + (startDayOffset + daysAhead - 1)
                + " (" + firstCaptureDate + " a " + lastCaptureDate + ")");

        int totalPublished = 0;
        int totalRequests = 0;

        for (int dayOffset = startDayOffset; dayOffset < startDayOffset + daysAhead; dayOffset++) {
            LocalDate date = baseDate.plusDays(dayOffset);
            String dateStr = date.format(TFL_DATE_FORMAT);
            String captureDateIso = date.toString();

            for (String[] route : routePairs) {
                String originName = route[0];
                String destName = route[1];

                String fromNaptan;
                String toNaptan;

                try {
                    fromNaptan = TflVenueResolver.resolve(originName);
                    toNaptan = TflVenueResolver.resolve(destName);
                } catch (IllegalArgumentException e) {
                    System.err.println("  [TfL] " + e.getMessage() + " - ruta omitida.");
                    continue;
                }

                for (String captureTime : captureTimes) {
                    try {
                        totalRequests++;

                        String raw = feeder.fetchRawJourneys(
                                fromNaptan,
                                toNaptan,
                                dateStr,
                                captureTime.trim()
                        );

                        List<TflJourney> journeys = mapper.map(
                                raw,
                                originName,
                                destName,
                                captureDateIso,
                                captureTime.trim(),
                                batchId,
                                capturedAt
                        );

                        for (TflJourney journey : journeys) {
                            String jsonEvent = serializer.serialize(journey);
                            publisher.publish(jsonEvent);
                            totalPublished++;
                        }

                        System.out.printf("  [%s -> %s] %s %s -> %d itinerarios publicados%n",
                                originName,
                                destName,
                                captureDateIso,
                                captureTime.trim(),
                                journeys.size());

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("[TfL] Captura interrumpida.");
                        return;
                    } catch (Exception e) {
                        System.err.println("  [TfL] Error ["
                                + originName + " -> " + destName + "] "
                                + captureDateIso + " " + captureTime + ": "
                                + e.getMessage());
                    } finally {
                        if (!pauseBetweenRequests()) {
                            return;
                        }
                    }
                }
            }
        }

        System.out.println("[TfL] Total de requests: " + totalRequests);
        System.out.println("[TfL] Total publicado: " + totalPublished
                + " itinerarios en topic '" + config.getTopicName() + "'");
    }

    private boolean pauseBetweenRequests() {
        try {
            Thread.sleep(config.getRequestSleepMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[TfL] Captura interrumpida durante la espera entre requests.");
            return false;
        }
    }
}