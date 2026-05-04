package org.ulpgc.paradiso.ticketmaster.feeder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TicketmasterDiscoveryFeeder implements EventFeeder {

    private static final String BASE_URL =
            "https://app.ticketmaster.com/discovery/v2/events.json";

    private final String apiKey;
    private final OkHttpClient httpClient;

    public TicketmasterDiscoveryFeeder(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String fetchRawEvents(String countryCode,
                                 String city,
                                 String category,
                                 String startDateTime,
                                 String endDateTime,
                                 int page,
                                 int size) throws Exception {

        String url = BASE_URL
                + "?apikey=" + apiKey
                + "&countryCode=" + countryCode
                + "&city=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                + "&classificationName=" + URLEncoder.encode(category, StandardCharsets.UTF_8)
                + "&startDateTime=" + startDateTime
                + "&endDateTime=" + endDateTime
                + "&page=" + page
                + "&size=" + size
                + "&locale=*";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 429) {
                throw new Exception("Límite de peticiones Ticketmaster alcanzado (429). Espera un momento.");
            }

            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new Exception("HTTP " + response.code()
                        + " en Ticketmaster [" + city + "]: " + body);
            }

            return Objects.requireNonNull(response.body()).string();
        }
    }
}