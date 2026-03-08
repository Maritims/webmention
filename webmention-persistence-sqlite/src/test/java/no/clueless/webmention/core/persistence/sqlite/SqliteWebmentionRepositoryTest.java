package no.clueless.webmention.core.persistence.sqlite;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SqliteWebmentionRepositoryTest {

    @Test
    void verifyDatabasePath_shouldSucceedWhenPathExists_andFileExists(@TempDir Path tempDir) throws IOException {
        var newFile = tempDir.resolve("foo.db");
        Assertions.assertTrue(newFile.toFile().createNewFile());
        assertTrue(no.clueless.sqlite.core.SqliteDatabasePathVerifier.verifyDatabasePath(newFile));
    }

    @Test
    void verifyDatabasePath_shouldSucceedWhenPathExists_andFileDoesNotExist() {
        var newFile = Path.of("/tmp/bar.db");
        Assertions.assertFalse(newFile.toFile().exists());
        assertTrue(no.clueless.sqlite.core.SqliteDatabasePathVerifier.verifyDatabasePath(newFile));
    }

    @Test
    void verifyDatabasePath_shouldFailWhenPathExists_andEndsWithDb_andIsADirectory(@TempDir Path tempDir) {
        var newFile = tempDir.resolve("baz.db");
        Assertions.assertTrue(newFile.toFile().mkdir());
        var result = Assertions.assertThrows(IllegalArgumentException.class, () -> no.clueless.sqlite.core.SqliteDatabasePathVerifier.verifyDatabasePath(newFile));
        Assertions.assertEquals("databasePath must be a file, but was a directory: " + newFile, result.getMessage());
    }

    @Test
    void verifyDatabasePath_shouldFailWhenPathDoesNotExist() {
        var result = Assertions.assertThrows(RuntimeException.class, () -> no.clueless.sqlite.core.SqliteDatabasePathVerifier.verifyDatabasePath(Path.of("/tmp/bar/baz.db")));
        Assertions.assertEquals("Database directory does not exist: /tmp/bar", result.getMessage());
    }
}