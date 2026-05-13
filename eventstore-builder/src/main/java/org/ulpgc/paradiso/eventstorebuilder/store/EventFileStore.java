package org.ulpgc.paradiso.eventstorebuilder.store;

public interface EventFileStore {

    void append(String topic, String jsonEvent) throws Exception;
}