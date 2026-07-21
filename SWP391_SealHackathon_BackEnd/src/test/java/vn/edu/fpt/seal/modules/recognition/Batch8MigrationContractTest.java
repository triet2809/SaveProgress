package vn.edu.fpt.seal.modules.recognition;

import org.junit.jupiter.api.Test;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class Batch8MigrationContractTest {
    @Test
    void migrationIsGuardedProfileScopedAndHasNoAutomaticAwardBackfill() throws Exception {
        String sql = Files.readString(Path.of("migration_batch8_team_recognitions.sql"));

        assertTrue(sql.contains("SET search_path TO public"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.team_recognitions"));
        assertTrue(sql.contains("REFERENCES public.team_profiles(id)"));
        assertTrue(sql.contains("qualification_count >= 0"));
        assertTrue(sql.contains("CREATE UNIQUE INDEX IF NOT EXISTS uq_team_recognition_active"));
        assertTrue(sql.contains("WHERE active = true"));
        assertTrue(sql.contains("TEAM_RECOGNITION_AWARDED"));
        assertTrue(sql.contains("TEAM_RECOGNITION_REVOKED"));
        assertFalse(sql.matches("(?is).*INSERT\\s+INTO\\s+public\\.team_recognitions\\s+SELECT.*"));
    }

    @Test
    void manifestDocumentsBatch8AfterImmutableFinishes() throws Exception {
        String manifest = Files.readString(Path.of("MIGRATIONS.md"));
        int batch7 = manifest.indexOf("7. `migration_batch7_historical_finishes_seeding.sql`");
        int batch8 = manifest.indexOf("8. `migration_batch8_team_recognitions.sql`");
        assertTrue(batch7 >= 0 && batch8 > batch7);
    }

    @Test
    void sharedPublicDtoContainsNoPrivateRosterOrCorrectionFields() throws Exception {
        String dto = Files.readString(Path.of(
                "src/main/java/vn/edu/fpt/seal/modules/recognition/dto/RecognitionDtos.java"));
        String summary = dto.substring(dto.indexOf("public record Summary"),
                dto.indexOf("public record QualifyingSeason"));
        assertFalse(summary.contains("email"));
        assertFalse(summary.contains("invite"));
        assertFalse(summary.contains("member"));
        assertFalse(summary.contains("revokeReason"));
        assertFalse(summary.contains("token"));
    }
}
