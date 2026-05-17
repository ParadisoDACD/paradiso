package org.ulpgc.paradiso.businessunit.datamart;

import org.ulpgc.paradiso.businessunit.utils.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Datamart {

    private final ConcurrentHashMap<String, ConcertRecord> concertsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcertRecord>> concertsByArtist = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, TransportRecord> transportsByKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, TransportRecord>> transportsByOriginAndDestination = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, OriginRecord> originsByKey = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ConcertRoutePlanRecord> plansById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcertRoutePlanRecord>> plansByEventId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcertRoutePlanRecord>> plansByArtist = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcertRoutePlanRecord>> plansByOrigin = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcertRoutePlanRecord>> plansByArtistAndOrigin = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcertRoutePlanRecord>> plansByOriginAndDestination = new ConcurrentHashMap<>();

    private volatile Instant lastProcessedAt;

    public void upsertConcert(ConcertRecord record) {
        if (record.externalEventId() == null || record.externalEventId().isBlank()) return;

        ConcertRecord previous = concertsById.put(record.externalEventId(), record);
        if (previous != null) {
            removeConcertFromArtistIndex(previous);
        }

        addConcertToArtistIndex(record);
        updateLastProcessedAt(record.capturedAt());
    }

    public void upsertTransport(TransportRecord record) {
        if (record.journeyKey() == null || record.journeyKey().isBlank()) return;

        TransportRecord previous = transportsByKey.put(record.journeyKey(), record);
        if (previous != null) {
            removeTransportFromIndexes(previous);
        }

        addTransportToIndexes(record);
        registerOriginFromTransport(record);
        updateLastProcessedAt(record.capturedAt());
    }

    public void upsertOrigin(OriginRecord record) {
        if (record.originKey() == null || record.originKey().isBlank()) return;
        originsByKey.put(record.originKey(), record);
    }

    public void upsertPlans(List<ConcertRoutePlanRecord> plans) {
        if (plans == null || plans.isEmpty()) return;

        for (ConcertRoutePlanRecord plan : plans) {
            if (plan.planId() == null || plan.planId().isBlank()) continue;

            ConcertRoutePlanRecord previous = plansById.put(plan.planId(), plan);
            if (previous != null) {
                removePlanFromIndexes(previous);
            }

            addPlanToIndexes(plan);
        }
    }

    public void replacePlansForEvent(String eventId, List<ConcertRoutePlanRecord> plans) {
        for (ConcertRoutePlanRecord existingPlan : plansByEventId(eventId)) {
            removePlan(existingPlan.planId());
        }

        upsertPlans(plans);
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

    public List<ConcertRecord> concertsByArtist(String artist) {
        return sortedConcerts(concertsByArtist.get(normalizeIndexKey(artist)));
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

    public List<TransportRecord> transportsByOriginAndDestination(String originKey, String destinationKey) {
        return sortedTransports(transportsByOriginAndDestination.get(compositeKey(originKey, destinationKey)));
    }

    public List<OriginRecord> origins() {
        return originsByKey.values().stream()
                .sorted(Comparator
                        .comparing(OriginRecord::active).reversed()
                        .thenComparing(OriginRecord::originName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public Optional<OriginRecord> originByKey(String originKey) {
        return Optional.ofNullable(originsByKey.get(originKey));
    }

    public List<ConcertRoutePlanRecord> plans() {
        return sortedPlans(plansById);
    }

    public List<ConcertRoutePlanRecord> plansByEventId(String eventId) {
        return sortedPlans(plansByEventId.get(StringUtils.safe(eventId)));
    }

    public List<ConcertRoutePlanRecord> plansByArtist(String artist) {
        return sortedPlans(plansByArtist.get(normalizeIndexKey(artist)));
    }

    public List<ConcertRoutePlanRecord> plansByOrigin(String originKey) {
        return sortedPlans(plansByOrigin.get(StringUtils.safe(originKey)));
    }

    public List<ConcertRoutePlanRecord> plansByArtistAndOrigin(String artist, String originKey) {
        return sortedPlans(plansByArtistAndOrigin.get(compositeKey(normalizeIndexKey(artist), originKey)));
    }

    public List<ConcertRoutePlanRecord> plansByOriginAndDestination(String originKey, String destinationKey) {
        return sortedPlans(plansByOriginAndDestination.get(compositeKey(originKey, destinationKey)));
    }

    public int concertCount() {
        return concertsById.size();
    }

    public int transportCount() {
        return transportsByKey.size();
    }

    public int originCount() {
        return originsByKey.size();
    }

    public int planCount() {
        return plansById.size();
    }

    public String lastProcessedAt() {
        return lastProcessedAt == null ? "" : lastProcessedAt.toString();
    }

    private void addConcertToArtistIndex(ConcertRecord record) {
        String artistKey = normalizeIndexKey(record.name());
        if (artistKey.isBlank()) return;

        concertsByArtist
                .computeIfAbsent(artistKey, ignored -> new ConcurrentHashMap<>())
                .put(record.externalEventId(), record);
    }

    private void removeConcertFromArtistIndex(ConcertRecord record) {
        removeFromIndex(concertsByArtist, normalizeIndexKey(record.name()), record.externalEventId());
    }

    private void addTransportToIndexes(TransportRecord record) {
        String key = compositeKey(record.sourceOrigin(), record.sourceDestination());
        if (key.isBlank()) return;

        transportsByOriginAndDestination
                .computeIfAbsent(key, ignored -> new ConcurrentHashMap<>())
                .put(record.journeyKey(), record);
    }

    private void registerOriginFromTransport(TransportRecord record) {
        String originKey = StringUtils.safe(record.sourceOrigin());

        if (originKey.isBlank()) {
            return;
        }

        originsByKey.putIfAbsent(originKey, new OriginRecord(
                originKey,
                StringUtils.safe(record.originName()).isBlank() ? originKey : record.originName(),
                "",
                "London",
                true
        ));
    }

    private void removeTransportFromIndexes(TransportRecord record) {
        removeFromIndex(
                transportsByOriginAndDestination,
                compositeKey(record.sourceOrigin(), record.sourceDestination()),
                record.journeyKey()
        );
    }

    private void addPlanToIndexes(ConcertRoutePlanRecord plan) {
        addToIndex(plansByEventId, StringUtils.safe(plan.eventId()), plan);
        addToIndex(plansByArtist, normalizeIndexKey(plan.artistName()), plan);
        addToIndex(plansByOrigin, StringUtils.safe(plan.originKey()), plan);
        addToIndex(plansByArtistAndOrigin, compositeKey(normalizeIndexKey(plan.artistName()), plan.originKey()), plan);
        addToIndex(plansByOriginAndDestination, compositeKey(plan.originKey(), plan.destinationStopKey()), plan);
    }

    private void removePlan(String planId) {
        ConcertRoutePlanRecord removed = plansById.remove(planId);
        if (removed != null) {
            removePlanFromIndexes(removed);
        }
    }

    private void removePlanFromIndexes(ConcertRoutePlanRecord plan) {
        removeFromIndex(plansByEventId, StringUtils.safe(plan.eventId()), plan.planId());
        removeFromIndex(plansByArtist, normalizeIndexKey(plan.artistName()), plan.planId());
        removeFromIndex(plansByOrigin, StringUtils.safe(plan.originKey()), plan.planId());
        removeFromIndex(plansByArtistAndOrigin, compositeKey(normalizeIndexKey(plan.artistName()), plan.originKey()), plan.planId());
        removeFromIndex(plansByOriginAndDestination, compositeKey(plan.originKey(), plan.destinationStopKey()), plan.planId());
    }

    private void addToIndex(ConcurrentHashMap<String, ConcurrentHashMap<String, ConcertRoutePlanRecord>> index,
                            String key,
                            ConcertRoutePlanRecord plan) {
        if (key == null || key.isBlank()) return;

        index.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>())
                .put(plan.planId(), plan);
    }

    private <T> void removeFromIndex(ConcurrentHashMap<String, ConcurrentHashMap<String, T>> index,
                                     String key,
                                     String id) {
        if (key == null || key.isBlank()) return;

        ConcurrentHashMap<String, T> bucket = index.get(key);
        if (bucket == null) return;

        bucket.remove(id);

        if (bucket.isEmpty()) {
            index.remove(key, bucket);
        }
    }

    private List<ConcertRecord> sortedConcerts(ConcurrentHashMap<String, ConcertRecord> source) {
        if (source == null || source.isEmpty()) return List.of();

        return source.values().stream()
                .sorted(Comparator
                        .comparing(ConcertRecord::dateTimeIso,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(ConcertRecord::name,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<ConcertRoutePlanRecord> sortedPlans(ConcurrentHashMap<String, ConcertRoutePlanRecord> source) {
        if (source == null || source.isEmpty()) return List.of();

        return source.values().stream()
                .sorted(Comparator
                        .comparing(ConcertRoutePlanRecord::eventDateTime,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(ConcertRoutePlanRecord::score,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ConcertRoutePlanRecord::durationMinutes,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ConcertRoutePlanRecord::originName,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<TransportRecord> sortedTransports(ConcurrentHashMap<String, TransportRecord> source) {
        if (source == null || source.isEmpty()) return List.of();

        return source.values().stream()
                .sorted(this::compareTransports)
                .toList();
    }

    private int compareTransports(TransportRecord a, TransportRecord b) {
        return Comparator
                .comparing(TransportRecord::durationMinutes,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(TransportRecord::startDateTime,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(TransportRecord::journeyKey,
                        Comparator.nullsLast(String::compareTo))
                .compare(a, b);
    }

    private String compositeKey(String left, String right) {
        String normalizedLeft = StringUtils.safe(left);
        String normalizedRight = StringUtils.safe(right);

        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
            return "";
        }

        return normalizedLeft + "|" + normalizedRight;
    }

    private String normalizeIndexKey(String value) {
        return StringUtils.safe(value).toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void updateLastProcessedAt(String capturedAt) {
        tryParseInstant(capturedAt).ifPresent(parsed -> {
            if (lastProcessedAt == null || parsed.isAfter(lastProcessedAt)) {
                lastProcessedAt = parsed;
            }
        });
    }

    private Optional<Instant> tryParseInstant(String value) {
        if (StringUtils.safe(value).isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}