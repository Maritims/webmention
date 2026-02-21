package no.clueless.webmention.persistence.sqlite;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SqliteDatabasePathVerifier {
    /**
     * Extracts the last part of the connection string, which is the path to the database file.
     *
     * @param connectionString The connection string.
     * @return The path to the database file.
     */
    public static Path extractAbsoluteDatabasePath(String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalArgumentException("connectionString cannot be null or empty");
        }
        if (!connectionString.startsWith("jdbc:sqlite:")) {
            throw new IllegalArgumentException("connectionString must start with jdbc:sqlite:, but was " + connectionString);
        }

        var rawPath     = connectionString.replaceFirst("^(jdbc:)?sqlite:", "");
        var decodedPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        return Paths.get(decodedPath).normalize();
    }

    /**
     * Verify that the database directory path exists and is writable.
     *
     * @param databaseFilePath The path to the database file. Cannot be null nor a directory.
     * @return True if the database directory path exists and is writable.
     * @throws IllegalArgumentException If the database file path is null, does not end with .db or is a directory.
     */
    public static boolean verifyDatabasePath(Path databaseFilePath) {
        if (databaseFilePath == null) {
            throw new IllegalArgumentException("databasePath cannot be null");
        }
        if (!databaseFilePath.toString().endsWith(".db")) {
            throw new IllegalArgumentException("databasePath must end with .db, but was " + databaseFilePath);
        }
        if (Files.isDirectory(databaseFilePath)) {
            throw new IllegalArgumentException("databasePath must be a file, but was a directory: " + databaseFilePath);
        }

        var databaseDirPath = databaseFilePath.toAbsolutePath().getParent();
        if (!Files.exists(databaseDirPath)) {
            throw new IllegalArgumentException("Database directory does not exist: " + databaseDirPath);
        }
        if (!databaseDirPath.toFile().canWrite()) {
            throw new IllegalArgumentException("Cannot write to database directory " + databaseDirPath);
        }

        return true;
    }
}
