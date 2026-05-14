package org.ulpgc.paradiso.businessunit.service;

public record RecommendationFilter(
        String eventId,
        String artist,
        String origin,
        String venue,
        String fromDate,
        String untilDate
) {

    public static RecommendationFilter empty() {
        return new RecommendationFilter(null, null, null, null, null, null);
    }
}