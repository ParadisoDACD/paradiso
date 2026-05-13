package org.ulpgc.paradiso.tfl.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class TflConfig {

    private final Properties props = new Properties();

    public TflConfig() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("tfl.properties")) {
            if (in == null) {
                throw new RuntimeException(
                        "tfl.properties no encontrado. " +
                                "Copia tfl.properties.example, renombralo y rellena app.key.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando configuracion TfL", e);
        }
    }

    public String getAppKey() {
        return props.getProperty("app.key");
    }

    public String getSqlitePath() {
        return props.getProperty("sqlite.path", "data/tfl.db");
    }

    public int getCapturePeriodMinutes() {
<<<<<<< Updated upstream
        return Integer.parseInt(props.getProperty("capture.period.minutes", "60"));
    }

    public List<String> getCaptureTimes() {
        return Arrays.asList(props.getProperty("capture.times", "0900,1400,1900").split(","));
=======
        return parseInt("capture.period.minutes", "60");
    }

    public List<String> getCaptureTimes() {
        return parseCommaSeparated(props.getProperty("capture.times", "0900,1400,1900"));
    }

    public int getCaptureStartDayOffset() {
        return parseInt("capture.start.day.offset", "0");
    }

    public int getCaptureDaysAhead() {
        return parseInt("capture.days.ahead", "2");
    }

    public long getRequestSleepMillis() {
        return parseLong("request.sleep.ms", "200");
    }

    public List<String> getOrigins() {
        return parseCommaSeparated(props.getProperty("origins", ""));
    }

    public List<String> getDestinations() {
        return parseCommaSeparated(props.getProperty("destinations", ""));
    }

    public List<String[]> getRoutePairs() {
        List<String> origins = getOrigins();
        List<String> destinations = getDestinations();

        if (!origins.isEmpty() && !destinations.isEmpty()) {
            List<String[]> routePairs = new ArrayList<>();

            for (String origin : origins) {
                for (String destination : destinations) {
                    routePairs.add(new String[]{origin, destination});
                }
            }

            return routePairs;
        }

        return getRoutes();
>>>>>>> Stashed changes
    }

    public List<String[]> getRoutes() {
        List<String[]> routes = new ArrayList<>();
<<<<<<< Updated upstream
        for (String pair : props.getProperty("routes", "").split(";")) {
            String[] parts = pair.trim().split(">");
=======

        for (String pair : props.getProperty("routes", "").split(";")) {
            String[] parts = pair.trim().split(">");

>>>>>>> Stashed changes
            if (parts.length == 2) {
                routes.add(new String[]{parts[0].trim(), parts[1].trim()});
            }
        }
<<<<<<< Updated upstream
=======

>>>>>>> Stashed changes
        return routes;
    }

    public String getBrokerUrl() {
        return props.getProperty("broker.url", "tcp://localhost:61616");
    }

    public String getTopicName() {
        return props.getProperty("topic.name", "TflJourney");
    }

    public String getSourceSystem() {
        return props.getProperty("source.system", "tfl-module");
    }
<<<<<<< Updated upstream
=======

    private int parseInt(String key, String defaultValue) {
        return Integer.parseInt(props.getProperty(key, defaultValue).trim());
    }

    private long parseLong(String key, String defaultValue) {
        return Long.parseLong(props.getProperty(key, defaultValue).trim());
    }

    private List<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
>>>>>>> Stashed changes
}