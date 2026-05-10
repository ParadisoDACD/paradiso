package org.ulpgc.paradiso.businessunit.datamart;

public record DatamartStatus(
        int concerts,
        int transports,
        String lastProcessedAt
) {}