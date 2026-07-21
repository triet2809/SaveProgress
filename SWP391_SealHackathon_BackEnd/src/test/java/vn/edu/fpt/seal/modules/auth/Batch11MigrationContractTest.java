package vn.edu.fpt.seal.modules.auth;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Batch11MigrationContractTest {
    @Test
    void securityVersionMigrationIsGuardedAndTransactional() throws Exception {
        String sql = Files.readString(Path.of("migration_batch11_auth_security_version.sql")).toLowerCase();
        assertTrue(sql.contains("begin;"));
        assertTrue(sql.contains("commit;"));
        assertTrue(sql.contains("add column if not exists security_version"));
        assertTrue(sql.contains("where security_version is null"));
        assertTrue(sql.contains("set not null"));
    }
}
