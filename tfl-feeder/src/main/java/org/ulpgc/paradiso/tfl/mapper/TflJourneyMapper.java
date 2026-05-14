package org.ulpgc.paradiso.tfl.mapper;

import com.google.gson.*;
import org.ulpgc.paradiso.tfl.model.TflJourney;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class TflJourneyMapper {

    public List<TflJourney> map(String rawJson,
                                TflCaptureContext context) {

        List<TflJourney> result = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(rawJson).getAsJsonObject();

            if (!root.has("journeys")) {
                return result;
            }

            for (JsonElement elem : root.getAsJsonArray("journeys")) {
                try {
                    JsonObject j = elem.getAsJsonObject();
                    TflJourney journey = new TflJourney();

                    journey.setStartDateTime(getString(j, "startDateTime"));
                    journey.setArrivalDateTime(getString(j, "arrivalDateTime"));

                    journey.setDurationMinutes(
                            j.has("duration") ? j.get("duration").getAsInt() : 0
                    );

                    if (j.has("legs")) {
                        JsonArray legs = j.getAsJsonArray("legs");
                        journey.setNumberOfLegs(legs.size());

                        if (!legs.isEmpty()) {
                            JsonObject firstLeg = legs.get(0).getAsJsonObject();

                            if (firstLeg.has("mode")) {
                                journey.setFirstLegMode(
                                        getString(firstLeg.getAsJsonObject("mode"), "id")
                                );
                            }

                            if (firstLeg.has("departurePoint")) {
                                journey.setOriginName(
                                        getString(firstLeg.getAsJsonObject("departurePoint"),
                                                "commonName")
                                );
                            }

                            JsonObject lastLeg = legs.get(legs.size() - 1).getAsJsonObject();
                            if (lastLeg.has("arrivalPoint")) {
                                journey.setDestinationName(
                                        getString(lastLeg.getAsJsonObject("arrivalPoint"),
                                                "commonName")
                                );
                            }
                        }
                    }

                    journey.setCaptureDate(context.captureDate());
                    journey.setCaptureTime(context.captureTime());
                    journey.setSourceOrigin(context.sourceOrigin());
                    journey.setSourceDestination(context.sourceDestination());
                    journey.setCaptureBatchId(context.captureBatchId());
                    journey.setCapturedAt(context.capturedAt());

                    journey.setJourneyHash(generateHash(
                            context.sourceOrigin(),
                            context.sourceDestination(),
                            journey.getStartDateTime(),
                            journey.getArrivalDateTime(),
                            context.captureDate(),
                            context.captureTime()
                    ));

                    result.add(journey);

                } catch (Exception ex) {
                    System.err.println("  [TfL] Itinerario omitido: " + ex.getMessage());
                }
            }

        } catch (Exception ex) {
            System.err.println("  [TfL] Error procesando JSON: " + ex.getMessage());
        }

        return result;
    }

    private String getString(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString()
                : null;
    }

    private String generateHash(String... parts) {
        try {
            String combined = String.join("|", parts).replace("null", "");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.substring(0, 16);
        } catch (Exception e) {
            return java.util.UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 16);
        }
    }
}