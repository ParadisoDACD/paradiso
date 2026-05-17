package org.ulpgc.paradiso.ticketmaster.mapper;

import com.google.gson.*;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterEvent;

import java.util.ArrayList;
import java.util.List;

public class TicketmasterEventMapper {

    public List<TicketmasterEvent> map(String rawJson, TicketmasterCaptureContext context) {
        try {
            return parseEvents(rawJson, context);
        } catch (Exception ex) {
            System.err.println("  [TM] Error procesando JSON: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    private List<TicketmasterEvent> parseEvents(String rawJson, TicketmasterCaptureContext context) {
        JsonObject root = JsonParser.parseString(rawJson).getAsJsonObject();
        if (!root.has("_embedded")) return new ArrayList<>();

        JsonArray events = root.getAsJsonObject("_embedded").getAsJsonArray("events");
        List<TicketmasterEvent> result = new ArrayList<>();
        for (JsonElement elem : events) {
            tryMapSingleEvent(elem.getAsJsonObject(), context, result);
        }
        return result;
    }

    private void tryMapSingleEvent(JsonObject e, TicketmasterCaptureContext context, List<TicketmasterEvent> result) {
        try {
            result.add(mapSingleEvent(e, context));
        } catch (Exception ex) {
            System.err.println("  [TM] Evento omitido: " + ex.getMessage());
        }
    }

    private TicketmasterEvent mapSingleEvent(JsonObject e, TicketmasterCaptureContext context) {
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
        extractClassification(ev, e);
        extractDates(ev, e);
        extractVenue(ev, e);
        return ev;
    }

    private void extractClassification(TicketmasterEvent ev, JsonObject e) {
        if (!e.has("classifications")) return;
        JsonArray cls = e.getAsJsonArray("classifications");
        if (cls.isEmpty()) return;
        JsonObject c = cls.get(0).getAsJsonObject();
        ev.setSegment(getNestedName(c, "segment"));
        ev.setGenre(getNestedName(c, "genre"));
    }

    private void extractDates(TicketmasterEvent ev, JsonObject e) {
        if (!e.has("dates") || !e.getAsJsonObject("dates").has("start")) return;
        JsonObject start = e.getAsJsonObject("dates").getAsJsonObject("start");
        ev.setLocalDate(getString(start, "localDate"));
        ev.setLocalTime(getString(start, "localTime"));
        ev.setDateTimeIso(getString(start, "dateTime"));
    }

    private void extractVenue(TicketmasterEvent ev, JsonObject e) {
        if (!e.has("_embedded")) return;
        JsonObject emb = e.getAsJsonObject("_embedded");
        if (!emb.has("venues") || emb.getAsJsonArray("venues").isEmpty()) return;
        JsonObject venue = emb.getAsJsonArray("venues").get(0).getAsJsonObject();
        ev.setVenueName(getString(venue, "name"));
        ev.setCity(getNestedName(venue, "city"));
        ev.setCountryCode(getNestedField(venue, "country", "countryCode"));
    }

    private String getString(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : null;
    }

    private String getNestedName(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return getString(obj.getAsJsonObject(key), "name");
    }

    private String getNestedField(JsonObject obj, String key, String field) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return getString(obj.getAsJsonObject(key), field);
    }
}