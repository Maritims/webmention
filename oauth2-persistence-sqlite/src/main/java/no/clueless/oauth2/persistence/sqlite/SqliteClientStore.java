package no.clueless.oauth2.persistence.sqlite;

import no.clueless.oauth2.core.ClientStore;
import no.clueless.oauth2.core.OAuthClient;
import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static no.clueless.sqlite.core.SqliteDatabasePathVerifier.extractAbsoluteDatabasePath;
import static no.clueless.sqlite.core.SqliteDatabasePathVerifier.verifyDatabasePath;

public class SqliteClientStore implements ClientStore {
    @NotNull
    private final String connectionString;
    @NotNull
    private final String createTableQuery;

    public SqliteClientStore(@NotNull String connectionString) {
        if (connectionString.isBlank()) {
            throw new IllegalArgumentException("connectionString cannot be blank");
        }

        this.connectionString = connectionString;
        this.createTableQuery = """
                CREATE TABLE IF NOT EXISTS clients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                clientId TEXT NOT NULL UNIQUE,
                clientSecret TEXT NOT NULL,
                isEnabled BOOLEAN NOT NULL DEFAULT TRUE,
                scopes TEXT NOT NULL,
                created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
    }

    @NotNull
    public final ClientStore initialize() {
        if (!verifyDatabasePath(extractAbsoluteDatabasePath(connectionString))) {
            throw new RuntimeException("Database path did not pass verification. The connection string must be of the form jdbc:sqlite:path/to/database.db, but was " + connectionString);
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement(createTableQuery);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
        return this;
    }

    @Override
    public OAuthClient getClient(@NotNull String clientId) {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("SELECT * FROM clients WHERE clientId = ?");
            statement.setString(1, clientId);
            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                var clientSecret = resultSet.getString("clientSecret");
                var scopes       = Arrays.stream(resultSet.getString("scopes").split(",")).collect(Collectors.toSet());
                var isEnabled    = resultSet.getBoolean("isEnabled");
                return new OAuthClient(clientId, clientSecret, scopes, isEnabled);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public void disableClient(@NotNull String clientId) {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        try(var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("UPDATE clients SET isEnabled = false WHERE clientId = ?");
            statement.setString(1, clientId);
            var result = statement.executeUpdate();
            if(result == 0) {
                throw new RuntimeException("Failed to update client");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public void enableClient(@NotNull String clientId) {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        try(var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("UPDATE clients SET isEnabled = true WHERE clientId = ?");
            statement.setString(1, clientId);
            var result = statement.executeUpdate();
            if(result == 0) {
                throw new RuntimeException("Failed to update client");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public void deleteClient(@NotNull String clientId) {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        try(var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("DELETE FROM clients WHERE clientId = ?");
            statement.setString(1, clientId);
            var result = statement.executeUpdate();
            if(result == 0) {
                throw new RuntimeException("Failed to delete client");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public void registerClient(@NotNull String clientId, @NotNull String clientSecret, @NotNull Set<String> scopes) {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        if (clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be blank");
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("INSERT INTO clients (clientId, clientSecret, isEnabled, scopes) VALUES(?, ?, ?, ?);");
            statement.setString(1, clientId);
            statement.setString(2, BCrypt.hashpw(clientSecret, BCrypt.gensalt()));
            statement.setBoolean(3, true);
            statement.setString(4, String.join(",", scopes));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public boolean shouldSeedInitialClient() {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("SELECT COUNT(*) FROM clients WHERE scopes LIKE ?");
            var pattern   = "%webmentions:manage%";
            statement.setString(1, pattern);
            var resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1) == 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public @NotNull Set<OAuthClient> getClients(int page, int size, @NotNull String orderByColumn, boolean ascending) {
        String direction = ascending ? "ASC" : "DESC";
        String query     = "SELECT * FROM clients ORDER BY " + orderByColumn + " " + direction + " LIMIT ? OFFSET ?";

        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement(query);
            statement.setInt(1, size);
            statement.setInt(2, page * size);
            var resultSet = statement.executeQuery();

            Set<OAuthClient> clients = new java.util.HashSet<>();
            while (resultSet.next()) {
                var clientId     = resultSet.getString("clientId");
                var clientSecret = resultSet.getString("clientSecret");
                var scopes       = Arrays.stream(resultSet.getString("scopes").split(",")).collect(Collectors.toSet());
                var isEnabled    = resultSet.getBoolean("isEnabled");
                clients.add(new OAuthClient(clientId, clientSecret, scopes, isEnabled));
            }
            return clients;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }
}
