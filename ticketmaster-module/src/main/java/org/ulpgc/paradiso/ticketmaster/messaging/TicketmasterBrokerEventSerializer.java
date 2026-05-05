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
        String ts = resolveTs(event);
        BrokerEvent<TicketmasterEvent> brokerEvent =
                new BrokerEvent<>(ts, sourceSystem, event);
        return GSON.toJson(brokerEvent);
    }

    private String resolveTs(TicketmasterEvent event) {
        String dateTimeIso = event.getDateTimeIso();
        if (dateTimeIso != null && !dateTimeIso.isBlank()) {
            return dateTimeIso;
        }
        return event.getCapturedAt();
    }
}