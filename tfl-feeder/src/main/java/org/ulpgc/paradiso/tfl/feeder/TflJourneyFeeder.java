package org.ulpgc.paradiso.tfl.feeder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.ulpgc.paradiso.tfl.config.TflConfig;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TflJourneyFeeder implements JourneyFeeder {

    private static final String BASE_URL = "https://api.tfl.gov.uk/Journey/JourneyResults";

    private final String appKey;
    private final OkHttpClient httpClient;
    private final int maxRetries;
    private final long retryBackoffMillis;

    public TflJourneyFeeder(TflConfig config) {
        this(config.getAppKey(), httpConfigFrom(config));
    }

    public TflJourneyFeeder(String appKey) {
        this(appKey, HttpConfig.defaults());
    }

    private TflJourneyFeeder(String appKey, HttpConfig config) {
        this.appKey = appKey;
        this.maxRetries = config.maxRetries();
        this.retryBackoffMillis = config.retryBackoffMillis();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.connectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(config.readTimeoutSeconds(), TimeUnit.SECONDS)
                .callTimeout(config.callTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String fetchRawJourneys(TflJourneyRequest request) throws Exception {
        Request httpRequest = new Request.Builder().url(buildUrl(request)).get().build();
        return executeWithRetry(httpRequest, request);
    }

    private String executeWithRetry(Request httpRequest, TflJourneyRequest request) throws Exception {
        Exception lastFailure = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return executeRequest(httpRequest, request);
            } catch (SocketTimeoutException exception) {
                lastFailure = exception;
                logRetry("timeout", attempt);
            } catch (IOException exception) {
                lastFailure = exception;
                logRetry("error de red: " + exception.getMessage(), attempt);
            } catch (RetryableTflException exception) {
                lastFailure = exception;
                logRetry(exception.getMessage(), attempt);
            }
            if (attempt < maxRetries) sleepBeforeRetry(attempt);
        }
        throw new Exception("TfL no respondió tras " + (maxRetries + 1) + " intentos: "
                + (lastFailure != null ? lastFailure.getMessage() : "error desconocido"));
    }

    private String buildUrl(TflJourneyRequest request) {
        return BASE_URL
                + "/" + request.fromNaptan() + "/to/" + request.toNaptan()
                + "?app_key=" + appKey
                + "&date=" + request.date() + "&time=" + request.time()
                + "&timeIs=Departing&journeyPreference=LeastTime"
                + "&mode=tube,bus,overground,elizabeth-line,dlr,tram,national-rail";
    }

    private String executeRequest(Request httpRequest, TflJourneyRequest request) throws Exception {
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return handleResponse(response, request);
        }
    }

    private String handleResponse(Response response, TflJourneyRequest request) throws Exception {
        int code = response.code();
        if (code == 429) throw new Exception("Límite de peticiones TfL alcanzado (429).");
        if (code == 401 || code == 403) throw new Exception("Autenticación TfL fallida (" + code + ").");
        if (code == 300 || code == 404) return "{}";
        if (code >= 500) throw new RetryableTflException("HTTP " + code + " temporal en TfL");
        if (!response.isSuccessful()) {
            String body = response.body() != null ? response.body().string() : "";
            throw new Exception("HTTP " + code + " en TfL ["
                    + request.fromNaptan() + " -> " + request.toNaptan() + "]: " + body);
        }
        return Objects.requireNonNull(response.body()).string();
    }

    private void logRetry(String reason, int attempt) {
        if (attempt < maxRetries)
            System.err.println("  [TfL] Aviso: " + reason
                    + ". Reintentando (" + (attempt + 1) + "/" + maxRetries + ")...");
    }

    private void sleepBeforeRetry(int attempt) throws InterruptedException {
        Thread.sleep(retryBackoffMillis * (attempt + 1));
    }

    private static HttpConfig httpConfigFrom(TflConfig config) {
        return new HttpConfig(
                config.getHttpConnectTimeoutSeconds(),
                config.getHttpReadTimeoutSeconds(),
                config.getHttpCallTimeoutSeconds(),
                config.getRequestMaxRetries(),
                config.getRequestRetryBackoffMillis()
        );
    }

    private record HttpConfig(int connectTimeoutSeconds,
                              int readTimeoutSeconds,
                              int callTimeoutSeconds,
                              int maxRetries,
                              long retryBackoffMillis) {
        static HttpConfig defaults() {
            return new HttpConfig(10, 45, 60, 2, 1000);
        }
    }

    private static class RetryableTflException extends Exception {
        private RetryableTflException(String message) { super(message); }
    }
}