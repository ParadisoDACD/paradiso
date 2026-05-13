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

    private static final String BASE_URL =
            "https://api.tfl.gov.uk/Journey/JourneyResults";

    private final String appKey;
    private final OkHttpClient httpClient;
    private final int maxRetries;
    private final long retryBackoffMillis;

    public TflJourneyFeeder(TflConfig config) {
        this(
                config.getAppKey(),
                config.getHttpConnectTimeoutSeconds(),
                config.getHttpReadTimeoutSeconds(),
                config.getHttpCallTimeoutSeconds(),
                config.getRequestMaxRetries(),
                config.getRequestRetryBackoffMillis()
        );
    }

    public TflJourneyFeeder(String appKey) {
        this(appKey, 10, 45, 60, 2, 1000);
    }

    private TflJourneyFeeder(String appKey,
                             int connectTimeoutSeconds,
                             int readTimeoutSeconds,
                             int callTimeoutSeconds,
                             int maxRetries,
                             long retryBackoffMillis) {
        this.appKey = appKey;
        this.maxRetries = maxRetries;
        this.retryBackoffMillis = retryBackoffMillis;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .callTimeout(callTimeoutSeconds, TimeUnit.SECONDS)
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

        Exception lastFailure = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return executeRequest(request, fromNaptan, toNaptan);
            } catch (SocketTimeoutException e) {
                lastFailure = e;
                logRetry("timeout", attempt);
            } catch (IOException e) {
                lastFailure = e;
                logRetry("error de red: " + e.getMessage(), attempt);
            } catch (RetryableTflException e) {
                lastFailure = e;
                logRetry(e.getMessage(), attempt);
            }

            if (attempt < maxRetries) {
                sleepBeforeRetry(attempt);
            }
        }

        throw new Exception("TfL no respondio tras "
                + (maxRetries + 1)
                + " intentos: "
                + (lastFailure != null ? lastFailure.getMessage() : "error desconocido"));
    }

    private String executeRequest(Request request,
                                  String fromNaptan,
                                  String toNaptan) throws Exception {
        try (Response response = httpClient.newCall(request).execute()) {
            int code = response.code();

            if (code == 429) {
                throw new Exception("Limite de peticiones TfL alcanzado (429). "
                        + "Reduce el ritmo de captura o aumenta request.sleep.ms.");
            }

            if (code == 401 || code == 403) {
                throw new Exception("Autenticacion TfL fallida (" + code
                        + "). Verifica app.key.");
            }

            if (code == 300 || code == 404) {
                return "{}";
            }

            if (code >= 500) {
                throw new RetryableTflException("HTTP " + code + " temporal en TfL");
            }

            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new Exception("HTTP " + code
                        + " en TfL [" + fromNaptan + " -> " + toNaptan + "]: " + body);
            }

            return Objects.requireNonNull(response.body()).string();
        }
    }

    private void logRetry(String reason, int attempt) {
        if (attempt < maxRetries) {
            System.err.println("  [TfL] Aviso: " + reason
                    + ". Reintentando (" + (attempt + 1)
                    + "/" + maxRetries + ")...");
        }
    }

    private void sleepBeforeRetry(int attempt) throws InterruptedException {
        long delay = retryBackoffMillis * (attempt + 1);
        Thread.sleep(delay);
    }

    private static class RetryableTflException extends Exception {
        private RetryableTflException(String message) {
            super(message);
        }
    }
}