package org.ulpgc.paradiso.tfl.messaging;

public interface EventPublisher extends AutoCloseable {

    void publish(String jsonEvent) throws Exception;

    @Override
    void close() throws Exception;
}