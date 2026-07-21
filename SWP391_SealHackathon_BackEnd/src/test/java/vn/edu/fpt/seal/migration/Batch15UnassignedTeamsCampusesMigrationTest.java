package vn.edu.fpt.seal.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Batch15UnassignedTeamsCampusesMigrationTest {
    private static final Path MIGRATION = Path.of("migration_batch15_unassigned_teams_campuses.sql");

    @Test
    void migrationIsTransactionalRepeatableAndRestoresAllCampuses() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertTrue(sql.startsWith("-- Restore"));
        assertTrue(sql.contains("BEGIN;"));
        assertTrue(sql.contains("COMMIT;"));
        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS event_id"));
        assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS idx_teams_event_id"));
        assertTrue(sql.contains("DROP NOT NULL"));
        for (String campus : new String[]{"Ha Noi", "Ho Chi Minh City", "Da Nang", "Can Tho", "Quy Nhon"}) {
            assertTrue(sql.contains(campus), "Missing campus: " + campus);
        }
    }

    @Test
    void migrationBackfillsEventAndRejectsUnprovableRows() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertTrue(sql.contains("SET event_id = track.event_id"));
        assertTrue(sql.contains("WHERE team.event_id IS NULL"));
        assertTrue(sql.contains("Batch 15 aborted: % team row(s) have no provable event"));
        assertTrue(sql.contains("Team track must belong to the team event"));
    }
}
