package org.ulpgc.paradiso.ticketmaster.messaging;

public interface EventPublisher extends AutoCloseable {

    void publish(String jsonEvent) throws Exception;

    @Override
    void close() throws Exception;
}