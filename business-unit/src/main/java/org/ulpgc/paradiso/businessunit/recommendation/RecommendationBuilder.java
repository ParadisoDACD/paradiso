package org.ulpgc.paradiso.businessunit.recommendation;

import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRoutePlanRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;
import org.ulpgc.paradiso.businessunit.utils.DateUtils;
import org.ulpgc.paradiso.businessunit.utils.StringUtils;
import org.ulpgc.paradiso.businessunit.venue.VenueNormalizer;
import org.ulpgc.paradiso.businessunit.venue.VenueStopMapping;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class RecommendationBuilder {

    private final Datamart datamart;
    private final VenueNormalizer venueNormalizer;
    private final RouteScoringService scoringService;
    private final Clock clock;

    public RecommendationBuilder(Datamart datamart,
                                 VenueNormalizer venueNormalizer,
                                 RouteScoringService scoringService) {
        this(datamart, venueNormalizer, scoringService, Clock.systemUTC());
    }

    RecommendationBuilder(Datamart datamart,
                          VenueNormalizer venueNormalizer,
                          RouteScoringService scoringService,
                          Clock clock) {
        this.datamart = datamart;
        this.venueNormalizer = venueNormalizer;
        this.scoringService = scoringService;
        this.clock = clock;
    }

    public List<ConcertRoutePlanRecord> buildPlansForConcert(ConcertRecord concert) {
        if (concert == null || StringUtils.safe(concert.externalEventId()).isBlank()) {
            return List.of();
        }

        Optional<VenueStopMapping> mapping = venueNormalizer.findMapping(concert.venueName());

        if (mapping.isEmpty()) {
            return List.of();
        }

        return datamart.transports().stream()
                .filter(transport -> matchesTransport(transport, mapping.get()))
                .filter(transport -> hasCompatibleDate(concert, transport))
                .map(transport -> buildPlan(concert, transport, mapping.get()))
                .sorted(this::comparePlans)
                .toList();
    }

    public List<ConcertRoutePlanRecord> buildPlansForTransport(TransportRecord transport) {
        if (transport == null || StringUtils.safe(transport.journeyKey()).isBlank()) {
            return List.of();
        }

        return datamart.concerts().stream()
                .flatMap(concert -> plansForConcertAndTransport(concert, transport))
                .sorted(this::comparePlans)
                .toList();
    }

    public List<ConcertRoutePlanRecord> buildAllPlans() {
        return datamart.concerts().stream()
                .flatMap(concert -> buildPlansForConcert(concert).stream())
                .sorted(this::comparePlans)
                .toList();
    }

    public void rebuildAll() {
        datamart.upsertPlans(buildAllPlans());
    }

    private Stream<ConcertRoutePlanRecord> plansForConcertAndTransport(ConcertRecord concert,
                                                                       TransportRecord transport) {
        Optional<VenueStopMapping> mapping = venueNormalizer.findMapping(concert.venueName());

        if (mapping.isEmpty()) {
            return Stream.empty();
        }

        if (!matchesTransport(transport, mapping.get())) {
            return Stream.empty();
        }

        if (!hasCompatibleDate(concert, transport)) {
            return Stream.empty();
        }

        return Stream.of(buildPlan(concert, transport, mapping.get()));
    }

    private ConcertRoutePlanRecord buildPlan(ConcertRecord concert,
                                             TransportRecord transport,
                                             VenueStopMapping mapping) {
        return new ConcertRoutePlanRecord(
                planId(concert, transport),
                concert.externalEventId(),
                concert.name(),
                concert.name(),
                concert.genre(),
                concert.venueName(),
                mapping.venueKey(),
                concert.localDate(),
                concert.localTime(),
                eventDateTime(concert),
                transport.sourceOrigin(),
                transport.originName(),
                mapping.nearestStopKey(),
                mapping.nearestStopName(),
                transport.journeyKey(),
                transport.startDateTime(),
                transport.arrivalDateTime(),
                transport.durationMinutes(),
                transport.numberOfLegs(),
                transport.firstLegMode(),
                scoringService.score(concert, transport, mapping),
                matchType(transport, mapping),
                Instant.now(clock).toString()
        );
    }

    private boolean matchesTransport(TransportRecord transport, VenueStopMapping mapping) {
        if (StringUtils.safe(transport.sourceDestination()).equalsIgnoreCase(StringUtils.safe(mapping.nearestStopKey()))) {
            return true;
        }

        String target = StringUtils.normalize(
                StringUtils.safe(transport.destinationName()) + " " + StringUtils.safe(transport.sourceDestination())
        );

        return Stream.concat(
                        Stream.of(mapping.nearestStopName(), mapping.nearestStopKey(), mapping.canonicalVenueName()),
                        mapping.aliases().stream()
                )
                .map(StringUtils::normalize)
                .filter(alias -> !alias.isBlank())
                .anyMatch(alias -> target.contains(alias) || alias.contains(target));
    }

    private boolean hasCompatibleDate(ConcertRecord concert, TransportRecord transport) {
        Optional<LocalDate> concertDate = concertDate(concert);
        Optional<LocalDate> routeDate = routeDate(transport);

        if (concertDate.isEmpty() || routeDate.isEmpty()) {
            return true;
        }

        return concertDate.get().equals(routeDate.get());
    }

    private Optional<LocalDate> concertDate(ConcertRecord concert) {
        String localDate = StringUtils.safe(concert.localDate());

        if (!localDate.isBlank()) {
            return DateUtils.parseDate(localDate);
        }

        return DateUtils.parseDatePrefix(concert.dateTimeIso());
    }

    private Optional<LocalDate> routeDate(TransportRecord transport) {
        return DateUtils.parseDatePrefix(transport.startDateTime())
                .or(() -> DateUtils.parseDatePrefix(transport.arrivalDateTime()))
                .or(() -> DateUtils.parseDate(transport.captureDate()));
    }

    private MatchType matchType(TransportRecord transport, VenueStopMapping mapping) {
        if (StringUtils.safe(transport.sourceDestination()).equalsIgnoreCase(StringUtils.safe(mapping.nearestStopKey()))) {
            return MatchType.EXACT_VENUE_STOP;
        }

        return MatchType.ALIAS_MATCH;
    }

    private String eventDateTime(ConcertRecord concert) {
        if (!StringUtils.safe(concert.dateTimeIso()).isBlank()) {
            return concert.dateTimeIso();
        }

        if (StringUtils.safe(concert.localDate()).isBlank()) {
            return "";
        }

        if (StringUtils.safe(concert.localTime()).isBlank()) {
            return concert.localDate();
        }

        return concert.localDate() + "T" + concert.localTime();
    }

    private String planId(ConcertRecord concert, TransportRecord transport) {
        return StringUtils.safe(concert.externalEventId()) + "::" + StringUtils.safe(transport.journeyKey());
    }

    private int comparePlans(ConcertRoutePlanRecord a, ConcertRoutePlanRecord b) {
        return Comparator
                .comparing(ConcertRoutePlanRecord::score,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ConcertRoutePlanRecord::durationMinutes,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ConcertRoutePlanRecord::departureTime,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(ConcertRoutePlanRecord::planId,
                        Comparator.nullsLast(String::compareTo))
                .compare(a, b);
    }
}