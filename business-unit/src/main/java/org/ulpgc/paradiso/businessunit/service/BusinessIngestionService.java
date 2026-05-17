package org.ulpgc.paradiso.businessunit.service;

import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRoutePlanRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.TransportRecord;
import org.ulpgc.paradiso.businessunit.recommendation.RecommendationBuilder;

import java.util.List;

public class BusinessIngestionService {

    private final Datamart datamart;
    private final RecommendationBuilder recommendationBuilder;

    public BusinessIngestionService(Datamart datamart, RecommendationBuilder recommendationBuilder) {
        this.datamart = datamart;
        this.recommendationBuilder = recommendationBuilder;
    }

    public void ingestConcert(ConcertRecord concert) {
        if (concert == null) {
            return;
        }
        datamart.upsertConcert(concert);
        List<ConcertRoutePlanRecord> plans = recommendationBuilder.buildPlansForConcert(concert);
        datamart.replacePlansForEvent(concert.externalEventId(), plans);
    }

    public void ingestTransport(TransportRecord transport) {
        if (transport == null) {
            return;
        }
        datamart.upsertTransport(transport);
        List<ConcertRoutePlanRecord> plans = recommendationBuilder.buildPlansForTransport(transport);
        datamart.upsertPlans(plans);
    }

    public void rebuildRecommendations() {
        datamart.upsertPlans(recommendationBuilder.buildAllPlans());
    }
}