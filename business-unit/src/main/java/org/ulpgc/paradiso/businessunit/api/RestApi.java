package org.ulpgc.paradiso.businessunit.api;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.DatamartStatus;
import org.ulpgc.paradiso.businessunit.service.ConcertSearchTransportResponse;
import org.ulpgc.paradiso.businessunit.service.ConcertTransportResponse;
import org.ulpgc.paradiso.businessunit.service.ConcertTransportService;

import java.util.Map;

public class RestApi {

    private static final int DEFAULT_CONCERT_LIMIT = 10;
    private static final int MAX_CONCERT_LIMIT = 50;
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;
    private static final int MIN_RECOMMENDATION_LIMIT = 1;
    private static final int MAX_RECOMMENDATION_LIMIT = 20;

    private final Datamart datamart;
    private final ConcertTransportService service;
    private final int port;
    private final Gson gson = new Gson();

    private Javalin app;

    public RestApi(Datamart datamart,
                   ConcertTransportService service,
                   int port) {
        this.datamart = datamart;
        this.service = service;
        this.port = port;
    }

    public void start() {
        app = Javalin.create().start(port);

        app.get("/", ctx -> json(ctx, Map.of(
                "application", "Paradiso Business Unit",
                "description", "Conciertos en Londres y rutas TfL al venue",
                "userFlow", Map.of(
                        "1", "GET /concerts/upcoming",
                        "2", "Elegir externalEventId",
                        "3", "GET /recommendations/{externalEventId}"
                ),
                "endpoints", Map.of(
                        "status", "/status",
                        "concerts", "/concerts",
                        "upcomingConcerts", "/concerts/upcoming",
                        "concertSearch", "/concerts?query={text}",
                        "transport", "/transport",
                        "route", "/concerts/{id}/transport",
                        "recommendationsBySearch", "/recommendations?query={text}&limit={n}",
                        "recommendationsById", "/recommendations/{id}"
                )
        )));

        app.get("/status", ctx -> json(ctx, new DatamartStatus(
                datamart.concertCount(),
                datamart.transportCount(),
                datamart.lastProcessedAt()
        )));

        app.get("/concerts", ctx -> {
            String query = ctx.queryParam("query");

            if (isBlank(query)) {
                json(ctx, datamart.concerts());
                return;
            }

            json(ctx, service.searchConcerts(query));
        });

        app.get("/concerts/upcoming", ctx -> {
            String query = ctx.queryParam("query");
            int limit = queryParamAsInt(ctx, "limit", DEFAULT_CONCERT_LIMIT, 1, MAX_CONCERT_LIMIT);
            json(ctx, service.upcomingConcerts(query, limit));
        });

        app.get("/concerts/{id}", ctx -> {
            String id = ctx.pathParam("id");

            datamart.concertById(id).ifPresentOrElse(
                    concert -> json(ctx, concert),
                    () -> jsonError(ctx, 404, "Concierto no encontrado: " + id)
            );
        });

        app.get("/concerts/{id}/transport", ctx -> {
            String id = ctx.pathParam("id");
            ConcertTransportResponse response = service.transportForConcert(id);

            if (!response.found()) {
                ctx.status(404);
            }

            json(ctx, response);
        });

        app.get("/recommendations", ctx -> {
            String query = ctx.queryParam("query");

            if (isBlank(query)) {
                jsonError(ctx, 400, "Debe indicarse el parámetro query. Ejemplo: /recommendations?query=example");
                return;
            }

            int limit = queryParamAsInt(
                    ctx,
                    "limit",
                    DEFAULT_RECOMMENDATION_LIMIT,
                    MIN_RECOMMENDATION_LIMIT,
                    MAX_RECOMMENDATION_LIMIT
            );

            ConcertSearchTransportResponse response = service.recommendationsForSearch(query, limit);

            if (!response.found()) {
                ctx.status(404);
            }

            json(ctx, response);
        });

        app.get("/recommendations/{id}", ctx -> {
            String id = ctx.pathParam("id");
            ConcertTransportResponse response = service.transportForConcert(id);

            if (!response.found()) {
                ctx.status(404);
            }

            json(ctx, response);
        });

        app.get("/transport", ctx -> json(ctx, datamart.transports()));

        System.out.println("[BusinessUnit] REST API disponible en http://localhost:" + port);
        System.out.println("[BusinessUnit] Endpoints:");
        System.out.println("  GET /status");
        System.out.println("  GET /concerts");
        System.out.println("  GET /concerts?query={text}");
        System.out.println("  GET /concerts/upcoming");
        System.out.println("  GET /concerts/upcoming?query={text}&limit={n}");
        System.out.println("  GET /concerts/{id}");
        System.out.println("  GET /concerts/{id}/transport");
        System.out.println("  GET /recommendations?query={text}&limit={n}");
        System.out.println("  GET /recommendations/{id}");
        System.out.println("  GET /transport");
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    private int queryParamAsInt(Context ctx,
                                String name,
                                int defaultValue,
                                int minValue,
                                int maxValue) {
        String value = ctx.queryParam(name);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value);
            return Math.max(minValue, Math.min(maxValue, parsed));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void json(Context ctx, Object object) {
        ctx.contentType("application/json");
        ctx.result(gson.toJson(object));
    }

    private void jsonError(Context ctx, int status, String message) {
        ctx.status(status);
        json(ctx, Map.of("error", message));
    }
}