package org.ulpgc.paradiso.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Configuration {

    private final Properties properties = new Properties();
    private final LocalEnvironment localEnvironment = new LocalEnvironment();

    private Configuration(String propertiesFileName) {
        loadProperties(propertiesFileName);
    }

    public static Configuration fromProperties(String propertiesFileName) {
        return new Configuration(propertiesFileName);
    }

    public String required(String envKey, String propKey) {
        String result = value(envKey, propKey, "");
        if (!result.isBlank()) return result;
        throw new IllegalStateException("Configuración requerida no definida: "
                + envKey + " o " + propKey);
    }

    public String value(String envKey, String propKey, String defaultValue) {
        String systemValue = System.getenv(envKey);
        if (hasText(systemValue)) return systemValue.trim();
        String localValue = localEnvironment.get(envKey);
        if (hasText(localValue)) return localValue.trim();
        return properties.getProperty(propKey, defaultValue).trim();
    }

    public int integer(String envKey, String propKey, int defaultValue) {
        try {
            return Integer.parseInt(value(envKey, propKey, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            warnInvalid(propKey, Integer.toString(defaultValue));
            return defaultValue;
        }
    }

    public long longValue(String envKey, String propKey, long defaultValue) {
        try {
            return Long.parseLong(value(envKey, propKey, Long.toString(defaultValue)));
        } catch (NumberFormatException e) {
            warnInvalid(propKey, Long.toString(defaultValue));
            return defaultValue;
        }
    }

    public boolean bool(String envKey, String propKey, boolean defaultValue) {
        String raw = value(envKey, propKey, Boolean.toString(defaultValue));
        if (raw.equalsIgnoreCase("true")) return true;
        if (raw.equalsIgnoreCase("false")) return false;
        warnInvalid(propKey, Boolean.toString(defaultValue));
        return defaultValue;
    }

    public List<String> commaSeparated(String envKey, String propKey, String defaultValue) {
        String raw = value(envKey, propKey, defaultValue);
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private void loadProperties(String propertiesFileName) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(propertiesFileName)) {
            if (in != null) properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo " + propertiesFileName, e);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void warnInvalid(String propKey, String defaultValue) {
        System.err.println("[Config] " + propKey + " inválido, usando " + defaultValue);
    }
}