package org.ulpgc.paradiso.eventstorebuilder.store;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class JsonLinesEventFileStore implements EventFileStore {

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final Path root;

    public JsonLinesEventFileStore(String eventstorePath) {
        this.root = Path.of(eventstorePath);
    }

    @Override
    public synchronized void append(String topic, String jsonEvent) throws Exception {
        JsonObject rootObject = JsonParser.parseString(jsonEvent).getAsJsonObject();

        String ts = getRequiredString(rootObject, "ts");
        String ss = getRequiredString(rootObject, "ss");

        Instant instant = Instant.parse(ts);
        String yyyymmdd = FILE_DATE_FORMAT.format(instant);

        Path targetFile = root
                .resolve(sanitize(topic))
                .resolve(sanitize(ss))
                .resolve(yyyymmdd + ".events");

        Files.createDirectories(targetFile.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(
                targetFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(jsonEvent);
            writer.newLine();
        }
    }

    private String getRequiredString(JsonObject object, String field) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            throw new IllegalArgumentException(
                    "El evento JSON recibido no contiene el campo obligatorio '" + field + "'."
            );
        }
        return object.get(field).getAsString();
    }

    private String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}