package org.ulpgc.paradiso.ticketmaster.persistence;

import org.ulpgc.paradiso.ticketmaster.model.TicketmasterCaptureRun;
import org.ulpgc.paradiso.ticketmaster.model.TicketmasterEvent;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.List;

public class SqliteEventStore implements EventStore {

    private final String dbPath;

    public SqliteEventStore(String dbPath) {
        this.dbPath = dbPath;
        File dir = new File(dbPath).getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    @Override
    public void initializeSchema() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ticketmaster_capture_run (
                    capture_batch_id TEXT PRIMARY KEY,
                    started_at       TEXT NOT NULL,
                    finished_at      TEXT,
                    status           TEXT NOT NULL,
                    scope_summary    TEXT,
                    records_fetched  INTEGER DEFAULT 0,
                    records_inserted INTEGER DEFAULT 0,
                    error_message    TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ticketmaster_event_capture (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    capture_batch_id    TEXT    NOT NULL,
                    external_event_id   TEXT    NOT NULL,
                    name                TEXT,
                    classification_name TEXT,
                    segment             TEXT,
                    genre               TEXT,
                    city                TEXT,
                    country_code        TEXT,
                    venue_name          TEXT,
                    event_url           TEXT,
                    local_date          TEXT,
                    local_time          TEXT,
                    date_time_iso       TEXT,
                    source_country      TEXT,
                    source_city         TEXT,
                    source_category     TEXT,
                    captured_at         TEXT    NOT NULL,
                    FOREIGN KEY (capture_batch_id)
                        REFERENCES ticketmaster_capture_run(capture_batch_id)
                )
            """);
        }

        System.out.println("[Ticketmaster] Esquema SQLite listo: " + dbPath);
    }

    @Override
    public void startRun(TicketmasterCaptureRun run) throws Exception {
        String sql = """
            INSERT INTO ticketmaster_capture_run
                (capture_batch_id, started_at, status, scope_summary)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, run.getCaptureBatchId());
            ps.setString(2, run.getStartedAt());
            ps.setString(3, run.getStatus());
            ps.setString(4, run.getScopeSummary());
            ps.executeUpdate();
        }
    }

    @Override
    public void saveAll(List<TicketmasterEvent> events) throws Exception {
        if (events.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO ticketmaster_event_capture
                (capture_batch_id, external_event_id, name, classification_name,
                 segment, genre, city, country_code, venue_name, event_url,
                 local_date, local_time, date_time_iso,
                 source_country, source_city, source_category, captured_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (TicketmasterEvent ev : events) {
                ps.setString(1, ev.getCaptureBatchId());
                ps.setString(2, ev.getExternalEventId());
                ps.setString(3, ev.getName());
                ps.setString(4, ev.getClassificationName());
                ps.setString(5, ev.getSegment());
                ps.setString(6, ev.getGenre());
                ps.setString(7, ev.getCity());
                ps.setString(8, ev.getCountryCode());
                ps.setString(9, ev.getVenueName());
                ps.setString(10, ev.getEventUrl());
                ps.setString(11, ev.getLocalDate());
                ps.setString(12, ev.getLocalTime());
                ps.setString(13, ev.getDateTimeIso());
                ps.setString(14, ev.getSourceCountry());
                ps.setString(15, ev.getSourceCity());
                ps.setString(16, ev.getSourceCategory());
                ps.setString(17, ev.getCapturedAt());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();
        }
    }

    @Override
    public void finishRun(String batchId, int fetched, int inserted) throws Exception {
        String sql = """
            UPDATE ticketmaster_capture_run
               SET finished_at = ?, status = ?,
                   records_fetched = ?, records_inserted = ?
             WHERE capture_batch_id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, Instant.now().toString());
            ps.setString(2, "OK");
            ps.setInt(3, fetched);
            ps.setInt(4, inserted);
            ps.setString(5, batchId);
            ps.executeUpdate();
        }
    }

    @Override
    public void failRun(String batchId, String errorMessage) throws Exception {
        String sql = """
            UPDATE ticketmaster_capture_run
               SET finished_at = ?, status = ?, error_message = ?
             WHERE capture_batch_id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, Instant.now().toString());
            ps.setString(2, "ERROR");
            ps.setString(3, errorMessage);
            ps.setString(4, batchId);
            ps.executeUpdate();
        }
    }
}