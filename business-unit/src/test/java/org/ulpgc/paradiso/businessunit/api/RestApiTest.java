package org.ulpgc.paradiso.businessunit.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRoutePlanRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.service.ConcertTransportService;
import org.ulpgc.paradiso.businessunit.recommendation.MatchType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestApiTest {

    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newHttpClient();

    private RestApi api;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        port = freePort();

        Datamart datamart = new Datamart();
        seedDatamart(datamart);

        api = new RestApi(datamart, new ConcertTransportService(datamart), port);
        api.start();
    }

    @AfterEach
    void tearDown() {
        api.stop();
    }

    @Test
    void statusReturnsDatamartCounters() throws Exception {
        HttpResponse<String> response = get("/status");

        assertEquals(200, response.statusCode());

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        assertEquals(1, json.get("concerts").getAsInt());
        assertEquals(0, json.get("transports").getAsInt());
        assertEquals(0, json.get("origins").getAsInt());
        assertEquals(2, json.get("routePlans").getAsInt());
    }

    @Test
    void recommendationsReturnsPrecomputedPlans() throws Exception {
        HttpResponse<String> response = get("/recommendations");

        assertEquals(200, response.statusCode());

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        assertEquals(2, json.get("count").getAsInt());
        assertEquals(2, json.getAsJsonArray("results").size());
    }

    @Test
    void recommendationsPaginationLimitsPageSize() throws Exception {
        HttpResponse<String> response = get("/recommendations?page=0&size=999");

        assertEquals(200, response.statusCode());

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        assertEquals(0, json.get("page").getAsInt());
        assertEquals(100, json.get("size").getAsInt());
        assertEquals(1, json.get("totalPages").getAsInt());
        assertEquals(2, json.getAsJsonArray("results").size());
    }

    @Test
    void routesByConcertReturnsPlansForExistingConcert() throws Exception {
        HttpResponse<String> response = get("/concerts/TM-001/routes");

        assertEquals(200, response.statusCode());

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        assertEquals(2, json.get("count").getAsInt());
        assertEquals("TM-001", json.getAsJsonObject("query").get("eventId").getAsString());
    }

    @Test
    void routesByConcertReturnsNotFoundForUnknownConcert() throws Exception {
        HttpResponse<String> response = get("/concerts/UNKNOWN/routes");

        assertEquals(404, response.statusCode());

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        assertTrue(json.get("error").getAsString().contains("Concierto no encontrado"));
    }

    @Test
    void recommendationsByArtistReturnsMatchingPlans() throws Exception {
        HttpResponse<String> response = get("/artists/Arctic%20Monkeys/recommendations");

        assertEquals(200, response.statusCode());

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        assertEquals(2, json.get("count").getAsInt());
        assertEquals("Arctic Monkeys", json.getAsJsonObject("query").get("artist").getAsString());
    }

    private void seedDatamart(Datamart datamart) {
        datamart.upsertConcert(new ConcertRecord(
                "TM-001",
                "Arctic Monkeys",
                "music",
                "Music",
                "Rock",
                "London",
                "GB",
                "The O2",
                "https://example.com/tm-001",
                "2026-07-15",
                "20:00:00",
                "2026-07-15T19:00:00Z",
                "music",
                "2026-05-14T12:00:00Z"
        ));

        datamart.upsertPlans(List.of(
                routePlan(
                        "PLAN-001",
                        "KingsCross",
                        "King's Cross St. Pancras",
                        38,
                        0.94
                ),
                routePlan(
                        "PLAN-002",
                        "Paddington",
                        "Paddington",
                        45,
                        0.88
                )
        ));
    }

    private ConcertRoutePlanRecord routePlan(String planId,
                                             String originKey,
                                             String originName,
                                             Integer durationMinutes,
                                             Double score) {
        return new ConcertRoutePlanRecord(
                planId,
                "TM-001",
                "Arctic Monkeys",
                "Arctic Monkeys",
                "Rock",
                "The O2",
                "the_o2",
                "2026-07-15",
                "20:00:00",
                "2026-07-15T19:00:00Z",
                originKey,
                originName,
                "O2Arena",
                "North Greenwich",
                "journey-" + planId,
                "2026-07-15T18:10:00",
                "2026-07-15T18:48:00",
                durationMinutes,
                1,
                "tube",
                score,
                MatchType.EXACT_VENUE_STOP,
                "2026-05-14T12:00:00Z"
        );
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}