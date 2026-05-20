package org.ulpgc.paradiso.businessunit.config;

import org.ulpgc.paradiso.common.config.Configuration;

import java.util.List;

public class BusinessUnitConfig {

    private final Configuration config;

    public BusinessUnitConfig() {
        config = Configuration.fromProperties("business-unit.properties");
    }

    public String getBrokerUrl() {
        return config.required("PARADISO_BROKER_URL", "broker.url");
    }

    public String getClientId() {
        return config.value("PARADISO_CLIENT_ID",
                "client.id", "paradiso-business-unit");
    }

    public List<String> getTopics() {
        return config.commaSeparated("PARADISO_TOPICS",
                "topics", "TicketmasterEvent,TflJourney");
    }

    public String getEventstorePath() {
        return config.value("PARADISO_EVENTSTORE_PATH",
                "eventstore.path", "eventstore");
    }

    public int getApiPort() {
        return config.integer("PARADISO_API_PORT", "api.port", 7000);
    }

    public boolean isSubscriberEnabled() {
        return config.bool("PARADISO_SUBSCRIBER_ENABLED",
                "subscriber.enabled", true);
    }

    public long getSubscriberReconnectDelayMillis() {
        return config.longValue("PARADISO_RECONNECT_DELAY_MS",
                "subscriber.reconnect.delay.ms", 5_000L);
    }

    public long getSubscriberReconnectMaxDelayMillis() {
        return config.longValue("PARADISO_RECONNECT_MAX_DELAY_MS",
                "subscriber.reconnect.max.delay.ms", 30_000L);
    }
}