package org.ulpgc.paradiso.tfl.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class TflConfig {

    private final Properties props = new Properties();
    private final LocalEnvironment localEnvironment = new LocalEnvironment();

    public TflConfig() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("tfl.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando configuración TfL", e);
        }
    }

    public String getAppKey() {
        return requiredValue("PARADISO_TFL_APP_KEY", "app.key");
    }

    public String getJourneyBaseUrl() {
        return requiredValue("PARADISO_TFL_JOURNEY_BASE_URL", "journey.base.url");
    }

    public int getCapturePeriodMinutes() {
        return parseInt("PARADISO_TFL_CAPTURE_PERIOD_MINUTES",
                "capture.period.minutes", "90");
    }

    public List<String> getCaptureTimes() {
        return parseCommaSeparated(value("PARADISO_TFL_CAPTURE_TIMES",
                "capture.times",
                "1530,1600,1630,1700,1730,1800,1815,1830,1845,1900,1915,1930,1945,2000,2030"));
    }

    public int getCaptureStartDayOffset() {
        return parseInt("PARADISO_TFL_CAPTURE_START_DAY_OFFSET",
                "capture.start.day.offset", "0");
    }

    public int getCaptureDaysAhead() {
        return parseInt("PARADISO_TFL_CAPTURE_DAYS_AHEAD",
                "capture.days.ahead", "10");
    }

    public long getRequestSleepMillis() {
        return parseLong("PARADISO_TFL_REQUEST_SLEEP_MS",
                "request.sleep.ms", "150");
    }

    public int getHttpConnectTimeoutSeconds() {
        return parseInt("PARADISO_TFL_HTTP_CONNECT_TIMEOUT_SECONDS",
                "http.connect.timeout.seconds", "10");
    }

    public int getHttpReadTimeoutSeconds() {
        return parseInt("PARADISO_TFL_HTTP_READ_TIMEOUT_SECONDS",
                "http.read.timeout.seconds", "45");
    }

    public int getHttpCallTimeoutSeconds() {
        return parseInt("PARADISO_TFL_HTTP_CALL_TIMEOUT_SECONDS",
                "http.call.timeout.seconds", "60");
    }

    public int getRequestMaxRetries() {
        return parseInt("PARADISO_TFL_REQUEST_MAX_RETRIES",
                "request.max.retries", "2");
    }

    public long getRequestRetryBackoffMillis() {
        return parseLong("PARADISO_TFL_REQUEST_RETRY_BACKOFF_MS",
                "request.retry.backoff.ms", "1000");
    }

    public List<String> getOrigins() {
        return parseCommaSeparated(value("PARADISO_TFL_ORIGINS", "origins", ""));
    }

    public List<String> getDestinations() {
        return parseCommaSeparated(value("PARADISO_TFL_DESTINATIONS", "destinations", ""));
    }

    public List<String[]> getRoutePairs() {
        List<String> origins = getOrigins();
        List<String> destinations = getDestinations();
        if (!origins.isEmpty() && !destinations.isEmpty()) return cartesianRoutes(origins, destinations);
        return getRoutes();
    }

    public List<String[]> getRoutes() {
        List<String[]> routes = new ArrayList<>();
        for (String pair : value("PARADISO_TFL_ROUTES", "routes", "").split(";")) {
            String[] parts = pair.trim().split(">");
            if (parts.length == 2) routes.add(new String[]{parts[0].trim(), parts[1].trim()});
        }
        return routes;
    }

    public String getBrokerUrl() {
        return requiredValue("PARADISO_BROKER_URL", "broker.url");
    }

    public String getTopicName() {
        return value("PARADISO_TFL_TOPIC", "topic.name", "TflJourney");
    }

    public String getSourceSystem() {
        return value("PARADISO_TFL_SOURCE_SYSTEM", "source.system", "tfl-feeder");
    }

    private List<String[]> cartesianRoutes(List<String> origins, List<String> destinations) {
        List<String[]> routePairs = new ArrayList<>();
        for (String origin : origins) {
            for (String destination : destinations) {
                routePairs.add(new String[]{origin, destination});
            }
        }
        return routePairs;
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

    private int parseInt(String envKey, String propKey, String defaultValue) {
        return Integer.parseInt(value(envKey, propKey, defaultValue));
    }

    private long parseLong(String envKey, String propKey, String defaultValue) {
        return Long.parseLong(value(envKey, propKey, defaultValue));
    }

    private List<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}