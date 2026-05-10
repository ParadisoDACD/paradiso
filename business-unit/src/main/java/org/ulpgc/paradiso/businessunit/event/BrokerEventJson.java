package org.ulpgc.paradiso.businessunit.event;

public record BrokerEventJson(
        String ts,
        String ss,
        Object payload
) {}