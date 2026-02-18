package no.clueless.webmention.persistence.sqlite;

import no.clueless.webmention.persistence.Client;
import no.clueless.webmention.persistence.ClientRepository;

import java.sql.*;
import java.time.ZoneId;
import java.util.Optional;

public class SqliteClientRepository extends SqliteBaseRepository<Client> implements ClientRepository {
    public SqliteClientRepository(String connectionString) {
        super(connectionString, """
                CREATE TABLE IF NOT EXISTS clients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                clientId TEXT NOT NULL UNIQUE,
                clientSecret TEXT NOT NULL,
                isEnabled BOOLEAN NOT NULL DEFAULT TRUE,
                created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                """);
    }

    @Override
    protected Client mapFromResultSet(ResultSet resultSet) throws SQLException {
        return null;
    }

    @Override
    protected PreparedStatement prepareCountStatement(Connection connection) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        return connection.prepareStatement("SELECT COUNT(*) FROM clients");
    }

    @Override
    protected PreparedStatement prepareFindByIdStatement(Connection connection, Integer id) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id cannot be null or less than 1");
        }

        var preparedStatement = connection.prepareStatement("SELECT id, clientSecret, isEnabled, updated FROM clients WHERE id = ?");
        preparedStatement.setInt(1, id);
        return preparedStatement;
    }

    @Override
    protected PreparedStatement prepareCreateStatement(Connection connection, Client entity) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }

        var preparedStatement = connection.prepareStatement("INSERT INTO clients (clientId, clientSecret, isEnabled, updated) VALUES (?, ?, ?)");
        preparedStatement.setString(1, entity.clientId());
        preparedStatement.setString(2, entity.clientSecret());
        return preparedStatement;
    }

    @Override
    protected PreparedStatement prepareUpdateStatement(Connection connection, Client client) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        if (client == null) {
            throw new IllegalArgumentException("client cannot be null");
        }

        var preparedStatement = connection.prepareStatement("UPDATE clients SET clientSecret = ?, isEnabled = ?, updated = ? WHERE id = ?");
        preparedStatement.setString(1, client.clientSecret());
        preparedStatement.setBoolean(2, client.isEnabled());
        preparedStatement.setTimestamp(3, Timestamp.from(client.updated().atZone(ZoneId.systemDefault()).toInstant()));
        preparedStatement.setInt(4, client.id());
        return preparedStatement;
    }

    @Override
    public Optional<Client> findByClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("SELECT * FROM clients WHERE clientId = ?");
            statement.setString(1, clientId);
            var resultSet = statement.executeQuery();
            return resultSet.next() ? Optional.of(mapFromResultSet(resultSet)) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }
}
