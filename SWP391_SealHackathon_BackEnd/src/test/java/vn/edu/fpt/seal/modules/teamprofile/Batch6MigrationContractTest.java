package vn.edu.fpt.seal.modules.teamprofile;

import org.junit.jupiter.api.Test;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class Batch6MigrationContractTest {
    @Test
    void migrationPreservesLegacyRowsAndAvoidsNameBasedMerging() throws Exception {
        String sql = Files.readString(Path.of("migration_batch6_team_profiles.sql"));

        assertTrue(sql.contains("WHERE t.team_profile_id IS NULL"));
        assertTrue(sql.contains("WHERE id = legacy.id AND team_profile_id IS NULL"));
        assertFalse(sql.matches("(?is).*UPDATE\\s+public\\.team_members.*"));
        assertFalse(sql.matches("(?is).*DELETE\\s+FROM\\s+public\\.teams.*"));
        assertFalse(sql.matches("(?is).*GROUP\\s+BY\\s+t\\.name.*"));
        assertTrue(sql.contains("source_team_id uuid REFERENCES public.teams(id)"));
    }
}
