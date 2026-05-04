package org.ulpgc.paradiso.tfl.controller;

import org.ulpgc.paradiso.tfl.config.TflConfig;
import org.ulpgc.paradiso.tfl.feeder.JourneyFeeder;
import org.ulpgc.paradiso.tfl.feeder.TflVenueResolver;
import org.ulpgc.paradiso.tfl.mapper.TflJourneyMapper;
import org.ulpgc.paradiso.tfl.model.TflCaptureRun;
import org.ulpgc.paradiso.tfl.model.TflJourney;
import org.ulpgc.paradiso.tfl.persistence.JourneyStore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TflController {

    private static final DateTimeFormatter TFL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TflConfig config;
    private final JourneyFeeder feeder;
    private final TflJourneyMapper mapper;
    private final JourneyStore store;

    public TflController(TflConfig config,
                         JourneyFeeder feeder,
                         TflJourneyMapper mapper,
                         JourneyStore store) {
        this.config = config;
        this.feeder = feeder;
        this.mapper = mapper;
        this.store = store;
    }

    public void executeCapture() {
        String batchId = UUID.randomUUID().toString();
        String capturedAt = Instant.now().toString();

        System.out.println("\n[TfL] ======== Iniciando captura ========");
        System.out.println("[TfL] Lote: " + batchId);

        TflCaptureRun run = new TflCaptureRun();
        run.setCaptureBatchId(batchId);
        run.setStartedAt(capturedAt);
        run.setStatus("RUNNING");
        run.setScopeSummary(config.getRoutes().toString()
                + " | franjas: " + config.getCaptureTimes());

        try {
            store.startRun(run);

            List<TflJourney> allJourneys = new ArrayList<>();
            int totalFetched = 0;

            for (int dayOffset = 0; dayOffset < 2; dayOffset++) {
                LocalDate date = LocalDate.now().plusDays(dayOffset);
                String dateStr = date.format(TFL_DATE_FORMAT);
                String captureDate = date.toString();

                for (String[] route : config.getRoutes()) {
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

                    for (String captureTime : config.getCaptureTimes()) {
                        try {
                            String raw = feeder.fetchRawJourneys(
                                    fromNaptan,
                                    toNaptan,
                                    dateStr,
                                    captureTime.trim()
                            );

                            List<TflJourney> batch = mapper.map(
                                    raw,
                                    originName,
                                    destName,
                                    captureDate,
                                    captureTime.trim(),
                                    batchId,
                                    capturedAt
                            );

                            totalFetched += batch.size();
                            allJourneys.addAll(batch);

                            System.out.printf("  [%s -> %s] %s %s -> %d itinerarios%n",
                                    originName,
                                    destName,
                                    captureDate,
                                    captureTime.trim(),
                                    batch.size());

                            Thread.sleep(200);

                        } catch (Exception e) {
                            System.err.println("  [TfL] Error ["
                                    + originName + " -> " + destName + "] "
                                    + captureDate + " " + captureTime + ": "
                                    + e.getMessage());
                        }
                    }
                }
            }

            store.saveAll(allJourneys);
            store.finishRun(batchId, totalFetched, allJourneys.size());

            System.out.printf("[TfL] Captura OK - %d itinerarios insertados%n",
                    allJourneys.size());

        } catch (Exception e) {
            System.err.println("[TfL] ERROR FATAL: " + e.getMessage());
            try {
                store.failRun(batchId, e.getMessage());
            } catch (Exception ignored) {
            }
        }
    }
}