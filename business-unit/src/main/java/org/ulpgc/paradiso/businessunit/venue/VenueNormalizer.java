package org.ulpgc.paradiso.businessunit.venue;

import org.ulpgc.paradiso.businessunit.utils.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VenueNormalizer {

    private static final VenueStopMapping THE_O2 = new VenueStopMapping(
            "the_o2", "The O2", "O2Arena", "North Greenwich",
            Set.of("The O2", "O2 Arena", "The O2 Arena", "indigo at The O2",
                    "The O2, London", "North Greenwich")
    );

    private static final VenueStopMapping WEMBLEY = new VenueStopMapping(
            "wembley", "Wembley Stadium / OVO Arena Wembley", "WembleyPark", "Wembley Park",
            Set.of("Wembley Stadium", "OVO Arena Wembley", "Wembley Arena",
                    "Wembley", "Wembley Park")
    );

    private static final VenueStopMapping BRIXTON_ACADEMY = new VenueStopMapping(
            "brixton_academy", "O2 Academy Brixton", "BrixtonAcademy", "Brixton",
            Set.of("O2 Academy Brixton", "Brixton Academy", "Academy Brixton", "Brixton")
    );

    private static final VenueStopMapping ROYAL_ALBERT_HALL = new VenueStopMapping(
            "royal_albert_hall", "Royal Albert Hall", "RoyalAlbertHall", "High Street Kensington",
            Set.of("Royal Albert Hall", "High Street Kensington", "South Kensington", "Kensington")
    );

    private static final VenueStopMapping ALEXANDRA_PALACE = new VenueStopMapping(
            "alexandra_palace", "Alexandra Palace", "AlexandraPalace", "Alexandra Palace",
            Set.of("Alexandra Palace", "Alexandra Palace Theatre", "Ally Pally", "Alexandra")
    );

    private final List<VenueStopMapping> mappings;

    public VenueNormalizer() {
        this(defaultMappings());
    }

    public VenueNormalizer(List<VenueStopMapping> mappings) {
        this.mappings = List.copyOf(mappings);
    }

    public Optional<VenueStopMapping> findMapping(String venueName) {
        String normalizedVenueName = StringUtils.normalize(venueName);
        if (normalizedVenueName.isBlank()) {
            return Optional.empty();
        }
        return mappings.stream()
                .filter(mapping -> matches(mapping, normalizedVenueName))
                .findFirst();
    }

    public List<VenueStopMapping> mappings() {
        return mappings;
    }

    private boolean matches(VenueStopMapping mapping, String normalizedVenueName) {
        return mapping.aliases().stream()
                .map(StringUtils::normalize)
                .anyMatch(alias -> !alias.isBlank()
                        && (normalizedVenueName.equals(alias)
                        || normalizedVenueName.contains(alias)
                        || alias.contains(normalizedVenueName)));
    }

    private static List<VenueStopMapping> defaultMappings() {
        return List.of(THE_O2, WEMBLEY, BRIXTON_ACADEMY, ROYAL_ALBERT_HALL, ALEXANDRA_PALACE);
    }
}