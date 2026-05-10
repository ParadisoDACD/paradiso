package org.ulpgc.paradiso.businessunit.service;

import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConcertTransportService {

    private static final int MAX_ROUTES_PER_CONCERT = 10;

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "from",
            "london", "venue", "arena", "academy", "hall",
            "theatre", "theater", "station", "underground",
            "o2", "royal", "national", "central", "east",
            "west", "north", "south", "new", "old"
    );

    private final Datamart datamart;

    public ConcertTransportService(Datamart datamart) {
        this.datamart = datamart;
    }

    public List<ConcertRecord> searchConcerts(String query) {
        String normalizedQuery = normalize(query);

        if (normalizedQuery.isBlank()) {
            return datamart.concerts();
        }

        return datamart.concerts().stream()
                .filter(concert -> concertMatches(concert, normalizedQuery))
                .toList();
    }

    public ConcertTransportResponse transportForConcert(String concertId) {
        return datamart.concertById(concertId)
                .map(this::buildResponse)
                .orElseGet(() -> ConcertTransportResponse.notFound(concertId));
    }

    public ConcertSearchTransportResponse recommendationsForSearch(String query) {
        String normalizedQuery = normalize(query);

        if (normalizedQuery.isBlank()) {
            return ConcertSearchTransportResponse.empty(query);
        }

        List<ConcertTransportResponse> results = searchConcerts(query).stream()
                .map(this::buildResponse)
                .toList();

        return ConcertSearchTransportResponse.of(query, results);
    }

    private ConcertTransportResponse buildResponse(ConcertRecord concert) {
        Set<String> keywords = extractKeywords(concert.venueName());

        if (keywords.isEmpty()) {
            return ConcertTransportResponse.fallback(concert, bestAvailableRoutes());
        }

        List<TransportRecord> matched = datamart.transports().stream()
                .filter(transport -> hasTransportMatch(transport, keywords))
                .sorted((a, b) -> compareDuration(a.durationMinutes(), b.durationMinutes()))
                .limit(MAX_ROUTES_PER_CONCERT)
                .toList();

        if (!matched.isEmpty()) {
            return ConcertTransportResponse.matched(concert, matched);
        }

        return ConcertTransportResponse.fallback(concert, bestAvailableRoutes());
    }

    private List<TransportRecord> bestAvailableRoutes() {
        return datamart.transports().stream()
                .sorted((a, b) -> compareDuration(a.durationMinutes(), b.durationMinutes()))
                .limit(MAX_ROUTES_PER_CONCERT)
                .toList();
    }

    private boolean concertMatches(ConcertRecord concert, String normalizedQuery) {
        String target = normalize(String.join(" ",
                safe(concert.externalEventId()),
                safe(concert.name()),
                safe(concert.classificationName()),
                safe(concert.segment()),
                safe(concert.genre()),
                safe(concert.city()),
                safe(concert.venueName()),
                safe(concert.localDate())
        ));

        return target.contains(normalizedQuery);
    }

    private boolean hasTransportMatch(TransportRecord transport, Set<String> keywords) {
        String searchTarget = normalize(
                safe(transport.destinationName()) + " " + safe(transport.sourceDestination())
        );

        return keywords.stream().anyMatch(searchTarget::contains);
    }

    private Set<String> extractKeywords(String venueName) {
        return Arrays.stream(normalize(venueName).split("\\s+"))
                .filter(word -> word.length() > 3)
                .filter(word -> !STOPWORDS.contains(word))
                .collect(Collectors.toSet());
    }

    private String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int compareDuration(Integer a, Integer b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return Integer.compare(a, b);
    }
}