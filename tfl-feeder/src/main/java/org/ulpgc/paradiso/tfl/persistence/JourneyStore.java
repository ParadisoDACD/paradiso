package org.ulpgc.paradiso.tfl.persistence;

import org.ulpgc.paradiso.tfl.model.TflCaptureRun;
import org.ulpgc.paradiso.tfl.model.TflJourney;

import java.util.List;

public interface JourneyStore {

    void initializeSchema() throws Exception;

    void startRun(TflCaptureRun run) throws Exception;

    void saveAll(List<TflJourney> journeys) throws Exception;

    void finishRun(String captureBatchId, int fetched, int inserted) throws Exception;

    void failRun(String captureBatchId, String errorMessage) throws Exception;
}