package org.ulpgc.paradiso.ticketmaster.feeder;

public interface EventFeeder {

    String fetchRawEvents(String countryCode,
                          String city,
                          String category,
                          String startDateTime,
                          String endDateTime,
                          int page,
                          int size) throws Exception;
}