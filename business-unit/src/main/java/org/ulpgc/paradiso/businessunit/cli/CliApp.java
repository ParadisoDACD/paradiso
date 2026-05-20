package org.ulpgc.paradiso.businessunit.cli;

import org.ulpgc.paradiso.businessunit.datamart.ConcertRecord;
import org.ulpgc.paradiso.businessunit.datamart.ConcertRoutePlanRecord;
import org.ulpgc.paradiso.businessunit.datamart.Datamart;
import org.ulpgc.paradiso.businessunit.datamart.OriginRecord;
import org.ulpgc.paradiso.businessunit.service.ConcertTransportService;
import org.ulpgc.paradiso.businessunit.service.RecommendationFilter;
import org.ulpgc.paradiso.businessunit.utils.StringUtils;

import java.util.Comparator;
import java.util.List;

public class CliApp {

    private static final int MAX_UPCOMING = 20;
    private static final int MAX_ROUTES = 8;

    private final Datamart datamart;
    private final ConcertTransportService service;
    private final CliConsole console;
    private final CliFormatter formatter;

    public CliApp(Datamart datamart, ConcertTransportService service) {
        this.datamart = datamart;
        this.service = service;
        console = new CliConsole();
        formatter = new CliFormatter();
    }

    public void run() {
        try {
            runMenu();
        } finally {
            console.close();
        }
    }

    private void runMenu() {
        console.printBanner();
        printDatamartStatus();
        while (showMainMenu()) { }
        console.printEmpty();
        console.printSuccess("Hasta pronto. ¡Disfruta del concierto!");
    }

    private boolean showMainMenu() {
        console.printTitle("¿Qué quieres hacer?");
        console.printNumbered(1, "Ver próximos conciertos en Londres");
        console.printNumbered(2, "Buscar conciertos por artista, recinto o nombre");
        console.printNumbered(3, "Salir");
        console.printEmpty();
        return selectedMainOption();
    }

    private boolean selectedMainOption() {
        return switch (console.readInt("→ Elige una opción [1-3]:", 1, 3)) {
            case 1 -> showConcertListAndContinue(null);
            case 2 -> showSearchFlowAndContinue();
            default -> false;
        };
    }

    private boolean showConcertListAndContinue(String query) {
        showConcertList(query);
        return true;
    }

    private boolean showSearchFlowAndContinue() {
        showSearchFlow();
        return true;
    }

    private void showSearchFlow() {
        console.printTitle("Buscar conciertos");
        String query = console.readLine("→ Introduce artista, recinto o palabra clave:");
        if (query.isBlank()) {
            console.printWarning("La búsqueda está vacía. Volviendo al menú principal.");
            return;
        }
        showConcertList(query);
    }

    private void showConcertList(String query) {
        List<ConcertRecord> concerts = service.upcomingConcerts(query, MAX_UPCOMING);
        if (concerts.isEmpty()) {
            printNoConcertsWarning(query);
            return;
        }
        selectConcert(concerts, query);
    }

    private void selectConcert(List<ConcertRecord> concerts, String query) {
        printConcertOptions(concerts, query);
        int choice = console.readInt("→ Elige un concierto [1-" + (concerts.size() + 1) + "]:",
                1, concerts.size() + 1);
        if (choice <= concerts.size()) showConcertDetail(concerts.get(choice - 1));
    }

    private void printConcertOptions(List<ConcertRecord> concerts, String query) {
        console.printTitle(concertListTitle(query));
        for (int i = 0; i < concerts.size(); i++) {
            console.printNumbered(i + 1, formatter.concertSummary(concerts.get(i)));
        }
        console.printNumbered(concerts.size() + 1, "↩ Volver al menú principal");
        console.printEmpty();
    }

    private String concertListTitle(String query) {
        return query == null ? "Próximos conciertos en Londres" : "Resultados: " + query;
    }

    private void showConcertDetail(ConcertRecord concert) {
        console.printSeparator();
        console.printLine(formatter.concertDetail(concert));
        console.printSeparator();
        List<OriginRecord> origins = datamart.origins();
        if (origins.isEmpty()) {
            printNoOriginsWarning();
            return;
        }
        showOriginSelection(concert, origins);
    }

    private void showOriginSelection(ConcertRecord concert, List<OriginRecord> origins) {
        printOriginOptions(origins);
        int choice = console.readInt("→ Elige un origen [1-" + (origins.size() + 1) + "]:",
                1, origins.size() + 1);
        if (choice <= origins.size()) showRoutes(concert, origins.get(choice - 1));
    }

    private void printOriginOptions(List<OriginRecord> origins) {
        console.printTitle("¿Desde dónde quieres salir?");
        for (int i = 0; i < origins.size(); i++) {
            console.printNumbered(i + 1, formatter.originSummary(origins.get(i)));
        }
        console.printNumbered(origins.size() + 1, "↩ Volver a la lista de conciertos");
        console.printEmpty();
    }

    private void showRoutes(ConcertRecord concert, OriginRecord origin) {
        RecommendationFilter filter = new RecommendationFilter(
                concert.externalEventId(), null, origin.originKey(), null, null, null);
        List<ConcertRoutePlanRecord> routes = service.recommendations(filter);
        printRouteSection(concert, origin, routes);
        console.readLine("→ Pulsa Intro para continuar...");
    }

    private void printRouteSection(ConcertRecord concert, OriginRecord origin,
                                   List<ConcertRoutePlanRecord> routes) {
        console.printSeparator();
        console.printTitle("Rutas desde " + originLabel(origin)
                + " hasta " + StringUtils.safe(concert.venueName()));
        if (routes.isEmpty()) printNoRoutesWarning();
        else printTopRoutes(routes);
        console.printSeparator();
    }

    private void printTopRoutes(List<ConcertRoutePlanRecord> routes) {
        List<ConcertRoutePlanRecord> top = topRoutes(routes);
        console.printLine("  Mostrando las " + top.size()
                + " mejores de " + routes.size() + " rutas, ordenadas por puntuación.");
        console.printEmpty();
        for (int i = 0; i < top.size(); i++) console.printLine(formatter.routeDetail(top.get(i), i + 1));
    }

    private List<ConcertRoutePlanRecord> topRoutes(List<ConcertRoutePlanRecord> routes) {
        return routes.stream()
                .sorted(Comparator.comparing(this::safeScore).reversed())
                .limit(MAX_ROUTES)
                .toList();
    }

    private double safeScore(ConcertRoutePlanRecord route) {
        return route.score() == null ? 0.0 : route.score();
    }

    private void printDatamartStatus() {
        if (datamart.concertCount() == 0) {
            console.printWarning("El datamart está vacío. Ejecuta primero los feeders o revisa el Event Store.");
            return;
        }
        console.printSuccess("Datamart cargado: " + datamart.concertCount() + " conciertos, "
                + datamart.originCount() + " orígenes TfL, "
                + datamart.planCount() + " recomendaciones precalculadas.");
        console.printEmpty();
    }

    private void printNoConcertsWarning(String query) {
        String context = query == null ? "en el datamart" : "para «" + query + "»";
        console.printWarning("No hay conciertos disponibles " + context + ".");
        console.printDim("  Revisa que ticketmaster-feeder haya publicado eventos y que el Event Store sea correcto.");
    }

    private void printNoOriginsWarning() {
        console.printWarning("No hay orígenes TfL disponibles en el datamart.");
        console.printDim("  Revisa que tfl-feeder haya publicado rutas.");
    }

    private void printNoRoutesWarning() {
        console.printWarning("No se encontraron rutas para esta combinación.");
        console.printDim("  Prueba con otro origen o consulta más tarde.");
    }

    private String originLabel(OriginRecord origin) {
        String name = StringUtils.safe(origin.originName());
        return name.isBlank() ? StringUtils.safe(origin.originKey()) : name;
    }
}