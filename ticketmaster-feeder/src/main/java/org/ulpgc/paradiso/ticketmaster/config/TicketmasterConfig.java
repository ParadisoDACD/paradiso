package org.ulpgc.paradiso.ticketmaster.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class TicketmasterConfig {

    private final Properties props = new Properties();

    public TicketmasterConfig() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("ticketmaster.properties")) {
            if (in == null) {
                throw new RuntimeException(
                        "ticketmaster.properties no encontrado. " +
                                "Copia ticketmaster.properties.example, renómbralo y rellena api.key.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando configuración Ticketmaster", e);
        }
    }

    public String getApiKey() {
        return props.getProperty("api.key");
    }

    public int getLookaheadDays() {
        return Integer.parseInt(props.getProperty("lookahead.days", "14"));
    }

    public int getCapturePeriodMinutes() {
        return Integer.parseInt(props.getProperty("capture.period.minutes", "60"));
    }

    public List<String> getCountries() {
        return Arrays.asList(props.getProperty("countries", "GB").split(","));
    }

    public List<String> getCities() {
        return Arrays.asList(props.getProperty("cities", "London").split(","));
    }

    public List<String> getCategories() {
        return Arrays.asList(props.getProperty("categories", "music").split(","));
    }

    public String getBrokerUrl() {
        return props.getProperty("broker.url", "tcp://localhost:61616");
    }

    public String getTopicName() {
        return props.getProperty("topic.name", "TicketmasterEvent");
    }

    public String getSourceSystem() {
        return props.getProperty("source.system", "ticketmaster-feeder");
    }
}