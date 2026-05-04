package org.ulpgc.paradiso.ticketmaster.controller;

import org.ulpgc.paradiso.ticketmaster.config.TicketmasterConfig;
import org.ulpgc.paradiso.ticketmaster.feeder.EventFeeder;
import org.ulpgc.paradiso.ticketmaster.mapper.TicketmasterEventMapper;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterCaptureRun;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterEvent;
import org.ulpgc.paradiso.ticketmaster.persistence.EventStore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TicketmasterController {

    private final TicketmasterConfig config;
    private final EventFeeder feeder;
    private final TicketmasterEventMapper mapper;
    private final EventStore store;

    public TicketmasterController(TicketmasterConfig config,
                                  EventFeeder feeder,
                                  TicketmasterEventMapper mapper,
                                  EventStore store) {
        this.config = config;
        this.feeder = feeder;
        this.mapper = mapper;
        this.store = store;
    }

    public void executeCapture() {
        String batchId = UUID.randomUUID().toString();
        String capturedAt = Instant.now().toString();

        System.out.println("\n[Ticketmaster] ======== Iniciando captura ========");
        System.out.println("[Ticketmaster] Lote: " + batchId);

        TicketmasterCaptureRun run = new TicketmasterCaptureRun();
        run.setCaptureBatchId(batchId);
        run.setStartedAt(capturedAt);
        run.setStatus("RUNNING");
        run.setScopeSummary(config.getCountries() + " | "
                + config.getCities() + " | " + config.getCategories());

        try {
            store.startRun(run);

            String startDT = LocalDate.now()
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toString();

            String endDT = LocalDate.now()
                    .plusDays(config.getLookaheadDays())
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toString();

            List<TicketmasterEvent> allEvents = new ArrayList<>();
            int totalFetched = 0;

            for (String country : config.getCountries()) {
                for (String city : config.getCities()) {
                    for (String category : config.getCategories()) {
                        int page = 0;

                        while (true) {
                            try {
                                String raw = feeder.fetchRawEvents(
                                        country.trim(),
                                        city.trim(),
                                        category.trim(),
                                        startDT,
                                        endDT,
                                        page,
                                        20
                                );

                                List<TicketmasterEvent> batch = mapper.map(
                                        raw,
                                        country.trim(),
                                        city.trim(),
                                        category.trim(),
                                        batchId,
                                        capturedAt
                                );

                                if (batch.isEmpty()) {
                                    break;
                                }

                                totalFetched += batch.size();
                                allEvents.addAll(batch);

                                System.out.printf("  [%s/%s/%s] pag.%d -> %d eventos%n",
                                        country.trim(),
                                        city.trim(),
                                        category.trim(),
                                        page,
                                        batch.size());

                                page++;

                                Thread.sleep(250);

                            } catch (Exception e) {
                                System.err.println("  [TM] Error pag." + page
                                        + " [" + country + "/" + city + "]: "
                                        + e.getMessage());
                                break;
                            }
                        }
                    }
                }
            }

            store.saveAll(allEvents);
            store.finishRun(batchId, totalFetched, allEvents.size());

            System.out.printf("[Ticketmaster] Captura OK - %d eventos insertados%n",
                    allEvents.size());

        } catch (Exception e) {
            System.err.println("[Ticketmaster] ERROR FATAL: " + e.getMessage());
            try {
                store.failRun(batchId, e.getMessage());
            } catch (Exception ignored) {
            }
        }
    }
}