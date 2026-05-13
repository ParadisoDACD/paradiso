package org.ulpgc.paradiso.tfl.persistence;

import org.ulpgc.paradiso.tfl.model.TflCaptureRun;
import org.ulpgc.paradiso.tfl.model.TflJourney;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.List;

public class SqliteJourneyStore implements JourneyStore {

    private final String dbPath;

    public SqliteJourneyStore(String dbPath) {
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
                CREATE TABLE IF NOT EXISTS tfl_capture_run (
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
                CREATE TABLE IF NOT EXISTS tfl_journey_capture (
                    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                    capture_batch_id   TEXT    NOT NULL,
                    journey_hash       TEXT    NOT NULL,
                    origin_name        TEXT,
                    destination_name   TEXT,
                    start_date_time    TEXT,
                    arrival_date_time  TEXT,
                    duration_minutes   INTEGER,
                    number_of_legs     INTEGER,
                    first_leg_mode     TEXT,
                    capture_date       TEXT,
                    capture_time       TEXT,
                    source_origin      TEXT,
                    source_destination TEXT,
                    captured_at        TEXT    NOT NULL,
                    FOREIGN KEY (capture_batch_id)
                        REFERENCES tfl_capture_run(capture_batch_id)
                )
            """);
        }

        System.out.println("[TfL] Esquema SQLite listo: " + dbPath);
    }

    @Override
    public void startRun(TflCaptureRun run) throws Exception {
        String sql = """
            INSERT INTO tfl_capture_run
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
    public void saveAll(List<TflJourney> journeys) throws Exception {
        if (journeys.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO tfl_journey_capture
                (capture_batch_id, journey_hash, origin_name, destination_name,
                 start_date_time, arrival_date_time, duration_minutes,
                 number_of_legs, first_leg_mode,
                 capture_date, capture_time,
                 source_origin, source_destination, captured_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (TflJourney j : journeys) {
                ps.setString(1, j.getCaptureBatchId());
                ps.setString(2, j.getJourneyHash());
                ps.setString(3, j.getOriginName());
                ps.setString(4, j.getDestinationName());
                ps.setString(5, j.getStartDateTime());
                ps.setString(6, j.getArrivalDateTime());
                ps.setInt(7, j.getDurationMinutes());
                ps.setInt(8, j.getNumberOfLegs());
                ps.setString(9, j.getFirstLegMode());
                ps.setString(10, j.getCaptureDate());
                ps.setString(11, j.getCaptureTime());
                ps.setString(12, j.getSourceOrigin());
                ps.setString(13, j.getSourceDestination());
                ps.setString(14, j.getCapturedAt());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();
        }
    }

    @Override
    public void finishRun(String batchId, int fetched, int inserted) throws Exception {
        String sql = """
            UPDATE tfl_capture_run
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
            UPDATE tfl_capture_run
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