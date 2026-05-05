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
        return Integer.parseInt(props.getProperty("capture.period.minutes", "60"));
    }

    public List<String> getCaptureTimes() {
        return Arrays.asList(props.getProperty("capture.times", "0900,1400,1900").split(","));
    }

    public List<String[]> getRoutes() {
        List<String[]> routes = new ArrayList<>();
        for (String pair : props.getProperty("routes", "").split(";")) {
            String[] parts = pair.trim().split(">");
            if (parts.length == 2) {
                routes.add(new String[]{parts[0].trim(), parts[1].trim()});
            }
        }
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
}