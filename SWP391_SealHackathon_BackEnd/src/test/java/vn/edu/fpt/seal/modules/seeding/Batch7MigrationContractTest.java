package vn.edu.fpt.seal.modules.seeding;

import org.junit.jupiter.api.Test;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class Batch7MigrationContractTest {
    @Test
    void migrationCreatesImmutableGuardedHistoryWithoutRankingBackfill() throws Exception {
        String sql = Files.readString(Path.of("migration_batch7_historical_finishes_seeding.sql"));

        assertTrue(sql.contains("SET search_path TO public"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.event_team_finishes"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.event_seed_assignments"));
        assertTrue(sql.contains("Historical finish snapshots are immutable"));
        assertTrue(sql.contains("version_status <> 'published'"));
        assertTrue(sql.contains("selected_sequence <> highest_sequence"));
        assertTrue(sql.contains("uq_event_seed_number_active"));
        assertFalse(sql.matches("(?is).*INSERT\\s+INTO\\s+public\\.event_team_finishes\\s+SELECT.*round_rankings.*"));
        assertFalse(sql.toLowerCase().contains("bracket"));
        assertFalse(sql.toLowerCase().contains("seed separation"));
    }

    @Test
    void setupIntegrationPreservesConfirmedSeedTracksAndWarnsWithoutFalseGuarantees() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/vn/edu/fpt/seal/modules/event/service/EventService.java"));
        assertTrue(source.contains("fixedByTeam.containsKey(team.getId())"));
        assertTrue(source.contains("Confirmed seed track must remain part of competition setup"));
        assertTrue(source.contains("seedReview.warning()"));
        assertFalse(source.toLowerCase().contains("guaranteed to avoid"));
    }
}
