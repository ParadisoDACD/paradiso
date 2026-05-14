package org.ulpgc.paradiso.tfl.feeder;

public record TflJourneyRequest(String fromNaptan,
                                String toNaptan,
                                String date,
                                String time) {
}