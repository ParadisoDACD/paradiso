package org.ulpgc.paradiso.businessunit.datamart;

public record OriginRecord(
        String originKey,
        String originName,
        String naptanId,
        String area,
        boolean active
) {}