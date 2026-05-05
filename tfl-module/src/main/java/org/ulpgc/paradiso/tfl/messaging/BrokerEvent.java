package org.ulpgc.paradiso.tfl.messaging;

public class BrokerEvent<T> {

    private final String ts;
    private final String ss;
    private final T payload;

    public BrokerEvent(String ts, String ss, T payload) {
        this.ts = ts;
        this.ss = ss;
        this.payload = payload;
    }

    public String getTs() {
        return ts;
    }

    public String getSs() {
        return ss;
    }

    public T getPayload() {
        return payload;
    }
}