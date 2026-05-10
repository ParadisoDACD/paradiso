package org.ulpgc.paradiso.businessunit.datamart;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Datamart {

    private final ConcurrentHashMap<String, ConcertRecord> concertsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TransportRecord> transportsByKey = new ConcurrentHashMap<>();

    private volatile Instant lastProcessedAt;

    public void upsertConcert(ConcertRecord record) {
        if (record.externalEventId() == null || record.externalEventId().isBlank()) return;
        concertsById.put(record.externalEventId(), record);
        updateLastProcessedAt(record.ts());
    }

    public void upsertTransport(TransportRecord record) {
        if (record.journeyKey() == null || record.journeyKey().isBlank()) return;
        transportsByKey.put(record.journeyKey(), record);
        updateLastProcessedAt(record.ts());
    }

    public List<ConcertRecord> concerts() {
        return concertsById.values().stream()
                .sorted(Comparator
                        .comparing(ConcertRecord::dateTimeIso,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(ConcertRecord::name,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public Optional<ConcertRecord> concertById(String id) {
        return Optional.ofNullable(concertsById.get(id));
    }

    public List<TransportRecord> transports() {
        return transportsByKey.values().stream()
                .sorted(Comparator
                        .comparing(TransportRecord::destinationName,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(TransportRecord::durationMinutes,
                                Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    public int concertCount() {
        return concertsById.size();
    }

    public int transportCount() {
        return transportsByKey.size();
    }

    public String lastProcessedAt() {
        return lastProcessedAt == null ? "" : lastProcessedAt.toString();
    }

    private void updateLastProcessedAt(String ts) {
        if (ts == null || ts.isBlank()) return;

        try {
            Instant parsed = Instant.parse(ts);
            if (lastProcessedAt == null || parsed.isAfter(lastProcessedAt)) {
                lastProcessedAt = parsed;
            }
        } catch (Exception ignored) {
            // Un timestamp inválido no debe romper el datamart.
        }
    }
}