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
        if (!eventStoreExists()) {
            printMissingEventStore();
            return 0;
        }

        int total = loadTopics();

        System.out.println("[EventStoreLoader] Carga histórica completada. Total: " + total);
        return total;
    }

    private boolean eventStoreExists() {
        return Files.exists(eventstoreRoot);
    }

    private void printMissingEventStore() {
        System.out.println("[EventStoreLoader] Event Store no encontrado en: "
                + eventstoreRoot.toAbsolutePath());
        System.out.println("[EventStoreLoader] Sugerencia: ejecutar primero "
                + "eventstore-builder y los publishers para generar datos.");
    }

    private int loadTopics() {
        int total = 0;

        for (String topic : topics) {
            int count = loadTopic(topic);
            printTopicSummary(topic, count);
            total += count;
        }

        return total;
    }

    private void printTopicSummary(String topic, int count) {
        System.out.println("[EventStoreLoader] Topic " + topic
                + ": " + count + " líneas procesadas.");
    }

    private int loadTopic(String topic) {
        Path topicPath = eventstoreRoot.resolve(topic);

        if (!Files.exists(topicPath)) {
            System.out.println("[EventStoreLoader] Sin histórico para topic: " + topic);
            return 0;
        }

        return loadTopicFiles(topic, topicPath);
    }

    private int loadTopicFiles(String topic, Path topicPath) {
        try {
            return loadFiles(topic, eventFilesIn(topicPath));
        } catch (Exception exception) {
            System.err.println("[EventStoreLoader] Error recorriendo topic "
                    + topic + ": " + exception.getMessage());
            return 0;
        }
    }

    private List<Path> eventFilesIn(Path topicPath) throws Exception {
        try (var walk = Files.walk(topicPath)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(this::isEventFile)
                    .sorted()
                    .toList();
        }
    }

    private boolean isEventFile(Path path) {
        return path.toString().endsWith(".events");
    }

    private int loadFiles(String topic, List<Path> files) {
        int count = 0;

        for (Path file : files) {
            count += loadFile(topic, file);
        }

        return count;
    }

    private int loadFile(String topic, Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return loadLines(topic, reader);
        } catch (Exception exception) {
            System.err.println("[EventStoreLoader] Error leyendo "
                    + file + ": " + exception.getMessage());
            return 0;
        }
    }

    private int loadLines(String topic, BufferedReader reader) throws Exception {
        int count = 0;
        String line;

        while ((line = reader.readLine()) != null) {
            count += processLine(topic, line);
        }

        return count;
    }

    private int processLine(String topic, String line) {
        if (line.isBlank()) {
            return 0;
        }

        processor.process(topic, line);
        return 1;
    }
}