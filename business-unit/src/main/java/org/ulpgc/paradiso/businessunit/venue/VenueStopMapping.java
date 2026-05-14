package org.ulpgc.paradiso.businessunit.venue;

import java.util.Set;

public record VenueStopMapping(
        String venueKey,
        String canonicalVenueName,
        String nearestStopKey,
        String nearestStopName,
        Set<String> aliases
) {

    public VenueStopMapping {
        aliases = Set.copyOf(aliases);
    }
}