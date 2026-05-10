package org.ulpgc.paradiso.businessunit.messaging;

import org.ulpgc.paradiso.businessunit.event.BusinessEventProcessor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReconnectingBusinessUnitSubscriber implements AutoCloseable {

    private final String brokerUrl;
    private final String clientId;
    private final List<String> topicNames;
    private final BusinessEventProcessor processor;
    private final long reconnectDelayMillis;
    private final long maxReconnectDelayMillis;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile Thread worker;
    private volatile CountDownLatch disconnectionSignal;
    private volatile BusinessUnitSubscriber activeSubscriber;

    public ReconnectingBusinessUnitSubscriber(String brokerUrl,
                                              String clientId,
                                              List<String> topicNames,
                                              BusinessEventProcessor processor,
                                              long reconnectDelayMillis,
                                              long maxReconnectDelayMillis) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.topicNames = List.copyOf(topicNames);
        this.processor = processor;
        this.reconnectDelayMillis = reconnectDelayMillis;
        this.maxReconnectDelayMillis = maxReconnectDelayMillis;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        worker = new Thread(this::runConnectionLoop, "business-unit-subscriber-reconnector");
        worker.start();

        System.out.println("[BusinessUnit] Gestor de reconexión del subscriber iniciado.");
    }

    private void runConnectionLoop() {
        int attempt = 1;

        while (running.get()) {
            CountDownLatch currentSignal = new CountDownLatch(1);
            disconnectionSignal = currentSignal;

            try {
                System.out.println("[BusinessUnit] Conectando subscriber a ActiveMQ. Intento " + attempt + "...");

                BusinessUnitSubscriber subscriber = new BusinessUnitSubscriber(
                        brokerUrl,
                        clientId,
                        topicNames,
                        processor,
                        currentSignal::countDown
                );

                setActiveSubscriber(subscriber);
                attempt = 1;

                currentSignal.await();

                if (running.get()) {
                    System.err.println("[BusinessUnit] Conexión ActiveMQ perdida. Se intentará reconectar.");
                }

            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[BusinessUnit] No se pudo establecer conexión real-time con ActiveMQ: "
                            + e.getMessage());
                }

            } finally {
                closeActiveSubscriber();
                disconnectionSignal = null;
            }

            if (running.get()) {
                sleepBeforeReconnect(attempt);
                attempt++;
            }
        }

        System.out.println("[BusinessUnit] Gestor de reconexión del subscriber detenido.");
    }

    private synchronized void setActiveSubscriber(BusinessUnitSubscriber subscriber) {
        activeSubscriber = subscriber;
    }

    private synchronized void closeActiveSubscriber() {
        if (activeSubscriber == null) {
            return;
        }

        activeSubscriber.close();
        activeSubscriber = null;
    }

    private void sleepBeforeReconnect(int attempt) {
        long delay = reconnectDelayFor(attempt);

        System.out.println("[BusinessUnit] Reintentando conexión ActiveMQ en "
                + delay / 1_000 + " segundos...");

        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private long reconnectDelayFor(int attempt) {
        int safeAttempt = Math.max(1, attempt);
        long multiplier = 1L << Math.min(safeAttempt - 1, 4);
        long delay = reconnectDelayMillis * multiplier;

        if (delay < 0) {
            return maxReconnectDelayMillis;
        }

        return Math.min(delay, maxReconnectDelayMillis);
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        CountDownLatch signal = disconnectionSignal;

        if (signal != null) {
            signal.countDown();
        }

        closeActiveSubscriber();

        Thread currentWorker = worker;

        if (currentWorker != null) {
            currentWorker.interrupt();
        }
    }
}