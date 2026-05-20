package org.ulpgc.paradiso.ticketmaster.config;

import org.ulpgc.paradiso.common.config.Configuration;

import java.util.List;

public class TicketmasterConfig {

    private final Configuration config;

    public TicketmasterConfig() {
        config = Configuration.fromProperties("ticketmaster.properties");
    }

    public String getApiKey() {
        return config.required("PARADISO_TICKETMASTER_API_KEY", "api.key");
    }

    public String getApiBaseUrl() {
        return config.required("PARADISO_TICKETMASTER_API_BASE_URL", "api.base.url");
    }

    public int getLookaheadDays() {
        return config.integer("PARADISO_TICKETMASTER_LOOKAHEAD_DAYS",
                "lookahead.days", 60);
    }

    public int getCapturePeriodMinutes() {
        return config.integer("PARADISO_TICKETMASTER_CAPTURE_PERIOD_MINUTES",
                "capture.period.minutes", 60);
    }

    public List<String> getCountries() {
        return config.commaSeparated("PARADISO_TICKETMASTER_COUNTRIES",
                "countries", "GB");
    }

    public List<String> getCities() {
        return config.commaSeparated("PARADISO_TICKETMASTER_CITIES",
                "cities", "London");
    }

    public List<String> getCategories() {
        return config.commaSeparated("PARADISO_TICKETMASTER_CATEGORIES",
                "categories", "music");
    }

    public String getBrokerUrl() {
        return config.required("PARADISO_BROKER_URL", "broker.url");
    }

    public String getTopicName() {
        return config.value("PARADISO_TICKETMASTER_TOPIC",
                "topic.name", "TicketmasterEvent");
    }

    public String getSourceSystem() {
        return config.value("PARADISO_TICKETMASTER_SOURCE_SYSTEM",
                "source.system", "ticketmaster-feeder");
    }
}