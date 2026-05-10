package org.ulpgc.paradiso.businessunit;

import org.ulpgc.paradiso.businessunit.api.RestApi;
import org.ulpgc.paradiso.businessunit.config.BusinessUnitConfig;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.event.BusinessEventProcessor;
import org.ulpgc.paradiso.businessunit.loader.EventStoreLoader;
import org.ulpgc.paradiso.businessunit.messaging.ReconnectingBusinessUnitSubscriber;
import org.ulpgc.paradiso.businessunit.service.ConcertTransportService;

import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) throws Exception {
        BusinessUnitConfig config = new BusinessUnitConfig();
        printBanner(config);

        Datamart datamart = new Datamart();
        BusinessEventProcessor processor = new BusinessEventProcessor(datamart);

        System.out.println("[BusinessUnit] Iniciando carga histórica desde event store...");

        EventStoreLoader loader = new EventStoreLoader(
                config.getEventstorePath(),
                config.getTopics(),
                processor
        );

        int loaded = loader.loadAll();

        System.out.println("[BusinessUnit] Datamart inicial: "
                + datamart.concertCount() + " conciertos, "
                + datamart.transportCount() + " rutas. "
                + "Líneas leídas del event store: " + loaded);

        ReconnectingBusinessUnitSubscriber subscriber = startSubscriberManager(config, processor);

        ConcertTransportService service = new ConcertTransportService(datamart);
        RestApi api = new RestApi(datamart, service, config.getApiPort());
        api.start();

        CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[BusinessUnit] Señal de parada recibida. Cerrando...");

            if (subscriber != null) {
                subscriber.close();
            }

            api.stop();
            latch.countDown();
        }));

        System.out.println("[BusinessUnit] Sistema listo. Presiona Ctrl+C para detener.");
        latch.await();
    }

    private static ReconnectingBusinessUnitSubscriber startSubscriberManager(BusinessUnitConfig config,
                                                                             BusinessEventProcessor processor) {
        if (!config.isSubscriberEnabled()) {
            System.out.println("[BusinessUnit] Subscriber deshabilitado (subscriber.enabled=false).");
            System.out.println("[BusinessUnit] La API funcionará solo con datos históricos.");
            return null;
        }

        ReconnectingBusinessUnitSubscriber subscriber = new ReconnectingBusinessUnitSubscriber(
                config.getBrokerUrl(),
                config.getClientId(),
                config.getTopics(),
                processor,
                config.getSubscriberReconnectDelayMillis(),
                config.getSubscriberReconnectMaxDelayMillis()
        );

        subscriber.start();
        return subscriber;
    }

    private static void printBanner(BusinessUnitConfig config) {
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║     Paradiso - Business Unit     ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println("[BusinessUnit] Broker:         " + config.getBrokerUrl());
        System.out.println("[BusinessUnit] Client ID:      " + config.getClientId());
        System.out.println("[BusinessUnit] Topics:         " + config.getTopics());
        System.out.println("[BusinessUnit] Event Store:    " + config.getEventstorePath());
        System.out.println("[BusinessUnit] API port:       " + config.getApiPort());
        System.out.println("[BusinessUnit] Subscriber:     " + config.isSubscriberEnabled());
    }
}