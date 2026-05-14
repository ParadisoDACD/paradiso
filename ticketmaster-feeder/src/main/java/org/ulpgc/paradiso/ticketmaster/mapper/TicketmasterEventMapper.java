package org.ulpgc.paradiso.ticketmaster.mapper;

import com.google.gson.*;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterEvent;

import java.util.ArrayList;
import java.util.List;

public class TicketmasterEventMapper {

    public List<TicketmasterEvent> map(String rawJson,
                                       TicketmasterCaptureContext context) {

        List<TicketmasterEvent> result = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(rawJson).getAsJsonObject();

            if (!root.has("_embedded")) {
                return result;
            }

            JsonArray events = root.getAsJsonObject("_embedded")
                    .getAsJsonArray("events");

            for (JsonElement elem : events) {
                try {
                    JsonObject e = elem.getAsJsonObject();
                    TicketmasterEvent ev = new TicketmasterEvent();

                    ev.setExternalEventId(getString(e, "id"));
                    ev.setName(getString(e, "name"));
                    ev.setEventUrl(getString(e, "url"));
                    ev.setClassificationName(context.sourceCategory());
                    ev.setCaptureBatchId(context.captureBatchId());
                    ev.setCapturedAt(context.capturedAt());
                    ev.setSourceCountry(context.sourceCountry());
                    ev.setSourceCity(context.sourceCity());
                    ev.setSourceCategory(context.sourceCategory());

                    if (e.has("classifications")) {
                        JsonArray cls = e.getAsJsonArray("classifications");
                        if (!cls.isEmpty()) {
                            JsonObject c = cls.get(0).getAsJsonObject();
                            ev.setSegment(getNestedName(c, "segment"));
                            ev.setGenre(getNestedName(c, "genre"));
                        }
                    }

                    if (e.has("dates") && e.getAsJsonObject("dates").has("start")) {
                        JsonObject start = e.getAsJsonObject("dates")
                                .getAsJsonObject("start");
                        ev.setLocalDate(getString(start, "localDate"));
                        ev.setLocalTime(getString(start, "localTime"));
                        ev.setDateTimeIso(getString(start, "dateTime"));
                    }

                    if (e.has("_embedded")) {
                        JsonObject emb = e.getAsJsonObject("_embedded");
                        if (emb.has("venues") && !emb.getAsJsonArray("venues").isEmpty()) {
                            JsonObject venue = emb.getAsJsonArray("venues")
                                    .get(0).getAsJsonObject();
                            ev.setVenueName(getString(venue, "name"));
                            ev.setCity(getNestedName(venue, "city"));
                            ev.setCountryCode(getNestedField(venue, "country", "countryCode"));
                        }
                    }

                    result.add(ev);

                } catch (Exception ex) {
                    System.err.println("  [TM] Evento omitido: " + ex.getMessage());
                }
            }

        } catch (Exception ex) {
            System.err.println("  [TM] Error procesando JSON: " + ex.getMessage());
        }

        return result;
    }

    private String getString(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString()
                : null;
    }

    private String getNestedName(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return getString(obj.getAsJsonObject(key), "name");
    }

    private String getNestedField(JsonObject obj, String key, String field) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return getString(obj.getAsJsonObject(key), field);
    }
}