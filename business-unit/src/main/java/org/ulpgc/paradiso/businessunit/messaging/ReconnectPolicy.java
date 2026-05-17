package org.ulpgc.paradiso.businessunit.messaging;

public record ReconnectPolicy(long initialDelayMillis, long maxDelayMillis) {}