package org.ulpgc.paradiso.ticketmaster.persistence;

import org.ulpgc.paradiso.ticketmaster.model.TicketmasterCaptureRun;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterEvent;

import java.util.List;

public interface EventStore {

    void initializeSchema() throws Exception;

    void startRun(TicketmasterCaptureRun run) throws Exception;

    void saveAll(List<TicketmasterEvent> events) throws Exception;

    void finishRun(String captureBatchId, int fetched, int inserted) throws Exception;

    void failRun(String captureBatchId, String errorMessage) throws Exception;
}