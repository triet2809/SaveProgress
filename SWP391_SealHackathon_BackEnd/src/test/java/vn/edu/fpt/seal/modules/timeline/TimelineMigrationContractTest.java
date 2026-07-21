package vn.edu.fpt.seal.modules.timeline;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class TimelineMigrationContractTest {
    @Test
    void migrationIsPortableRepeatableAndAddsVisibilityAndIdempotency() throws Exception {
        String sql = Files.readString(Path.of("migration_batch9_timeline_foundation.sql")).toLowerCase();
        assertTrue(sql.startsWith("-- batch 9"));
        assertTrue(sql.contains("set search_path to public"));
        assertTrue(sql.contains("alter table public.team_timeline_events"));
        assertTrue(sql.contains("add column if not exists visibility_scope"));
        assertTrue(sql.contains("add column if not exists idempotency_key"));
        assertTrue(sql.contains("add column if not exists metadata jsonb"));
        assertTrue(sql.contains("team_timeline_events_track_id_fkey"));
        assertTrue(sql.contains("occurred_at desc, id desc"));
        assertTrue(sql.contains("create unique index if not exists"));
    }
}
