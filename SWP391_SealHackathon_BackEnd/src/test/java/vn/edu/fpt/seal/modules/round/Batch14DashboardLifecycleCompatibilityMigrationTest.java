package vn.edu.fpt.seal.modules.round;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Batch14DashboardLifecycleCompatibilityMigrationTest {
    @Test
    void migrationTransactionallyNormalizesLegacyPublishedRoundState() throws Exception {
        String sql = Files.readString(Path.of("migration_batch14_round_lifecycle_compatibility.sql"))
                .toLowerCase();

        assertTrue(sql.contains("begin;"));
        assertTrue(sql.contains("set search_path to public"));
        assertTrue(sql.contains("update public.round_definitions"));
        assertTrue(sql.contains("update public.rounds"));
        assertTrue(sql.contains("where lifecycle_state = 'published'"));
        assertTrue(sql.contains("set lifecycle_state = 'appeal_window_open'"));
        assertTrue(sql.contains("commit;"));
    }
}
