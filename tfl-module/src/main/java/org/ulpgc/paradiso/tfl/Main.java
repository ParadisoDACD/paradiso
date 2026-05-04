package org.ulpgc.paradiso.tfl;

import org.ulpgc.paradiso.tfl.config.TflConfig;
import org.ulpgc.paradiso.tfl.controller.TflController;
import org.ulpgc.paradiso.tfl.feeder.TflJourneyFeeder;
import org.ulpgc.paradiso.tfl.mapper.TflJourneyMapper;
import org.ulpgc.paradiso.tfl.persistence.SqliteJourneyStore;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {

        TflConfig config = new TflConfig();

        SqliteJourneyStore store = new SqliteJourneyStore(config.getSqlitePath());
        store.initializeSchema();

        TflController controller = new TflController(
                config,
                new TflJourneyFeeder(config.getAppKey()),
                new TflJourneyMapper(),
                store
        );

        if (args.length > 0 && args[0].equals("--once")) {
            System.out.println("[TfL] Modo one-shot");
            controller.executeCapture();
            System.out.println("[TfL] Finalizado.");
            return;
        }

        System.out.printf("[TfL] Capturas periodicas cada %d minutos%n",
                config.getCapturePeriodMinutes());

        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(
                controller::executeCapture,
                0,
                config.getCapturePeriodMinutes(),
                TimeUnit.MINUTES
        );
    }
}