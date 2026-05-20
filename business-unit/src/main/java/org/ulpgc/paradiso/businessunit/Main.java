package org.ulpgc.paradiso.businessunit;

import org.ulpgc.paradiso.businessunit.api.RestApi;
import org.ulpgc.paradiso.businessunit.cli.CliApp;
import org.ulpgc.paradiso.businessunit.config.BusinessUnitConfig;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.event.BusinessEventProcessor;
import org.ulpgc.paradiso.businessunit.loader.EventStoreLoader;
import org.ulpgc.paradiso.businessunit.messaging.ReconnectPolicy;
import org.ulpgc.paradiso.businessunit.messaging.ReconnectingBusinessUnitSubscriber;
import org.ulpgc.paradiso.businessunit.recommendation.RecommendationBuilder;
import org.ulpgc.paradiso.businessunit.recommendation.RouteScoringService;
import org.ulpgc.paradiso.businessunit.service.BusinessIngestionService;
import org.ulpgc.paradiso.businessunit.service.ConcertTransportService;
import org.ulpgc.paradiso.businessunit.venue.VenueNormalizer;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    private record RuntimeContext(Datamart datamart,
                                  ConcertTransportService service,
                                  BusinessEventProcessor processor) {
    }

    private record RunningSystem(Datamart datamart,
                                 ConcertTransportService service,
                                 ReconnectingBusinessUnitSubscriber subscriber,
                                 RestApi api,
                                 CountDownLatch latch,
                                 AtomicBoolean closed) {

        private boolean close() {
            if (!closed.compareAndSet(false, true)) return false;
            if (subscriber != null) subscriber.close();
            api.stop();
            latch.countDown();
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        BusinessUnitConfig config = new BusinessUnitConfig();
        printBanner(config);
        RuntimeContext runtime = buildRuntime(config);
        RunningSystem system = startSystem(config, runtime);
        registerShutdownHook(system);
        startInterface(args, system);
    }

    private static RuntimeContext buildRuntime(BusinessUnitConfig config) {
        Datamart datamart = new Datamart();
        BusinessIngestionService ingestion = buildIngestionService(datamart);
        BusinessEventProcessor processor = new BusinessEventProcessor(ingestion);
        int loaded = loadHistoricalEvents(config, processor);
        ingestion.rebuildRecommendations();
        printDatamartStatus(datamart, loaded);
        ConcertTransportService service = new ConcertTransportService(datamart);
        return new RuntimeContext(datamart, service, processor);
    }

    private static BusinessIngestionService buildIngestionService(Datamart datamart) {
        RecommendationBuilder builder = new RecommendationBuilder(
                datamart, new VenueNormalizer(), new RouteScoringService());
        return new BusinessIngestionService(datamart, builder);
    }

    private static int loadHistoricalEvents(BusinessUnitConfig config,
                                            BusinessEventProcessor processor) {
        System.out.println("[BusinessUnit] Iniciando carga histórica desde event store...");
        return new EventStoreLoader(config.getEventstorePath(), config.getTopics(), processor).loadAll();
    }

    private static void printDatamartStatus(Datamart datamart, int loaded) {
        System.out.println("[BusinessUnit] Datamart inicial: "
                + datamart.concertCount() + " conciertos, "
                + datamart.transportCount() + " rutas, "
                + datamart.planCount() + " planes precalculados. "
                + "Líneas leídas del event store: " + loaded);
    }

    private static RunningSystem startSystem(BusinessUnitConfig config, RuntimeContext runtime) {
        ReconnectingBusinessUnitSubscriber subscriber = startSubscriberManager(config, runtime.processor());
        RestApi api = new RestApi(runtime.datamart(), runtime.service(), config.getApiPort());
        api.start();
        return new RunningSystem(runtime.datamart(), runtime.service(), subscriber,
                api, new CountDownLatch(1), new AtomicBoolean(false));
    }

    private static void registerShutdownHook(RunningSystem system) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(system)));
    }

    private static void shutdown(RunningSystem system) {
        if (system.close()) System.out.println("[BusinessUnit] Sistema cerrado.");
    }

    private static void startInterface(String[] args, RunningSystem system) throws InterruptedException {
        if (containsCliMode(args)) {
            runCli(system);
            return;
        }
        waitUntilShutdown(system);
    }

    private static boolean containsCliMode(String[] args) {
        return Arrays.asList(args).contains("--cli");
    }

    private static void runCli(RunningSystem system) {
        try {
            new CliApp(system.datamart(), system.service()).run();
        } catch (IllegalStateException e) {
            System.err.println("[BusinessUnit] CLI finalizada: " + e.getMessage());
        } finally {
            shutdown(system);
        }
    }

    private static void waitUntilShutdown(RunningSystem system) throws InterruptedException {
        System.out.println("[BusinessUnit] Sistema listo. Presiona Ctrl+C para detener.");
        system.latch().await();
    }

    private static ReconnectingBusinessUnitSubscriber startSubscriberManager(BusinessUnitConfig config,
                                                                             BusinessEventProcessor processor) {
        if (!config.isSubscriberEnabled()) return disabledSubscriber();
        ReconnectPolicy policy = new ReconnectPolicy(
                config.getSubscriberReconnectDelayMillis(),
                config.getSubscriberReconnectMaxDelayMillis());
        ReconnectingBusinessUnitSubscriber subscriber = new ReconnectingBusinessUnitSubscriber(
                config.getBrokerUrl(), config.getClientId(), config.getTopics(), processor, policy);
        subscriber.start();
        return subscriber;
    }

    private static ReconnectingBusinessUnitSubscriber disabledSubscriber() {
        System.out.println("[BusinessUnit] Subscriber deshabilitado. Solo datos históricos.");
        return null;
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