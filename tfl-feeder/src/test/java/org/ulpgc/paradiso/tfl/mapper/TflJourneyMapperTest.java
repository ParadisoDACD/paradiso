package org.ulpgc.paradiso.tfl.mapper;

import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.tfl.model.TflJourney;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TflJourneyMapperTest {

    private static final String JSON_UN_VIAJE = """
        {"journeys": [{
          "startDateTime":   "2026-07-15T09:00:00",
          "arrivalDateTime": "2026-07-15T09:38:00",
          "duration": 38,
          "legs": [
            {
              "mode": {"id": "tube", "name": "Tube"},
              "departurePoint": {"commonName": "King's Cross St. Pancras Underground Station"},
              "arrivalPoint":   {"commonName": "North Greenwich Underground Station"}
            }
          ]
        }]}""";

    private static final String JSON_SIN_VIAJES = "{}";

    private static final String JSON_DOS_VIAJES = """
        {"journeys": [
          {
            "startDateTime":"2026-07-15T09:00:00",
            "arrivalDateTime":"2026-07-15T09:38:00",
            "duration":38,
            "legs":[
              {
                "mode":{"id":"tube"},
                "departurePoint":{"commonName":"A"},
                "arrivalPoint":{"commonName":"B"}
              }
            ]
          },
          {
            "startDateTime":"2026-07-15T14:00:00",
            "arrivalDateTime":"2026-07-15T14:45:00",
            "duration":45,
            "legs":[
              {
                "mode":{"id":"bus"},
                "departurePoint":{"commonName":"A"},
                "arrivalPoint":{"commonName":"C"}
              },
              {
                "mode":{"id":"tube"},
                "departurePoint":{"commonName":"C"},
                "arrivalPoint":{"commonName":"B"}
              }
            ]
          }
        ]}""";

    @Test
    void mapeoCompletoDevuelveItinerarioCorrecto() {
        TflJourneyMapper mapper = new TflJourneyMapper();

        List<TflJourney> result = mapper.map(
                JSON_UN_VIAJE,
                new TflCaptureContext(
                        "KingsCross",
                        "O2Arena",
                        "2026-07-15",
                        "0900",
                        "batch-t-1",
                        "2026-04-01T00:00:00Z"
                )
        );

        assertEquals(1, result.size());

        TflJourney journey = result.get(0);

        assertEquals("2026-07-15T09:00:00", journey.getStartDateTime());
        assertEquals("2026-07-15T09:38:00", journey.getArrivalDateTime());
        assertEquals(38, journey.getDurationMinutes());
        assertEquals(1, journey.getNumberOfLegs());
        assertEquals("tube", journey.getFirstLegMode());
        assertEquals("King's Cross St. Pancras Underground Station", journey.getOriginName());
        assertEquals("North Greenwich Underground Station", journey.getDestinationName());
        assertEquals("KingsCross", journey.getSourceOrigin());
        assertEquals("O2Arena", journey.getSourceDestination());
        assertEquals("2026-07-15", journey.getCaptureDate());
        assertEquals("0900", journey.getCaptureTime());
        assertEquals("batch-t-1", journey.getCaptureBatchId());
        assertNotNull(journey.getJourneyHash());
        assertEquals(16, journey.getJourneyHash().length());
    }

    @Test
    void jsonSinJourneysRetornaListaVacia() {
        TflJourneyMapper mapper = new TflJourneyMapper();

        List<TflJourney> result = mapper.map(
                JSON_SIN_VIAJES,
                new TflCaptureContext(
                        "KingsCross",
                        "O2Arena",
                        "2026-07-15",
                        "0900",
                        "b",
                        "2026-04-01T00:00:00Z"
                )
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void dosViajesTienenHashesDistintosYLegsCorrectos() {
        TflJourneyMapper mapper = new TflJourneyMapper();

        List<TflJourney> result = mapper.map(
                JSON_DOS_VIAJES,
                new TflCaptureContext(
                        "KingsCross",
                        "O2Arena",
                        "2026-07-15",
                        "0900",
                        "batch-t-2",
                        "2026-04-01T00:00:00Z"
                )
        );

        assertEquals(2, result.size());

        assertEquals(1, result.get(0).getNumberOfLegs());
        assertEquals("tube", result.get(0).getFirstLegMode());

        assertEquals(2, result.get(1).getNumberOfLegs());
        assertEquals("bus", result.get(1).getFirstLegMode());

        assertNotEquals(
                result.get(0).getJourneyHash(),
                result.get(1).getJourneyHash()
        );
    }
}