package org.ulpgc.paradiso.ticketmaster.feeder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TicketmasterDiscoveryFeeder implements EventFeeder {

    private final String baseUrl;
    private final String apiKey;
    private final OkHttpClient httpClient;

    public TicketmasterDiscoveryFeeder(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String fetchRawEvents(TicketmasterSearchRequest request) throws Exception {
        String url = buildUrl(request);
        Request httpRequest = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return responseBodyOrFail(response, request.city());
        }
    }

    private String buildUrl(TicketmasterSearchRequest request) {
        return baseUrl
                + "?apikey=" + apiKey
                + "&countryCode=" + request.countryCode()
                + "&city=" + encode(request.city())
                + "&classificationName=" + encode(request.category())
                + "&startDateTime=" + request.startDateTime()
                + "&endDateTime=" + request.endDateTime()
                + "&page=" + request.page()
                + "&size=" + request.size()
                + "&locale=*";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String responseBodyOrFail(Response response, String city) throws Exception {
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