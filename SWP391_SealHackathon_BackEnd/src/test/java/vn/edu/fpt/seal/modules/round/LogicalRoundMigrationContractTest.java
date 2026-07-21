package vn.edu.fpt.seal.modules.round;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LogicalRoundMigrationContractTest {
    @Test
    void migrationBackfillsExistingRoundsWithoutChangingOperationalIds() throws Exception {
        String sql = Files.readString(Path.of("migration_batch10_logical_rounds.sql")).toLowerCase();
        assertTrue(sql.contains("create table if not exists public.round_definitions"));
        assertTrue(sql.contains("add column if not exists logical_round_id"));
        assertTrue(sql.contains("where r.logical_round_id is null"));
        assertTrue(sql.contains("unique (logical_round_id, track_id)"));
        assertTrue(sql.contains("set not null"));
        assertTrue(sql.contains("set search_path to public"));
    }
}
