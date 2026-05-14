package org.ulpgc.paradiso.businessunit.venue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VenueNormalizerTest {

    private final VenueNormalizer normalizer = new VenueNormalizer();

    @Test
    void mapsTheO2AliasesToO2Arena() {
        assertStop("The O2", "O2Arena");
        assertStop("O2 Arena", "O2Arena");
        assertStop("indigo at The O2", "O2Arena");
        assertStop("The O2, London", "O2Arena");
    }

    @Test
    void mapsWembleyAliasesToWembleyPark() {
        assertStop("Wembley Stadium", "WembleyPark");
        assertStop("OVO Arena Wembley", "WembleyPark");
    }

    @Test
    void mapsBrixtonToBrixtonAcademy() {
        assertStop("O2 Academy Brixton", "BrixtonAcademy");
        assertStop("Brixton Academy", "BrixtonAcademy");
    }

    @Test
    void mapsRoyalAlbertHallToNearestStop() {
        assertStop("Royal Albert Hall", "RoyalAlbertHall");
    }

    @Test
    void mapsAlexandraPalaceToNearestStop() {
        assertStop("Alexandra Palace", "AlexandraPalace");
    }

    @Test
    void returnsEmptyForUnknownVenue() {
        assertTrue(normalizer.findMapping("Unknown Venue XYZ").isEmpty());
    }

    private void assertStop(String venueName, String expectedStopKey) {
        VenueStopMapping mapping = normalizer.findMapping(venueName).orElseThrow();
        assertEquals(expectedStopKey, mapping.nearestStopKey());
    }
}