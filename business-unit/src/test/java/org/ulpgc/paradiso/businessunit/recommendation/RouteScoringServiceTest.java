package org.ulpgc.paradiso.businessunit.recommendation;

import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;
import org.ulpgc.paradiso.businessunit.venue.VenueStopMapping;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteScoringServiceTest {

    private final RouteScoringService scoringService = new RouteScoringService();

    @Test
    void shorterRouteGetsHigherScore() {
        ConcertRecord concert = concert();
        VenueStopMapping mapping = o2Mapping();

        double fastScore = scoringService.score(concert, transport("fast", 25, 2, "2026-06-01T19:10:00"), mapping);
        double slowScore = scoringService.score(concert, transport("slow", 60, 2, "2026-06-01T19:10:00"), mapping);

        assertTrue(fastScore > slowScore);
    }

    @Test
    void routeArrivingAfterConcertIsPenalized() {
        ConcertRecord concert = concert();
        VenueStopMapping mapping = o2Mapping();

        double onTimeScore = scoringService.score(concert, transport("on-time", 30, 2, "2026-06-01T19:30:00"), mapping);
        double lateScore = scoringService.score(concert, transport("late", 30, 2, "2026-06-01T20:20:00"), mapping);

        assertTrue(onTimeScore > lateScore);
    }

    private ConcertRecord concert() {
        return new ConcertRecord(
                "event1",
                "Coldplay",
                "",
                "Music",
                "Rock",
                "London",
                "GB",
                "The O2",
                "",
                "2026-06-01",
                "20:00:00",
                "2026-06-01T20:00:00",
                "music",
                "2026-05-14T09:00:00Z"
        );
    }

    private TransportRecord transport(String key, Integer durationMinutes, Integer numberOfLegs, String arrivalDateTime) {
        return new TransportRecord(
                key,
                key + "-hash",
                "Victoria",
                "North Greenwich",
                "2026-06-01T18:30:00",
                arrivalDateTime,
                durationMinutes,
                numberOfLegs,
                "tube",
                "2026-06-01",
                "1830",
                "Victoria",
                "O2Arena",
                "2026-05-14T09:00:00Z"
        );
    }

    private VenueStopMapping o2Mapping() {
        return new VenueStopMapping(
                "the_o2",
                "The O2",
                "O2Arena",
                "North Greenwich",
                Set.of("The O2", "O2 Arena", "North Greenwich")
        );
    }
}