package org.ulpgc.paradiso.eventstorebuilder.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public class LocalEnvironment {

    private static final String ENV_FILENAME = ".env";
    private static final String EXPORT_PREFIX = "export ";

    private final Properties values = new Properties();

    public LocalEnvironment() {
        findEnvFile().ifPresent(this::load);
    }

    public String get(String key) {
        return values.getProperty(key);
    }

    private Optional<Path> findEnvFile() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(ENV_FILENAME);
            if (Files.isRegularFile(candidate)) return Optional.of(candidate);
            current = current.getParent();
        }
        return Optional.empty();
    }

    private void load(Path path) {
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines.map(String::trim)
                    .filter(this::isConfigLine)
                    .forEach(this::putLine);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo .env", e);
        }
    }

    private boolean isConfigLine(String line) {
        return !line.isBlank() && !line.startsWith("#") && line.contains("=");
    }

    private void putLine(String line) {
        String normalized = normalize(line);
        int separator = normalized.indexOf('=');
        String key = normalized.substring(0, separator).trim();
        String value = cleanValue(normalized.substring(separator + 1).trim());
        if (!key.isBlank()) values.setProperty(key, value);
    }

    private String normalize(String line) {
        if (!line.startsWith(EXPORT_PREFIX)) return line;
        return line.substring(EXPORT_PREFIX.length()).trim();
    }

    private String cleanValue(String raw) {
        if (raw.length() < 2) return raw;
        if (isQuoted(raw, '"')) return raw.substring(1, raw.length() - 1);
        if (isQuoted(raw, '\'')) return raw.substring(1, raw.length() - 1);
        return raw;
    }

    private boolean isQuoted(String value, char quote) {
        return value.charAt(0) == quote && value.charAt(value.length() - 1) == quote;
    }
}