package org.ulpgc.paradiso.tfl.feeder;

public interface JourneyFeeder {

    String fetchRawJourneys(String fromNaptan,
                            String toNaptan,
                            String date,
                            String time) throws Exception;
}