package org.ulpgc.paradiso.businessunit.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;
import org.ulpgc.paradiso.businessunit.service.BusinessIngestionService;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BusinessEventProcessor {

    public static final String TICKETMASTER_TOPIC = "TicketmasterEvent";
    public static final String TFL_TOPIC = "TflJourney";

    private final BusinessIngestionService ingestionService;
    private final Gson gson = new Gson();

    public BusinessEventProcessor(BusinessIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public void process(String topic, String jsonLine) {
        try {
            processSafely(topic, jsonLine);
        } catch (Exception exception) {
            System.err.println("[BusinessEventProcessor] Evento ignorado ["
                    + topic + "]: " + exception.getMessage());
        }
    }

    private void processSafely(String topic, String jsonLine) {
        JsonObject root = parseRoot(jsonLine);

        if (!hasPayload(root)) {
            return;
        }

        dispatch(topic, root.getAsJsonObject("payload"), getString(root, "ts"));
    }

    private JsonObject parseRoot(String jsonLine) {
        return gson.fromJson(jsonLine, JsonObject.class);
    }

    private boolean hasPayload(JsonObject root) {
        return root != null
                && root.has("payload")
                && !root.get("payload").isJsonNull()
                && root.get("payload").isJsonObject();
    }

    private void dispatch(String topic, JsonObject payload, String timestamp) {
        if (TICKETMASTER_TOPIC.equals(topic)) {
            ingestionService.ingestConcert(toConcert(payload, timestamp));
            return;
        }

        if (TFL_TOPIC.equals(topic)) {
            ingestionService.ingestTransport(toTransport(payload, timestamp));
        }
    }

    private ConcertRecord toConcert(JsonObject payload, String timestamp) {
        return new ConcertRecord(
                getString(payload, "externalEventId"),
                getString(payload, "name"),
                getString(payload, "classificationName"),
                getString(payload, "segment"),
                getString(payload, "genre"),
                getString(payload, "city"),
                getString(payload, "countryCode"),
                getString(payload, "venueName"),
                getString(payload, "eventUrl"),
                getString(payload, "localDate"),
                getString(payload, "localTime"),
                getString(payload, "dateTimeIso"),
                getString(payload, "sourceCategory"),
                timestamp
        );
    }

    private TransportRecord toTransport(JsonObject payload, String timestamp) {
        String journeyHash = getString(payload, "journeyHash");
        String captureDate = getString(payload, "captureDate");
        String captureTime = getString(payload, "captureTime");
        String startDateTime = getString(payload, "startDateTime");
        String arrivalDateTime = getString(payload, "arrivalDateTime");

        return new TransportRecord(
                journeyKey(journeyHash, captureDate, captureTime, startDateTime, arrivalDateTime),
                journeyHash,
                getString(payload, "originName"),
                getString(payload, "destinationName"),
                startDateTime,
                arrivalDateTime,
                getInteger(payload, "durationMinutes"),
                getInteger(payload, "numberOfLegs"),
                getString(payload, "firstLegMode"),
                captureDate,
                captureTime,
                getString(payload, "sourceOrigin"),
                getString(payload, "sourceDestination"),
                timestamp
        );
    }

    private String journeyKey(String journeyHash,
                              String captureDate,
                              String captureTime,
                              String startDateTime,
                              String arrivalDateTime) {
        return Stream.of(
                        journeyHash,
                        captureDate,
                        captureTime,
                        startDateTime,
                        arrivalDateTime
                )
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("|"));
    }

    private String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString()
                : "";
    }

    private Integer getInteger(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull()
                    ? object.get(key).getAsInt()
                    : null;
        } catch (Exception exception) {
            return null;
        }
    }
}