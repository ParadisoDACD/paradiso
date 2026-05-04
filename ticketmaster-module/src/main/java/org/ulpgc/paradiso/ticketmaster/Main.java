package org.ulpgc.paradiso.ticketmaster;

import org.ulpgc.paradiso.ticketmaster.config.TicketmasterConfig;
import org.ulpgc.paradiso.ticketmaster.controller.TicketmasterController;
import org.ulpgc.paradiso.ticketmaster.feeder.TicketmasterDiscoveryFeeder;
import org.ulpgc.paradiso.ticketmaster.mapper.TicketmasterEventMapper;
import org.ulpgc.paradiso.ticketmaster.persistence.SqliteEventStore;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {

        TicketmasterConfig config = new TicketmasterConfig();

        SqliteEventStore store = new SqliteEventStore(config.getSqlitePath());
        store.initializeSchema();

        TicketmasterController controller = new TicketmasterController(
                config,
                new TicketmasterDiscoveryFeeder(config.getApiKey()),
                new TicketmasterEventMapper(),
                store
        );

        if (args.length > 0 && args[0].equals("--once")) {
            System.out.println("[Ticketmaster] Modo one-shot");
            controller.executeCapture();
            System.out.println("[Ticketmaster] Finalizado.");
            return;
        }

        System.out.printf("[Ticketmaster] Capturas periodicas cada %d minutos%n",
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