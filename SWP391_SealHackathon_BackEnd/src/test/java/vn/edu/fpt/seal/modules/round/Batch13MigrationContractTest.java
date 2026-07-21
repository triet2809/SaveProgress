package vn.edu.fpt.seal.modules.round;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Batch13MigrationContractTest {
    @Test
    void migrationIsTransactionalGuardedAndRejectsUnresolvedLegacyRows() throws Exception {
        String sql = Files.readString(Path.of("migration_batch13_promotion_provenance.sql")).toLowerCase();
        assertTrue(sql.contains("begin;"));
        assertTrue(sql.contains("set search_path to public"));
        assertTrue(sql.contains("source_result_version_id"));
        assertTrue(sql.contains("source_result_entry_id"));
        assertTrue(sql.contains("candidate_count = 1"));
        assertTrue(sql.contains("unresolved"));
        assertTrue(sql.contains("alter column source_result_version_id set not null"));
        assertTrue(sql.contains("fk_promotion_entry_version"));
        assertTrue(sql.contains("active published promoted entry"));
        assertTrue(sql.contains("immutable"));
        assertTrue(sql.contains("commit;"));
    }

    @Test
    void concurrentAdvancementRetainsDatabaseUniquenessGuard() throws Exception {
        String sql = Files.readString(Path.of("migration_batch12_logical_round_integrity.sql")).toLowerCase();
        assertTrue(sql.contains("uq_logical_round_promotions_target_team"));
        assertTrue(sql.contains("unique (target_logical_round_id, team_id)"));
    }
}
