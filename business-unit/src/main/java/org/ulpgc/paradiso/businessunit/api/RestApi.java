package org.ulpgc.paradiso.businessunit.api;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRoutePlanRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.DatamartStatus;
import org.ulpgc.paradiso.businessunit.service.ConcertTransportResponse;
import org.ulpgc.paradiso.businessunit.service.ConcertTransportService;
import org.ulpgc.paradiso.businessunit.service.RecommendationFilter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RestApi {

    private static final int DEFAULT_CONCERT_LIMIT = 10;
    private static final int MAX_CONCERT_LIMIT = 50;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

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

        registerRootEndpoint();
        registerStatusEndpoint();
        registerConcertEndpoints();
        registerRecommendationEndpoints();
        registerCatalogEndpoints();
        registerTransportEndpoints();

        printEndpoints();
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    private void registerRootEndpoint() {
        app.get("/", ctx -> json(ctx, Map.of(
                "application", "Paradiso Business Unit",
                "description", "API REST para consultar conciertos en Londres y recomendaciones de transporte TfL precalculadas",
                "userFlow", Map.of(
                        "1", "GET /concerts/upcoming",
                        "2", "Elegir externalEventId o artista",
                        "3", "GET /concerts/{id}/routes o GET /artists/{artist}/recommendations"
                ),
                "endpoints", Map.of(
                        "status", "/status",
                        "concerts", "/concerts",
                        "upcomingConcerts", "/concerts/upcoming",
                        "concertById", "/concerts/{id}",
                        "transport", "/transport",
                        "origins", "/origins",
                        "venues", "/venues",
                        "recommendations", "/recommendations?artist={artist}&origin={origin}",
                        "routesByConcert", "/concerts/{id}/routes",
                        "recommendationsByArtist", "/artists/{artist}/recommendations"
                )
        )));
    }

    private void registerStatusEndpoint() {
        app.get("/status", ctx -> json(ctx, new DatamartStatus(
                datamart.concertCount(),
                datamart.transportCount(),
                datamart.originCount(),
                datamart.planCount(),
                datamart.lastProcessedAt()
        )));
    }

    private void registerConcertEndpoints() {
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

        app.get("/concerts/{id}/routes", ctx -> {
            String id = ctx.pathParam("id");

            if (datamart.concertById(id).isEmpty()) {
                jsonError(ctx, 404, "Concierto no encontrado: " + id);
                return;
            }

            RecommendationFilter filter = new RecommendationFilter(
                    id,
                    null,
                    ctx.queryParam("origin"),
                    null,
                    ctx.queryParam("fromDate"),
                    ctx.queryParam("untilDate")
            );

            json(ctx, recommendationResponse(ctx, queryMap(filter), service.recommendations(filter)));
        });
    }

    private void registerRecommendationEndpoints() {
        app.get("/artists/{artist}/recommendations", ctx -> {
            String artist = ctx.pathParam("artist");

            RecommendationFilter filter = new RecommendationFilter(
                    null,
                    artist,
                    ctx.queryParam("origin"),
                    ctx.queryParam("venue"),
                    ctx.queryParam("fromDate"),
                    ctx.queryParam("untilDate")
            );

            List<ConcertRoutePlanRecord> results = service.recommendations(filter);

            if (results.isEmpty()) {
                ctx.status(404);
            }

            json(ctx, recommendationResponse(ctx, queryMap(filter), results));
        });

        app.get("/recommendations", ctx -> {
            RecommendationFilter filter = new RecommendationFilter(
                    ctx.queryParam("eventId"),
                    firstNonBlank(ctx.queryParam("artist"), ctx.queryParam("query")),
                    ctx.queryParam("origin"),
                    ctx.queryParam("venue"),
                    ctx.queryParam("fromDate"),
                    ctx.queryParam("untilDate")
            );

            List<ConcertRoutePlanRecord> results = service.recommendations(filter);
            json(ctx, recommendationResponse(ctx, queryMap(filter), results));
        });

        app.get("/recommendations/{id}", ctx -> {
            String id = ctx.pathParam("id");

            if (datamart.concertById(id).isEmpty()) {
                jsonError(ctx, 404, "Concierto no encontrado: " + id);
                return;
            }

            RecommendationFilter filter = new RecommendationFilter(
                    id,
                    null,
                    ctx.queryParam("origin"),
                    null,
                    null,
                    null
            );

            json(ctx, recommendationResponse(ctx, queryMap(filter), service.recommendations(filter)));
        });
    }

    private void registerCatalogEndpoints() {
        app.get("/origins", ctx -> json(ctx, datamart.origins()));
        app.get("/venues", ctx -> json(ctx, service.venueMappings()));
    }

    private void registerTransportEndpoints() {
        app.get("/transport", ctx -> json(ctx, datamart.transports()));

        app.get("/concerts/{id}/transport", ctx -> {
            String id = ctx.pathParam("id");
            ConcertTransportResponse response = service.transportForConcert(id);

            if (!response.found()) {
                ctx.status(404);
            }

            json(ctx, response);
        });
    }

    private Map<String, Object> recommendationResponse(Context ctx,
                                                       Map<String, String> query,
                                                       List<ConcertRoutePlanRecord> results) {
        PaginationRequest paginationRequest = paginationRequestFrom(ctx);
        List<ConcertRoutePlanRecord> responseResults = isPaginationRequested(ctx)
                ? paginate(results, paginationRequest)
                : results;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query);
        response.put("count", results.size());

        if (isPaginationRequested(ctx)) {
            response.put("page", paginationRequest.page());
            response.put("size", paginationRequest.size());
            response.put("totalPages", totalPages(results.size(), paginationRequest.size()));
        }

        response.put("results", responseResults);
        return response;
    }

    private List<ConcertRoutePlanRecord> paginate(List<ConcertRoutePlanRecord> results,
                                                  PaginationRequest paginationRequest) {
        long from = (long) paginationRequest.page() * paginationRequest.size();

        if (from >= results.size()) {
            return List.of();
        }

        int fromIndex = (int) from;
        int toIndex = Math.min(fromIndex + paginationRequest.size(), results.size());

        return results.subList(fromIndex, toIndex);
    }

    private PaginationRequest paginationRequestFrom(Context ctx) {
        int page = queryParamAsInt(ctx, "page", 0, 0, Integer.MAX_VALUE);
        int size = queryParamAsInt(ctx, "size", DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE);

        return new PaginationRequest(page, size);
    }

    private boolean isPaginationRequested(Context ctx) {
        return !isBlank(ctx.queryParam("page")) || !isBlank(ctx.queryParam("size"));
    }

    private int totalPages(int totalItems, int size) {
        if (totalItems == 0) {
            return 0;
        }

        return (int) Math.ceil((double) totalItems / size);
    }

    private Map<String, String> queryMap(RecommendationFilter filter) {
        Map<String, String> query = new LinkedHashMap<>();
        putIfPresent(query, "eventId", filter.eventId());
        putIfPresent(query, "artist", filter.artist());
        putIfPresent(query, "origin", filter.origin());
        putIfPresent(query, "venue", filter.venue());
        putIfPresent(query, "fromDate", filter.fromDate());
        putIfPresent(query, "untilDate", filter.untilDate());
        return query;
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (!isBlank(value)) {
            map.put(key, value);
        }
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first;
        }

        return second;
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

    private void printEndpoints() {
        System.out.println("[BusinessUnit] REST API disponible en http://localhost:" + port);
        System.out.println("[BusinessUnit] Endpoints:");
        System.out.println("  GET /status");
        System.out.println("  GET /concerts");
        System.out.println("  GET /concerts?query={text}");
        System.out.println("  GET /concerts/upcoming");
        System.out.println("  GET /concerts/upcoming?query={text}&limit={n}");
        System.out.println("  GET /concerts/{id}");
        System.out.println("  GET /concerts/{id}/routes");
        System.out.println("  GET /concerts/{id}/routes?origin={origin}");
        System.out.println("  GET /artists/{artist}/recommendations");
        System.out.println("  GET /artists/{artist}/recommendations?origin={origin}");
        System.out.println("  GET /recommendations");
        System.out.println("  GET /recommendations?artist={artist}&origin={origin}&venue={venue}&fromDate={yyyy-mm-dd}&untilDate={yyyy-mm-dd}");
        System.out.println("  GET /recommendations?page={page}&size={size}  (size máximo: " + MAX_PAGE_SIZE + ")");
        System.out.println("  GET /origins");
        System.out.println("  GET /venues");
        System.out.println("  GET /transport");
        System.out.println("  GET /concerts/{id}/transport  [legacy]");
        System.out.println("  GET /recommendations/{id}      [legacy]");
    }

    private record PaginationRequest(int page, int size) {
    }
}