package org.ulpgc.paradiso.ticketmaster.feeder;

public interface EventFeeder {

    String fetchRawEvents(TicketmasterSearchRequest request) throws Exception;
}