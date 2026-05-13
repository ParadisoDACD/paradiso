package org.ulpgc.paradiso.tfl.feeder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TflJourneyFeeder implements JourneyFeeder {

    private static final String BASE_URL =
            "https://api.tfl.gov.uk/Journey/JourneyResults";

    private final String appKey;
    private final OkHttpClient httpClient;

    public TflJourneyFeeder(String appKey) {
        this.appKey = appKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String fetchRawJourneys(String fromNaptan,
                                   String toNaptan,
                                   String date,
                                   String time) throws Exception {

        String url = BASE_URL
                + "/" + fromNaptan
                + "/to/" + toNaptan
                + "?app_key=" + appKey
                + "&date=" + date
                + "&time=" + time
                + "&timeIs=Departing"
                + "&journeyPreference=LeastTime"
                + "&mode=tube,bus,overground,elizabeth-line,dlr,tram,national-rail";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 429) {
                throw new Exception("Limite de peticiones TfL alcanzado (429). " +
                        "Verifica que has suscrito el plan '500 Requests per min'.");
            }

            if (response.code() == 401 || response.code() == 403) {
                throw new Exception("Autenticacion TfL fallida (" + response.code()
                        + "). Verifica tu app.key.");
            }

            if (response.code() == 300 || response.code() == 404) {
                return "{}";
            }

            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new Exception("HTTP " + response.code()
                        + " en TfL [" + fromNaptan + " -> " + toNaptan + "]: " + body);
            }

            return Objects.requireNonNull(response.body()).string();
        }
    }
}