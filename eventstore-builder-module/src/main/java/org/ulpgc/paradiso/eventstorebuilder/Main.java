package org.ulpgc.paradiso.eventstorebuilder;

import org.ulpgc.paradiso.eventstorebuilder.config.EventStoreBuilderConfig;
import org.ulpgc.paradiso.eventstorebuilder.store.JsonLinesEventFileStore;
import org.ulpgc.paradiso.eventstorebuilder.subscriber.ActivemqDurableSubscriber;

import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) throws Exception {
        EventStoreBuilderConfig config = new EventStoreBuilderConfig();
        JsonLinesEventFileStore store = new JsonLinesEventFileStore(config.getEventstorePath());

        System.out.println("[EventStoreBuilder] Broker:     " + config.getBrokerUrl());
        System.out.println("[EventStoreBuilder] Client ID:  " + config.getClientId());
        System.out.println("[EventStoreBuilder] Topics:     " + config.getTopics());
        System.out.println("[EventStoreBuilder] Eventstore: " + config.getEventstorePath());

        try (ActivemqDurableSubscriber subscriber = new ActivemqDurableSubscriber(
                config.getBrokerUrl(),
                config.getClientId(),
                config.getTopics(),
                store)) {

            System.out.println("[EventStoreBuilder] Listo. Escuchando mensajes...");
            System.out.println("[EventStoreBuilder] Pulsa Ctrl+C para detener.");

            new CountDownLatch(1).await();
        }
    }
}