package org.ulpgc.paradiso.tfl.feeder;

public interface JourneyFeeder {

    String fetchRawJourneys(TflJourneyRequest request) throws Exception;
}