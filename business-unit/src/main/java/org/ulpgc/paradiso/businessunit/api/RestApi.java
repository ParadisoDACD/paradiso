package org.ulpgc.paradiso.businessunit.api;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.DatamartStatus;
import org.ulpgc.paradiso.businessunit.service.ConcertTransportResponse;
import org.ulpgc.paradiso.businessunit.service.ConcertTransportService;

import java.util.Map;

public class RestApi {

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
                "endpoints", Map.of(
                        "status", "/status",
                        "concerts", "/concerts",
                        "transport", "/transport",
                        "route", "/concerts/{id}/transport"
                )
        )));

        app.get("/status", ctx -> json(ctx, new DatamartStatus(
                datamart.concertCount(),
                datamart.transportCount(),
                datamart.lastProcessedAt()
        )));

        app.get("/concerts", ctx -> json(ctx, datamart.concerts()));

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

        app.get("/transport", ctx -> json(ctx, datamart.transports()));

        System.out.println("[BusinessUnit] REST API disponible en http://localhost:" + port);
        System.out.println("[BusinessUnit] Endpoints: GET /status | /concerts | /transport | /concerts/{id}/transport");
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
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