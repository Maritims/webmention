package no.clueless.webmention.persistence.sqlite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SqliteWebmentionRepositoryTest {

    @Test
    void verifyDatabasePath_shouldSucceedWhenPathExists_andFileExists(@TempDir Path tempDir) throws IOException {
        var newFile = tempDir.resolve("foo.db");
        assertTrue(newFile.toFile().createNewFile());
        assertTrue(SqliteDatabasePathVerifier.verifyDatabasePath(newFile));
    }

    @Test
    void verifyDatabasePath_shouldSucceedWhenPathExists_andFileDoesNotExist() {
        var newFile = Path.of("/tmp/bar.db");
        assertFalse(newFile.toFile().exists());
        assertTrue(SqliteDatabasePathVerifier.verifyDatabasePath(newFile));
    }

    @Test
    void verifyDatabasePath_shouldFailWhenPathExists_andEndsWithDb_andIsADirectory(@TempDir Path tempDir) {
        var newFile = tempDir.resolve("baz.db");
        assertTrue(newFile.toFile().mkdir());
        var result = assertThrows(IllegalArgumentException.class, () -> SqliteDatabasePathVerifier.verifyDatabasePath(newFile));
        assertEquals("databasePath must be a file, but was a directory: " + newFile, result.getMessage());
    }

    @Test
    void verifyDatabasePath_shouldFailWhenPathDoesNotExist() {
        var result = assertThrows(RuntimeException.class, () -> SqliteDatabasePathVerifier.verifyDatabasePath(Path.of("/tmp/bar/baz.db")));
        assertEquals("Database directory does not exist: /tmp/bar", result.getMessage());
    }
}