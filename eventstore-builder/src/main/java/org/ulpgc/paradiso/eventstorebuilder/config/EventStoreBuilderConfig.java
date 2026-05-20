package org.ulpgc.paradiso.eventstorebuilder.config;

import org.ulpgc.paradiso.common.config.Configuration;

import java.util.List;

public class EventStoreBuilderConfig {

    private final Configuration config;

    public EventStoreBuilderConfig() {
        config = Configuration.fromProperties("eventstore-builder.properties");
    }

    public String getBrokerUrl() {
        return config.required("PARADISO_BROKER_URL", "broker.url");
    }

    public String getClientId() {
        return config.value("PARADISO_EVENTSTORE_BUILDER_CLIENT_ID",
                "client.id", "paradiso-eventstore-builder");
    }

    public List<String> getTopics() {
        return config.commaSeparated("PARADISO_EVENTSTORE_BUILDER_TOPICS",
                "topics", "TicketmasterEvent,TflJourney");
    }

    public String getEventstorePath() {
        return config.value("PARADISO_EVENTSTORE_PATH", "eventstore.path", "eventstore");
    }
}