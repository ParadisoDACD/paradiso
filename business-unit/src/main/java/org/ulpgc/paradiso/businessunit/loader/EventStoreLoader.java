package org.ulpgc.paradiso.businessunit.loader;

import org.ulpgc.paradiso.businessunit.event.BusinessEventProcessor;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EventStoreLoader {

    private final Path eventstoreRoot;
    private final List<String> topics;
    private final BusinessEventProcessor processor;

    public EventStoreLoader(String eventstorePath,
                            List<String> topics,
                            BusinessEventProcessor processor) {
        this.eventstoreRoot = Path.of(eventstorePath);
        this.topics = topics;
        this.processor = processor;
    }

    public int loadAll() {
        if (!Files.exists(eventstoreRoot)) {
            System.out.println("[EventStoreLoader] Event Store no encontrado en: "
                    + eventstoreRoot.toAbsolutePath());
            System.out.println("[EventStoreLoader] Sugerencia: ejecutar primero "
                    + "eventstore-builder-module y los publishers para generar datos.");
            return 0;
        }

        int total = 0;

        for (String topic : topics) {
            int count = loadTopic(topic);
            System.out.println("[EventStoreLoader] Topic " + topic
                    + ": " + count + " líneas procesadas.");
            total += count;
        }

        System.out.println("[EventStoreLoader] Carga histórica completada. Total: " + total);
        return total;
    }

    private int loadTopic(String topic) {
        Path topicPath = eventstoreRoot.resolve(topic);

        if (!Files.exists(topicPath)) {
            System.out.println("[EventStoreLoader] Sin histórico para topic: " + topic);
            return 0;
        }

        try {
            List<Path> files;

            try (var walk = Files.walk(topicPath)) {
                files = walk
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".events"))
                        .sorted()
                        .toList();
            }

            int count = 0;

            for (Path file : files) {
                count += loadFile(topic, file);
            }

            return count;

        } catch (Exception e) {
            System.err.println("[EventStoreLoader] Error recorriendo topic "
                    + topic + ": " + e.getMessage());
            return 0;
        }
    }

    private int loadFile(String topic, Path file) {
        int count = 0;

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                processor.process(topic, line);
                count++;
            }

        } catch (Exception e) {
            System.err.println("[EventStoreLoader] Error leyendo "
                    + file + ": " + e.getMessage());
        }

        return count;
    }
}