package org.ulpgc.paradiso.tfl.feeder;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class TflVenueResolver {

    private static final Map<String, String> NAPTAN_IDS = new LinkedHashMap<>();
    private static final Map<String, String> NORMALIZED_TO_LOGICAL_NAME = new HashMap<>();

    static {
        register("KingsCross", "940GZZLUKSX");
        registerAlias("King's Cross", "KingsCross");
        registerAlias("Kings Cross", "KingsCross");
        registerAlias("King's Cross St Pancras", "KingsCross");

        register("Victoria", "940GZZLUVIC");
        register("Waterloo", "940GZZLUWLO");
        register("Paddington", "940GZZLUPAC");

        register("LondonBridge", "940GZZLULNB");
        registerAlias("London Bridge", "LondonBridge");

        register("LiverpoolStreet", "940GZZLULVT");
        registerAlias("Liverpool Street", "LiverpoolStreet");

        register("Euston", "940GZZLUEUS");
        register("Marylebone", "940GZZLUMYB");

        register("CharingCross", "940GZZLUCHX");
        registerAlias("Charing Cross", "CharingCross");

        register("Stratford", "940GZZLUSTD");

        register("CanaryWharf", "940GZZLUCYF");
        registerAlias("Canary Wharf", "CanaryWharf");

        register("BakerStreet", "940GZZLUBST");
        registerAlias("Baker Street", "BakerStreet");

        register("OxfordCircus", "940GZZLUOXC");
        registerAlias("Oxford Circus", "OxfordCircus");

        register("PiccadillyCircus", "940GZZLUPCC");
        registerAlias("Piccadilly Circus", "PiccadillyCircus");

        register("LeicesterSquare", "940GZZLULSQ");
        registerAlias("Leicester Square", "LeicesterSquare");

        register("TottenhamCourtRoad", "940GZZLUTCR");
        registerAlias("Tottenham Court Road", "TottenhamCourtRoad");

        register("Farringdon", "940GZZLUFCN");
        register("Blackfriars", "940GZZLUBKF");
        register("Westminster", "940GZZLUWSM");
        register("Bank", "940GZZLUBNK");
        register("Moorgate", "940GZZLUMGT");

        register("GreenPark", "940GZZLUGPK");
        registerAlias("Green Park", "GreenPark");

        register("Holborn", "940GZZLUHBN");

        register("SouthKensington", "940GZZLUSKS");
        registerAlias("South Kensington", "SouthKensington");

        register("Hammersmith", "940GZZLUHSD");

        register("ShepherdsBush", "940GZZLUSBC");
        registerAlias("Shepherd's Bush", "ShepherdsBush");
        registerAlias("Shepherds Bush", "ShepherdsBush");

        register("Whitechapel", "940GZZLUWPL");

        register("CamdenTown", "940GZZLUCTN");
        registerAlias("Camden Town", "CamdenTown");

        register("NottingHillGate", "940GZZLUNHG");
        registerAlias("Notting Hill Gate", "NottingHillGate");

        register("Heathrow", "940GZZLUHRC");
        registerAlias("Heathrow Terminals 2 & 3", "Heathrow");
        registerAlias("Heathrow Terminal 2", "Heathrow");
        registerAlias("Heathrow Terminal 3", "Heathrow");

        register("O2Arena", "940GZZLUNGW");
        registerAlias("The O2", "O2Arena");
        registerAlias("O2 Arena", "O2Arena");
        registerAlias("North Greenwich", "O2Arena");

        register("WembleyPark", "940GZZLUWYP");
        registerAlias("Wembley Park", "WembleyPark");
        registerAlias("Wembley", "WembleyPark");

        register("RoyalAlbertHall", "940GZZLUHSK");
        registerAlias("Royal Albert Hall", "RoyalAlbertHall");
        registerAlias("High Street Kensington", "RoyalAlbertHall");

        register("BrixtonAcademy", "940GZZLUBXN");
        registerAlias("Brixton Academy", "BrixtonAcademy");
        registerAlias("O2 Academy Brixton", "BrixtonAcademy");
        registerAlias("Brixton", "BrixtonAcademy");

        register("AlexandraPalace", "910GALEXNDP");
        registerAlias("Alexandra Palace", "AlexandraPalace");
    }

    public static String resolve(String logicalName) {
        String id = resolveOrNull(logicalName);
        if (id == null) {
            throw new IllegalArgumentException(
                    "Nombre '" + logicalName + "' sin NaPTAN ID en TflVenueResolver. " +
                            "Búscalo en la API y añádelo al mapa.");
        }
        return id;
    }

    private static String resolveOrNull(String logicalName) {
        if (logicalName == null || logicalName.isBlank()) {
            return null;
        }
        String direct = NAPTAN_IDS.get(logicalName.trim());
        if (direct != null) {
            return direct;
        }
        String canonicalName = NORMALIZED_TO_LOGICAL_NAME.get(normalize(logicalName));
        if (canonicalName == null) {
            return null;
        }
        return NAPTAN_IDS.get(canonicalName);
    }

    private static void register(String logicalName, String naptanId) {
        NAPTAN_IDS.put(logicalName, naptanId);
        registerAlias(logicalName, logicalName);
    }

    private static void registerAlias(String alias, String logicalName) {
        NORMALIZED_TO_LOGICAL_NAME.put(normalize(alias), logicalName);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}