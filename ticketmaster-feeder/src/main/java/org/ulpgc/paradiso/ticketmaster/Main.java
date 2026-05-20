package org.ulpgc.paradiso.ticketmaster;

import org.ulpgc.paradiso.ticketmaster.config.TicketmasterConfig;
import org.ulpgc.paradiso.ticketmaster.controller.TicketmasterController;
import org.ulpgc.paradiso.ticketmaster.feeder.TicketmasterDiscoveryFeeder;
import org.ulpgc.paradiso.ticketmaster.mapper.TicketmasterEventMapper;
import org.ulpgc.paradiso.ticketmaster.messaging.ActivemqEventPublisher;
import org.ulpgc.paradiso.ticketmaster.messaging.TicketmasterBrokerEventSerializer;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        boolean onceMode = Arrays.asList(args).contains("--once");

        TicketmasterConfig config = new TicketmasterConfig();

        System.out.println("[Ticketmaster] Broker:  " + config.getBrokerUrl());
        System.out.println("[Ticketmaster] Topic:   " + config.getTopicName());
        System.out.println("[Ticketmaster] Source:  " + config.getSourceSystem());

        if (onceMode) {
            System.out.println("[Ticketmaster] Modo: one-shot");
            runOnce(config);
            System.out.println("[Ticketmaster] Finalizado.");
        } else {
            System.out.println("[Ticketmaster] Modo: periódico cada "
                    + config.getCapturePeriodMinutes() + " minutos");

            ScheduledExecutorService scheduler =
                    Executors.newSingleThreadScheduledExecutor();

            scheduler.scheduleAtFixedRate(
                    () -> {
                        try {
                            runOnce(config);
                        } catch (Exception e) {
                            System.err.println("[Ticketmaster] Error en ciclo periódico: "
                                    + e.getMessage());
                        }
                    },
                    0,
                    config.getCapturePeriodMinutes(),
                    TimeUnit.MINUTES
            );
        }
    }

    private static void runOnce(TicketmasterConfig config) throws Exception {
        try (ActivemqEventPublisher publisher = new ActivemqEventPublisher(
                config.getBrokerUrl(),
                config.getTopicName())) {

            TicketmasterController controller = new TicketmasterController(
                    config,
                    new TicketmasterDiscoveryFeeder(config.getApiKey(), config.getApiBaseUrl()),
                    new TicketmasterEventMapper(),
                    publisher,
                    new TicketmasterBrokerEventSerializer(config.getSourceSystem())
            );

            controller.executeCapture();
        }
    }
}