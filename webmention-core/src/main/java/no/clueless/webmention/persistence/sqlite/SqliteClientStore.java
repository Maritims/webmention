package no.clueless.webmention.persistence.sqlite;

import no.clueless.oauth.ClientStore;
import no.clueless.oauth.OAuthClient;
import no.clueless.oauth.Scope;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static no.clueless.webmention.persistence.sqlite.SqliteDatabasePathVerifier.extractAbsoluteDatabasePath;
import static no.clueless.webmention.persistence.sqlite.SqliteDatabasePathVerifier.verifyDatabasePath;

public class SqliteClientStore implements ClientStore {
    private final String connectionString;
    private final String createTableQuery;

    public SqliteClientStore(String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalArgumentException("connectionString cannot be null or blank");
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
    public OAuthClient getClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
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
    public void disableClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
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
    public void enableClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
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
    public void deleteClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
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
    public void registerClient(String clientId, String clientSecret, Set<Scope> scopes) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be null or blank");
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("INSERT INTO clients (clientId, clientSecret, isEnabled, scopes) VALUES(?, ?, ?, ?);");
            statement.setString(1, clientId);
            statement.setString(2, BCrypt.hashpw(clientSecret, BCrypt.gensalt()));
            statement.setBoolean(3, true);
            statement.setString(4, String.join(",", scopes.stream().map(Scope::getLabel).toList()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public boolean shouldSeedInitialClient() {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("SELECT COUNT(*) FROM clients WHERE scopes LIKE ?");
            var pattern   = "%" + Scope.WEBMENTIONS_MANAGE.getLabel() + "%";
            statement.setString(1, pattern);
            var resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1) == 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }
}
