package org.ulpgc.paradiso.eventstorebuilder.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class EventStoreBuilderConfig {

    private final Properties props = new Properties();

    public EventStoreBuilderConfig() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("eventstore-builder.properties")) {
            if (in == null) {
                throw new RuntimeException(
                        "eventstore-builder.properties no encontrado. " +
                                "Copia eventstore-builder.properties.example y renómbralo."
                );
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo eventstore-builder.properties", e);
        }
    }

    public String getBrokerUrl() {
        return props.getProperty("broker.url", "tcp://localhost:61616");
    }

    public String getClientId() {
        return props.getProperty("client.id", "paradiso-eventstore-builder");
    }

    public List<String> getTopics() {
        return Arrays.stream(
                        props.getProperty("topics", "TicketmasterEvent,TflJourney").split(","))
                .map(String::trim)
                .filter(topic -> !topic.isBlank())
                .toList();
    }

    public String getEventstorePath() {
        return props.getProperty("eventstore.path", "eventstore");
    }
}