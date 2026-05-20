package org.ulpgc.paradiso.ticketmaster.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class TicketmasterConfig {

    private final Properties props = new Properties();
    private final LocalEnvironment localEnvironment = new LocalEnvironment();

    public TicketmasterConfig() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("ticketmaster.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando configuración Ticketmaster", e);
        }
    }

    public String getApiKey() {
        return requiredValue("PARADISO_TICKETMASTER_API_KEY", "api.key");
    }

    public String getApiBaseUrl() {
        return requiredValue("PARADISO_TICKETMASTER_API_BASE_URL", "api.base.url");
    }

    public int getLookaheadDays() {
        return Integer.parseInt(value("PARADISO_TICKETMASTER_LOOKAHEAD_DAYS",
                "lookahead.days", "14"));
    }

    public int getCapturePeriodMinutes() {
        return Integer.parseInt(value("PARADISO_TICKETMASTER_CAPTURE_PERIOD_MINUTES",
                "capture.period.minutes", "60"));
    }

    public List<String> getCountries() {
        return parseCommaSeparated(value("PARADISO_TICKETMASTER_COUNTRIES",
                "countries", "GB"));
    }

    public List<String> getCities() {
        return parseCommaSeparated(value("PARADISO_TICKETMASTER_CITIES",
                "cities", "London"));
    }

    public List<String> getCategories() {
        return parseCommaSeparated(value("PARADISO_TICKETMASTER_CATEGORIES",
                "categories", "music"));
    }

    public String getBrokerUrl() {
        return requiredValue("PARADISO_BROKER_URL", "broker.url");
    }

    public String getTopicName() {
        return value("PARADISO_TICKETMASTER_TOPIC", "topic.name", "TicketmasterEvent");
    }

    public String getSourceSystem() {
        return value("PARADISO_TICKETMASTER_SOURCE_SYSTEM",
                "source.system", "ticketmaster-feeder");
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

    private List<String> parseCommaSeparated(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}