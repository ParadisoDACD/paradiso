package org.ulpgc.paradiso.tfl.messaging;

import com.google.gson.Gson;
import org.ulpgc.paradiso.tfl.model.TflJourney;

public class TflBrokerEventSerializer {

    private static final Gson GSON = new Gson();

    private final String sourceSystem;

    public TflBrokerEventSerializer(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String serialize(TflJourney journey) {
        BrokerEvent<TflJourney> brokerEvent =
                new BrokerEvent<>(journey.getCapturedAt(), sourceSystem, journey);
        return GSON.toJson(brokerEvent);
    }
}