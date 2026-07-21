package vn.edu.fpt.seal.modules.round;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Batch12MigrationContractTest {
    @Test
    void migrationIsTransactionalRepeatableAndEnforcesEventLevelIdentity() throws Exception {
        String sql = Files.readString(Path.of("migration_batch12_logical_round_integrity.sql")).toLowerCase();
        assertTrue(sql.contains("begin;"));
        assertTrue(sql.contains("set search_path to public"));
        assertTrue(sql.contains("unique (event_id, sequence_number)"));
        assertTrue(sql.contains("lower(name)"));
        assertTrue(sql.contains("create table if not exists public.logical_round_promotions"));
        assertTrue(sql.contains("unique (target_logical_round_id, team_id)"));
        assertTrue(sql.contains("create or replace function public.validate_logical_round_promotion"));
        assertTrue(sql.contains("commit;"));
    }
}
