package org.ulpgc.paradiso.businessunit.service;

import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRoutePlanRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;
import org.ulpgc.paradiso.businessunit.venue.VenueNormalizer;
import org.ulpgc.paradiso.businessunit.venue.VenueStopMapping;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConcertTransportService {

    private static final int MAX_ROUTES_PER_CONCERT = 10;
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;
    private static final int MIN_RECOMMENDATION_LIMIT = 1;
    private static final int MAX_RECOMMENDATION_LIMIT = 20;
    private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "from", "at",
            "london", "venue", "arena", "academy", "hall",
            "theatre", "theater", "station", "underground",
            "royal", "national", "central", "east",
            "west", "north", "south", "new", "old"
    );

    private final Datamart datamart;
    private final VenueNormalizer venueNormalizer;
    private final Supplier<LocalDate> currentDateSupplier;

    public ConcertTransportService(Datamart datamart) {
        this(datamart, new VenueNormalizer(), () -> LocalDate.now(LONDON_ZONE));
    }

    ConcertTransportService(Datamart datamart, Supplier<LocalDate> currentDateSupplier) {
        this(datamart, new VenueNormalizer(), currentDateSupplier);
    }

    ConcertTransportService(Datamart datamart,
                            VenueNormalizer venueNormalizer,
                            Supplier<LocalDate> currentDateSupplier) {
        this.datamart = datamart;
        this.venueNormalizer = venueNormalizer;
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
                .limit(Math.max(0, limit))
                .toList();
    }

    public ConcertTransportResponse transportForConcert(String concertId) {
        return datamart.concertById(concertId)
                .map(this::buildResponse)
                .orElseGet(() -> ConcertTransportResponse.notFound(concertId));
    }

    public ConcertSearchTransportResponse recommendationsForSearch(String query) {
        return recommendationsForSearch(query, DEFAULT_RECOMMENDATION_LIMIT);
    }

    public ConcertSearchTransportResponse recommendationsForSearch(String query, int limit) {
        String normalizedQuery = normalize(query);

        if (normalizedQuery.isBlank()) {
            return ConcertSearchTransportResponse.empty(query);
        }

        List<ConcertTransportResponse> results = upcomingConcerts(query, normalizeRecommendationLimit(limit)).stream()
                .map(this::buildResponse)
                .toList();

        return ConcertSearchTransportResponse.of(query, results);
    }

    public List<ConcertRoutePlanRecord> recommendations(RecommendationFilter filter) {
        RecommendationFilter safeFilter = filter == null ? RecommendationFilter.empty() : filter;

        return baseRecommendations(safeFilter).stream()
                .filter(plan -> matchesEvent(plan, safeFilter.eventId()))
                .filter(plan -> matchesArtist(plan, safeFilter.artist()))
                .filter(plan -> matchesOrigin(plan, safeFilter.origin()))
                .filter(plan -> matchesVenue(plan, safeFilter.venue()))
                .filter(plan -> isOnOrAfter(plan.eventDate(), safeFilter.fromDate()))
                .filter(plan -> isOnOrBefore(plan.eventDate(), safeFilter.untilDate()))
                .toList();
    }

    public List<ConcertRoutePlanRecord> recommendationsByEvent(String eventId) {
        if (safe(eventId).isBlank()) {
            return List.of();
        }

        return datamart.plansByEventId(eventId);
    }

    public List<ConcertRoutePlanRecord> recommendationsByEventAndOrigin(String eventId, String origin) {
        if (safe(eventId).isBlank()) {
            return List.of();
        }

        return recommendations(new RecommendationFilter(
                eventId,
                null,
                origin,
                null,
                null,
                null
        ));
    }

    public List<ConcertRoutePlanRecord> recommendationsByArtist(String artist) {
        if (safe(artist).isBlank()) {
            return List.of();
        }

        return datamart.plansByArtist(artist);
    }

    public List<ConcertRoutePlanRecord> recommendationsByArtistAndOrigin(String artist, String origin) {
        if (safe(artist).isBlank()) {
            return List.of();
        }

        if (safe(origin).isBlank()) {
            return recommendationsByArtist(artist);
        }

        return datamart.plansByArtistAndOrigin(artist, origin);
    }

    public List<ConcertRoutePlanRecord> recommendationsByOrigin(String origin) {
        if (safe(origin).isBlank()) {
            return List.of();
        }

        return datamart.plansByOrigin(origin);
    }

    public List<VenueStopMapping> venueMappings() {
        return venueNormalizer.mappings();
    }

    private List<ConcertRoutePlanRecord> baseRecommendations(RecommendationFilter filter) {
        if (!safe(filter.eventId()).isBlank()) {
            return datamart.plansByEventId(filter.eventId());
        }

        if (!safe(filter.origin()).isBlank()) {
            return datamart.plansByOrigin(filter.origin());
        }

        return datamart.plans();
    }

    private boolean matchesEvent(ConcertRoutePlanRecord plan, String eventId) {
        return safe(eventId).isBlank()
                || safe(plan.eventId()).equalsIgnoreCase(safe(eventId));
    }

    private boolean matchesArtist(ConcertRoutePlanRecord plan, String artist) {
        return safe(artist).isBlank()
                || normalize(plan.artistName()).contains(normalize(artist));
    }

    private boolean matchesOrigin(ConcertRoutePlanRecord plan, String origin) {
        return safe(origin).isBlank()
                || safe(plan.originKey()).equalsIgnoreCase(safe(origin))
                || normalize(plan.originName()).contains(normalize(origin));
    }

    private boolean matchesVenue(ConcertRoutePlanRecord plan, String venue) {
        return safe(venue).isBlank()
                || safe(plan.venueKey()).equalsIgnoreCase(safe(venue))
                || normalize(plan.venueName()).contains(normalize(venue))
                || normalize(plan.destinationStopName()).contains(normalize(venue))
                || safe(plan.destinationStopKey()).equalsIgnoreCase(safe(venue));
    }

    private boolean isOnOrAfter(String date, String fromDate) {
        if (safe(fromDate).isBlank()) {
            return true;
        }

        if (safe(date).isBlank()) {
            return false;
        }

        return date.compareTo(fromDate) >= 0;
    }

    private boolean isOnOrBefore(String date, String untilDate) {
        if (safe(untilDate).isBlank()) {
            return true;
        }

        if (safe(date).isBlank()) {
            return false;
        }

        return date.compareTo(untilDate) <= 0;
    }

    private ConcertTransportResponse buildResponse(ConcertRecord concert) {
        List<TransportRecord> availableRoutes = currentOrFutureTransports();
        Set<String> keywords = extractKeywords(concert.venueName());

        if (keywords.isEmpty()) {
            return ConcertTransportResponse.fallback(concert, bestRoutes(availableRoutes));
        }

        List<TransportRecord> matched = bestRoutes(
                availableRoutes.stream()
                        .filter(transport -> hasTransportMatch(transport, keywords))
                        .toList()
        );

        if (!matched.isEmpty()) {
            return ConcertTransportResponse.matched(concert, matched);
        }

        return ConcertTransportResponse.fallback(concert, bestRoutes(availableRoutes));
    }

    private int normalizeRecommendationLimit(int limit) {
        if (limit < MIN_RECOMMENDATION_LIMIT) {
            return DEFAULT_RECOMMENDATION_LIMIT;
        }

        return Math.min(limit, MAX_RECOMMENDATION_LIMIT);
    }

    private List<TransportRecord> bestRoutes(List<TransportRecord> routes) {
        return routes.stream()
                .sorted(this::compareRoutes)
                .limit(MAX_ROUTES_PER_CONCERT)
                .toList();
    }

    private List<TransportRecord> currentOrFutureTransports() {
        return datamart.transports().stream()
                .filter(this::isRouteTodayOrFuture)
                .toList();
    }

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

    private boolean isRouteTodayOrFuture(TransportRecord transport) {
        LocalDate routeDate = routeDate(transport);
        return routeDate != null && !routeDate.isBefore(currentDateSupplier.get());
    }

    private LocalDate routeDate(TransportRecord transport) {
        LocalDate dateFromStartDateTime = parseDatePrefix(transport.startDateTime());

        if (dateFromStartDateTime != null) {
            return dateFromStartDateTime;
        }

        return parseDatePrefix(transport.captureDate());
    }

    private LocalDate parseDatePrefix(String value) {
        String safeValue = safe(value);

        if (safeValue.length() < 10) {
            return null;
        }

        try {
            return LocalDate.parse(safeValue.substring(0, 10));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private int compareRoutes(TransportRecord a, TransportRecord b) {
        return Comparator
                .comparing((TransportRecord route) -> route.durationMinutes(), this::compareDuration)
                .thenComparing(route -> sortableDateTime(route.startDateTime()))
                .thenComparing(route -> sortableDateTime(route.arrivalDateTime()))
                .thenComparing(route -> safe(route.originName()))
                .thenComparing(route -> safe(route.destinationName()))
                .compare(a, b);
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

        Set<String> aliases = venueAliases(venueName);
        if (!aliases.isEmpty()) {
            return aliases;
        }

        return Arrays.stream(normalizedVenue.split("\\s+"))
                .filter(word -> word.length() > 3)
                .filter(word -> !STOPWORDS.contains(word))
                .collect(Collectors.toSet());
    }

    private Set<String> venueAliases(String venueName) {
        return venueNormalizer.findMapping(venueName)
                .map(this::keywordsFromMapping)
                .orElseGet(Set::of);
    }

    private Set<String> keywordsFromMapping(VenueStopMapping mapping) {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(
                                mapping.venueKey(),
                                mapping.canonicalVenueName(),
                                mapping.nearestStopKey(),
                                mapping.nearestStopName()
                        ),
                        mapping.aliases().stream()
                )
                .map(this::normalize)
                .filter(keyword -> !keyword.isBlank())
                .collect(Collectors.toSet());
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

    private String sortableDateTime(String value) {
        return safe(value).isBlank() ? "9999-12-31T99:99:99" : value;
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