package org.ulpgc.paradiso.tfl.config;

import org.ulpgc.paradiso.common.config.Configuration;

import java.util.ArrayList;
import java.util.List;

public class TflConfig {

    private static final String DEFAULT_CAPTURE_TIMES =
            "1530,1600,1630,1700,1730,1800,1815,1830,1845,1900,1915,1930,1945,2000,2030";

    private final Configuration config;

    public TflConfig() {
        config = Configuration.fromProperties("tfl.properties");
    }

    public String getAppKey() {
        return config.required("PARADISO_TFL_APP_KEY", "app.key");
    }

    public String getJourneyBaseUrl() {
        return config.required("PARADISO_TFL_JOURNEY_BASE_URL", "journey.base.url");
    }

    public int getCapturePeriodMinutes() {
        return config.integer("PARADISO_TFL_CAPTURE_PERIOD_MINUTES",
                "capture.period.minutes", 90);
    }

    public List<String> getCaptureTimes() {
        return config.commaSeparated("PARADISO_TFL_CAPTURE_TIMES",
                "capture.times", DEFAULT_CAPTURE_TIMES);
    }

    public int getCaptureStartDayOffset() {
        return config.integer("PARADISO_TFL_CAPTURE_START_DAY_OFFSET",
                "capture.start.day.offset", 0);
    }

    public int getCaptureDaysAhead() {
        return config.integer("PARADISO_TFL_CAPTURE_DAYS_AHEAD",
                "capture.days.ahead", 10);
    }

    public long getRequestSleepMillis() {
        return config.longValue("PARADISO_TFL_REQUEST_SLEEP_MS",
                "request.sleep.ms", 150L);
    }

    public int getHttpConnectTimeoutSeconds() {
        return config.integer("PARADISO_TFL_HTTP_CONNECT_TIMEOUT_SECONDS",
                "http.connect.timeout.seconds", 10);
    }

    public int getHttpReadTimeoutSeconds() {
        return config.integer("PARADISO_TFL_HTTP_READ_TIMEOUT_SECONDS",
                "http.read.timeout.seconds", 45);
    }

    public int getHttpCallTimeoutSeconds() {
        return config.integer("PARADISO_TFL_HTTP_CALL_TIMEOUT_SECONDS",
                "http.call.timeout.seconds", 60);
    }

    public int getRequestMaxRetries() {
        return config.integer("PARADISO_TFL_REQUEST_MAX_RETRIES",
                "request.max.retries", 2);
    }

    public long getRequestRetryBackoffMillis() {
        return config.longValue("PARADISO_TFL_REQUEST_RETRY_BACKOFF_MS",
                "request.retry.backoff.ms", 1_000L);
    }

    public List<String> getOrigins() {
        return config.commaSeparated("PARADISO_TFL_ORIGINS", "origins", "");
    }

    public List<String> getDestinations() {
        return config.commaSeparated("PARADISO_TFL_DESTINATIONS", "destinations", "");
    }

    public List<String[]> getRoutePairs() {
        List<String> origins = getOrigins();
        List<String> destinations = getDestinations();
        if (!origins.isEmpty() && !destinations.isEmpty()) return cartesianRoutes(origins, destinations);
        return getRoutes();
    }

    public List<String[]> getRoutes() {
        List<String[]> routes = new ArrayList<>();
        for (String pair : rawRoutes().split(";")) addRoute(routes, pair);
        return routes;
    }

    public String getBrokerUrl() {
        return config.required("PARADISO_BROKER_URL", "broker.url");
    }

    public String getTopicName() {
        return config.value("PARADISO_TFL_TOPIC", "topic.name", "TflJourney");
    }

    public String getSourceSystem() {
        return config.value("PARADISO_TFL_SOURCE_SYSTEM", "source.system", "tfl-feeder");
    }

    private List<String[]> cartesianRoutes(List<String> origins, List<String> destinations) {
        List<String[]> routePairs = new ArrayList<>();
        for (String origin : origins) addRoutesFromOrigin(routePairs, origin, destinations);
        return routePairs;
    }

    private void addRoutesFromOrigin(List<String[]> routePairs, String origin,
                                     List<String> destinations) {
        for (String destination : destinations) routePairs.add(new String[]{origin, destination});
    }

    private String rawRoutes() {
        return config.value("PARADISO_TFL_ROUTES", "routes", "");
    }

    private void addRoute(List<String[]> routes, String pair) {
        String[] parts = pair.trim().split(">");
        if (parts.length == 2) routes.add(new String[]{parts[0].trim(), parts[1].trim()});
    }
}