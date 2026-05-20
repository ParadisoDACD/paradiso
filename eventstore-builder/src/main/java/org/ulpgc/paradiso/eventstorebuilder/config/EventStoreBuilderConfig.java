package org.ulpgc.paradiso.eventstorebuilder.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class EventStoreBuilderConfig {

    private final Properties props = new Properties();
    private final LocalEnvironment localEnvironment = new LocalEnvironment();

    public EventStoreBuilderConfig() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("eventstore-builder.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo eventstore-builder.properties", e);
        }
    }

    public String getBrokerUrl() {
        return requiredValue("PARADISO_BROKER_URL", "broker.url");
    }

    public String getClientId() {
        return value("PARADISO_EVENTSTORE_BUILDER_CLIENT_ID",
                "client.id", "paradiso-eventstore-builder");
    }

    public List<String> getTopics() {
        return Arrays.stream(value("PARADISO_EVENTSTORE_BUILDER_TOPICS",
                        "topics", "TicketmasterEvent,TflJourney").split(","))
                .map(String::trim)
                .filter(topic -> !topic.isBlank())
                .toList();
    }

    public String getEventstorePath() {
        return value("PARADISO_EVENTSTORE_PATH", "eventstore.path", "eventstore");
    }

    private String requiredValue(String envKey, String propKey) {
        String result = value(envKey, propKey, "");
        if (!result.isBlank()) return result;
        throw new IllegalStateException("Configuración requerida no definida: "
                + envKey + " o " + propKey);
    }

    private String value(String envKey, String propKey, String defaultValue) {
        String systemValue = System.getenv(envKey);
        if (hasText(systemValue)) return systemValue.trim();
        String localValue = localEnvironment.get(envKey);
        if (hasText(localValue)) return localValue.trim();
        return props.getProperty(propKey, defaultValue).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}