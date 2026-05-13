package org.ulpgc.paradiso.tfl;

import org.ulpgc.paradiso.tfl.config.TflConfig;
import org.ulpgc.paradiso.tfl.controller.TflController;
import org.ulpgc.paradiso.tfl.feeder.TflJourneyFeeder;
import org.ulpgc.paradiso.tfl.mapper.TflJourneyMapper;
import org.ulpgc.paradiso.tfl.messaging.ActivemqEventPublisher;
import org.ulpgc.paradiso.tfl.messaging.TflBrokerEventSerializer;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        boolean onceMode = Arrays.asList(args).contains("--once");

        TflConfig config = new TflConfig();

        System.out.println("[TfL] Broker:  " + config.getBrokerUrl());
        System.out.println("[TfL] Topic:   " + config.getTopicName());
        System.out.println("[TfL] Source:  " + config.getSourceSystem());

        if (onceMode) {
            System.out.println("[TfL] Modo: one-shot");
            runOnce(config);
            System.out.println("[TfL] Finalizado.");
        } else {
            System.out.println("[TfL] Modo: periódico cada "
                    + config.getCapturePeriodMinutes() + " minutos");

            ScheduledExecutorService scheduler =
                    Executors.newSingleThreadScheduledExecutor();

            scheduler.scheduleAtFixedRate(
                    () -> {
                        try {
                            runOnce(config);
                        } catch (Exception e) {
                            System.err.println("[TfL] Error en ciclo periódico: "
                                    + e.getMessage());
                        }
                    },
                    0,
                    config.getCapturePeriodMinutes(),
                    TimeUnit.MINUTES
            );
        }
    }

    private static void runOnce(TflConfig config) throws Exception {
        try (ActivemqEventPublisher publisher = new ActivemqEventPublisher(
                config.getBrokerUrl(),
                config.getTopicName())) {

            TflController controller = new TflController(
                    config,
                    new TflJourneyFeeder(config),
                    new TflJourneyMapper(),
                    publisher,
                    new TflBrokerEventSerializer(config.getSourceSystem())
            );

            controller.executeCapture();
        }
    }
}