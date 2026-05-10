package org.ulpgc.paradiso.businessunit.service;

import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

public class ConcertTransportService {

    private static final int MAX_ROUTES_PER_CONCERT = 10;

    private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "from", "at",
            "london", "venue", "arena", "academy", "hall",
            "theatre", "theater", "station", "underground",
            "royal", "national", "central", "east",
            "west", "north", "south", "new", "old"
    );

    private final Datamart datamart;

    private final Supplier<LocalDate> currentDateSupplier;

    private boolean isTodayOrFuture(String localDate) {
        if (safe(localDate).isBlank()) {
            return false;
        }

        try {
            LocalDate concertDate = LocalDate.parse(localDate);
            return !concertDate.isBefore(currentDateSupplier.get());
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    public ConcertTransportService(Datamart datamart) {
        this(datamart, () -> LocalDate.now(LONDON_ZONE));
    }

    ConcertTransportService(Datamart datamart, Supplier<LocalDate> currentDateSupplier) {
        this.datamart = datamart;
        this.currentDateSupplier = currentDateSupplier;
    }

    public List<ConcertRecord> searchConcerts(String query) {
        String normalizedQuery = normalize(query);

        if (normalizedQuery.isBlank()) {
            return datamart.concerts();
        }

        return datamart.concerts().stream()
                .filter(concert -> concertMatches(concert, normalizedQuery))
                .sorted(this::compareConcertsByDate)
                .toList();
    }

    public List<ConcertRecord> upcomingConcerts(String query, int limit) {
        List<ConcertRecord> base = normalize(query).isBlank()
                ? datamart.concerts()
                : searchConcerts(query);

        return base.stream()
                .filter(concert -> isTodayOrFuture(concert.localDate()))
                .sorted(this::compareConcertsByDate)
                .limit(limit)
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
        String normalizedVenue = normalize(venueName);

        Set<String> aliases = venueAliases(normalizedVenue);
        if (!aliases.isEmpty()) {
            return aliases;
        }

        return Arrays.stream(normalizedVenue.split("\\s+"))
                .filter(word -> word.length() > 3)
                .filter(word -> !STOPWORDS.contains(word))
                .collect(Collectors.toSet());
    }

    private Set<String> venueAliases(String normalizedVenue) {
        if (normalizedVenue.isBlank()) {
            return Set.of();
        }

        if (normalizedVenue.contains("o2 academy brixton")) {
            return Set.of("brixton", "brixtonacademy");
        }

        if (normalizedVenue.equals("the o2")
                || normalizedVenue.contains("at the o2")
                || normalizedVenue.contains("the o2 arena")
                || normalizedVenue.contains("indigo at the o2")) {
            return Set.of("o2arena", "north greenwich", "greenwich");
        }

        if (normalizedVenue.contains("royal albert hall")) {
            return Set.of("royalalberthall", "albert", "kensington");
        }

        if (normalizedVenue.contains("wembley")) {
            return Set.of("wembley", "wembleypark");
        }

        if (normalizedVenue.contains("alexandra palace")) {
            return Set.of("alexandrapalace", "alexandra");
        }

        return Set.of();
    }

    private int compareConcertsByDate(ConcertRecord a, ConcertRecord b) {
        return Comparator
                .comparing((ConcertRecord concert) -> sortableDate(concert.localDate()))
                .thenComparing(concert -> sortableTime(concert.localTime()))
                .thenComparing(concert -> safe(concert.name()))
                .compare(a, b);
    }

    private String sortableDate(String value) {
        return safe(value).isBlank() ? "9999-12-31" : value;
    }

    private String sortableTime(String value) {
        return safe(value).isBlank() ? "99:99:99" : value;
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