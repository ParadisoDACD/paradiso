package org.ulpgc.paradiso.ticketmaster.messaging;

import com.google.gson.Gson;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterEvent;

public class TicketmasterBrokerEventSerializer {

    private static final Gson GSON = new Gson();

    private final String sourceSystem;

    public TicketmasterBrokerEventSerializer(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String serialize(TicketmasterEvent event) {
        BrokerEvent<TicketmasterEvent> brokerEvent =
                new BrokerEvent<>(event.getCapturedAt(), sourceSystem, event);
        return GSON.toJson(brokerEvent);
    }
}