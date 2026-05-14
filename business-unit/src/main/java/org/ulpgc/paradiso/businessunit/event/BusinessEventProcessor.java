package org.ulpgc.paradiso.businessunit.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;
import org.ulpgc.paradiso.businessunit.recommendation.RecommendationBuilder;
import org.ulpgc.paradiso.businessunit.recommendation.RouteScoringService;
import org.ulpgc.paradiso.businessunit.service.BusinessIngestionService;
import org.ulpgc.paradiso.businessunit.venue.VenueNormalizer;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BusinessEventProcessor {

    public static final String TICKETMASTER_TOPIC = "TicketmasterEvent";
    public static final String TFL_TOPIC = "TflJourney";

    private final BusinessIngestionService ingestionService;
    private final Gson gson = new Gson();

    public BusinessEventProcessor(Datamart datamart) {
        this(new BusinessIngestionService(
                datamart,
                new RecommendationBuilder(
                        datamart,
                        new VenueNormalizer(),
                        new RouteScoringService()
                )
        ));
    }

    public BusinessEventProcessor(BusinessIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public void process(String topic, String jsonLine) {
        try {
            JsonObject root = gson.fromJson(jsonLine, JsonObject.class);

            if (root == null
                    || !root.has("payload")
                    || root.get("payload").isJsonNull()
                    || !root.get("payload").isJsonObject()) {
                return;
            }

            String ts = getString(root, "ts");
            JsonObject payload = root.getAsJsonObject("payload");

            if (TICKETMASTER_TOPIC.equals(topic)) {
                ingestionService.ingestConcert(toConcert(payload, ts));
            } else if (TFL_TOPIC.equals(topic)) {
                ingestionService.ingestTransport(toTransport(payload, ts));
            }

        } catch (Exception e) {
            System.err.println("[BusinessEventProcessor] Evento ignorado ["
                    + topic + "]: " + e.getMessage());
        }
    }

    private ConcertRecord toConcert(JsonObject payload, String ts) {
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
                ts
        );
    }

    private TransportRecord toTransport(JsonObject payload, String ts) {
        String journeyHash = getString(payload, "journeyHash");
        String captureDate = getString(payload, "captureDate");
        String captureTime = getString(payload, "captureTime");
        String startDateTime = getString(payload, "startDateTime");
        String arrivalDateTime = getString(payload, "arrivalDateTime");

        String journeyKey = Stream.of(
                        journeyHash,
                        captureDate,
                        captureTime,
                        startDateTime,
                        arrivalDateTime
                )
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("|"));

        return new TransportRecord(
                journeyKey,
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
                ts
        );
    }

    private String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : "";
    }

    private Integer getInteger(JsonObject obj, String key) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull()
                    ? obj.get(key).getAsInt()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }
}