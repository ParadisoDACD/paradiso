package org.ulpgc.paradiso.businessunit.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class BusinessUnitConfig {

    private final Properties props = new Properties();

    public BusinessUnitConfig() {
        try (InputStream in = getClass()
                .getClassLoader()
                .getResourceAsStream("business-unit.properties")) {
            if (in == null) {
                throw new RuntimeException(
                        "[BusinessUnit] ERROR: business-unit.properties no encontrado en resources.\n"
                                + "  → Copia business-unit.properties.example\n"
                                + "  → Renómbralo a business-unit.properties\n"
                                + "  → Ajusta los valores según tu entorno local"
                );
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo business-unit.properties", e);
        }
    }

    public String getBrokerUrl() {
        return props.getProperty("broker.url", "tcp://localhost:61616");
    }

    public String getClientId() {
        return props.getProperty("client.id", "paradiso-business-unit");
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

    public int getApiPort() {
        try {
            return Integer.parseInt(props.getProperty("api.port", "7000").trim());
        } catch (NumberFormatException e) {
            System.err.println("[BusinessUnit] api.port inválido, usando 7000");
            return 7000;
        }
    }

    public boolean isSubscriberEnabled() {
        return Boolean.parseBoolean(props.getProperty("subscriber.enabled", "true").trim());
    }

    public long getSubscriberReconnectDelayMillis() {
        return readLong("subscriber.reconnect.delay.ms", 5_000L);
    }

    public long getSubscriberReconnectMaxDelayMillis() {
        return readLong("subscriber.reconnect.max.delay.ms", 30_000L);
    }

    private long readLong(String key, long defaultValue) {
        try {
            return Long.parseLong(props.getProperty(key, Long.toString(defaultValue)).trim());
        } catch (NumberFormatException e) {
            System.err.println("[BusinessUnit] " + key + " inválido, usando " + defaultValue);
            return defaultValue;
        }
    }
}