package org.ulpgc.paradiso.businessunit.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class BusinessUnitConfig {

    private final Properties props = new Properties();
    private final LocalEnvironment localEnvironment = new LocalEnvironment();

    public BusinessUnitConfig() {
        try (InputStream in = getClass()
                .getClassLoader()
                .getResourceAsStream("business-unit.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo business-unit.properties", e);
        }
    }

    public String getBrokerUrl() {
        return value("PARADISO_BROKER_URL", "broker.url", "tcp://localhost:61616");
    }

    public String getClientId() {
        return value("PARADISO_CLIENT_ID", "client.id", "paradiso-business-unit");
    }

    public List<String> getTopics() {
        return Arrays.stream(value("PARADISO_TOPICS", "topics", "TicketmasterEvent,TflJourney")
                        .split(","))
                .map(String::trim)
                .filter(topic -> !topic.isBlank())
                .toList();
    }

    public String getEventstorePath() {
        return value("PARADISO_EVENTSTORE_PATH", "eventstore.path", "eventstore");
    }

    public int getApiPort() {
        return readInt("PARADISO_API_PORT", "api.port", "7000");
    }

    public boolean isSubscriberEnabled() {
        return Boolean.parseBoolean(value(
                "PARADISO_SUBSCRIBER_ENABLED",
                "subscriber.enabled",
                "true"));
    }

    public long getSubscriberReconnectDelayMillis() {
        return readLong(
                "PARADISO_RECONNECT_DELAY_MS",
                "subscriber.reconnect.delay.ms",
                5_000L);
    }

    public long getSubscriberReconnectMaxDelayMillis() {
        return readLong(
                "PARADISO_RECONNECT_MAX_DELAY_MS",
                "subscriber.reconnect.max.delay.ms",
                30_000L);
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

    private int readInt(String envKey, String propKey, String defaultValue) {
        try {
            return Integer.parseInt(value(envKey, propKey, defaultValue));
        } catch (NumberFormatException e) {
            System.err.println("[BusinessUnit] " + propKey + " inválido, usando " + defaultValue);
            return Integer.parseInt(defaultValue);
        }
    }

    private long readLong(String envKey, String propKey, long defaultValue) {
        try {
            return Long.parseLong(value(envKey, propKey, Long.toString(defaultValue)));
        } catch (NumberFormatException e) {
            System.err.println("[BusinessUnit] " + propKey + " inválido, usando " + defaultValue);
            return defaultValue;
        }
    }
}