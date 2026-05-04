package org.ulpgc.paradiso.tfl.feeder;

import java.util.HashMap;
import java.util.Map;

public class TflVenueResolver {

    private static final Map<String, String> NAPTAN_IDS = new HashMap<>();

    static {
        NAPTAN_IDS.put("KingsCross", "940GZZLUKSX");
        NAPTAN_IDS.put("Victoria", "940GZZLUVIC");
        NAPTAN_IDS.put("Waterloo", "940GZZLUWLO");
        NAPTAN_IDS.put("Paddington", "940GZZLUPAC");
        NAPTAN_IDS.put("LondonBridge", "940GZZLULBG");

        NAPTAN_IDS.put("O2Arena", "940GZZLUNGW");
        NAPTAN_IDS.put("WembleyPark", "940GZZLUWMP");
        NAPTAN_IDS.put("RoyalAlbertHall", "940GZZLUHSK");
        NAPTAN_IDS.put("BrixtonAcademy", "940GZZLUBXN");
        NAPTAN_IDS.put("AlexandraPalace", "910GAXLPALLY");
    }

    public static String resolve(String logicalName) {
        String id = NAPTAN_IDS.get(logicalName);
        if (id == null) {
            throw new IllegalArgumentException(
                    "Nombre '" + logicalName + "' sin NaPTAN ID en TflVenueResolver. " +
                            "Buscalo en la API y añadelo al mapa.");
        }
        return id;
    }
}