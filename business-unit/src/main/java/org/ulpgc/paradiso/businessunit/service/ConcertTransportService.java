package org.ulpgc.paradiso.businessunit.service;

import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConcertTransportService {

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

    public ConcertTransportResponse transportForConcert(String concertId) {
        return datamart.concertById(concertId)
                .map(this::buildResponse)
                .orElseGet(() -> ConcertTransportResponse.notFound(concertId));
    }

    private ConcertTransportResponse buildResponse(ConcertRecord concert) {
        Set<String> keywords = extractKeywords(concert.venueName());

        if (keywords.isEmpty()) {
            return ConcertTransportResponse.fallback(concert, datamart.transports());
        }

        List<TransportRecord> matched = datamart.transports().stream()
                .filter(transport -> hasMatch(transport, keywords))
                .sorted((a, b) -> compareDuration(a.durationMinutes(), b.durationMinutes()))
                .toList();

        if (!matched.isEmpty()) {
            return ConcertTransportResponse.matched(concert, matched);
        }

        return ConcertTransportResponse.fallback(concert, datamart.transports());
    }

    private boolean hasMatch(TransportRecord transport, Set<String> keywords) {
        String searchTarget = normalize(
                transport.destinationName() + " " + transport.sourceDestination()
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

    private int compareDuration(Integer a, Integer b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return Integer.compare(a, b);
    }
}