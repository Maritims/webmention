package no.clueless.webmention.persistence.sqlite;

import no.clueless.oauth.ClientStore;
import no.clueless.oauth.OAuthClient;

import java.sql.*;

public class SqlClientStore implements ClientStore {
    private final String connectionString;
    private final String createTableQuery;

    public SqlClientStore(String connectionString) {
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
                created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                """;
    }

    public final ClientStore initialize() {
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
                var isEnabled    = resultSet.getBoolean("isEnabled");
                return new OAuthClient(clientId, clientSecret, null, isEnabled);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public void registerClient(String clientId, String clientSecret, String... scopes) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be null or blank");
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("INSERT INTO clients (clientId, clientSecret, isEnabled) VALUES(?, ?, ?);");
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }
}
