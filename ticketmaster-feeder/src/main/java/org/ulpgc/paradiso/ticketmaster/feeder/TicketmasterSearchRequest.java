package org.ulpgc.paradiso.ticketmaster.feeder;

public record TicketmasterSearchRequest(String countryCode,
                                        String city,
                                        String category,
                                        String startDateTime,
                                        String endDateTime,
                                        int page,
                                        int size) {
}