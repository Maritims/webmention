package no.clueless.webmention.persistence.sqlite;

import no.clueless.webmention.persistence.Webmention;
import no.clueless.webmention.persistence.WebmentionRepository;

import java.sql.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class SqliteWebmentionRepository extends SqliteBaseRepository<Webmention> implements WebmentionRepository {
    public SqliteWebmentionRepository(String connectionString) {
        super(connectionString, """
                CREATE TABLE IF NOT EXISTS webmentions (
                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                     isApproved BOOLEAN NOT NULL DEFAULT FALSE,
                     sourceUrl TEXT NOT NULL,
                     targetUrl TEXT NOT NULL,
                     mentionText TEXT,
                     created DATETIME DEFAULT CURRENT_TIMESTAMP,
                     updated DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    @Override
    protected Webmention mapFromResultSet(ResultSet resultSet) throws SQLException {
        var id          = resultSet.getInt("id");
        var isApproved  = resultSet.getBoolean("isApproved");
        var sourceUrl   = resultSet.getString("sourceUrl");
        var targetUrl   = resultSet.getString("targetUrl");
        var mentionText = resultSet.getString("mentionText");
        var created     = resultSet.getTimestamp("created").toLocalDateTime();
        var updated     = resultSet.getTimestamp("updated").toLocalDateTime();
        return new Webmention(id, isApproved, sourceUrl, targetUrl, mentionText, created, updated);
    }

    @Override
    protected PreparedStatement prepareCountStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("SELECT COUNT(*) FROM webmentions");
    }

    @Override
    protected PreparedStatement prepareFindByIdStatement(Connection connection, Integer id) throws SQLException {
        var preparedStatement = connection.prepareStatement("SELECT id, isApproved, sourceUrl, targetUrl, mentionText, created, updated FROM webmentions WHERE id = ?");
        preparedStatement.setInt(1, id);
        return preparedStatement;
    }

    @Override
    protected PreparedStatement prepareCreateStatement(Connection connection, Webmention webmention) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        if (webmention == null) {
            throw new IllegalArgumentException("webmention cannot be null");
        }

        var preparedStatement = connection.prepareStatement("INSERT INTO webmentions(isApproved, sourceUrl, targetUrl, mentionText) VALUES(?, ?, ?, ?)");
        preparedStatement.setBoolean(1, webmention.isApproved());
        preparedStatement.setString(2, webmention.sourceUrl());
        preparedStatement.setString(3, webmention.targetUrl());
        preparedStatement.setString(4, webmention.mentionText());
        return preparedStatement;
    }

    @Override
    protected PreparedStatement prepareUpdateStatement(Connection connection, Webmention webmention) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        if (webmention == null) {
            throw new IllegalArgumentException("webmention cannot be null");
        }

        var preparedStatement = connection.prepareStatement("UPDATE webmentions SET isApproved = ?, mentionText = ?, updated = ? WHERE id = ?");
        preparedStatement.setBoolean(1, webmention.isApproved());
        preparedStatement.setString(2, webmention.mentionText());
        preparedStatement.setTimestamp(3, Timestamp.from(webmention.updated().atZone(ZoneId.systemDefault()).toInstant()));
        preparedStatement.setInt(4, webmention.id());
        return preparedStatement;
    }

    @Override
    public Webmention getWebmentionBySourceUrl(String sourceUrl) {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var sql       = "SELECT id, isApproved, sourceUrl, targetURl, mentionText, created, updated FROM webmentions WHERE sourceUrl = ?";
            var statement = connection.prepareStatement(sql);
            statement.setString(1, sourceUrl);
            var resultSet = statement.executeQuery();
            return resultSet.next() ? mapFromResultSet(resultSet) : null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    public long getApprovedCount() {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery("SELECT COUNT(*) FROM webmentions WHERE isApproved = true");
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    public List<Webmention> getApprovedWebmentions(int pageNumber, int pageSize, String orderByColumn, String orderDirection) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must be greater than or equal to 0");
        }
        if (orderByColumn == null || orderByColumn.isBlank()) {
            throw new IllegalArgumentException("orderByColumn cannot be null or blank");
        }
        if (!orderByColumn.equalsIgnoreCase("id") && !orderByColumn.equalsIgnoreCase("name") && !orderByColumn.equalsIgnoreCase("message") && !orderByColumn.equalsIgnoreCase("timestamp")) {
            throw new IllegalArgumentException("orderByColumn must be either id, name, message or timestamp");
        }
        if (orderDirection == null || !orderDirection.equalsIgnoreCase("asc") && !orderDirection.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("orderDirection must be either asc or desc");
        }

        var webmentions = new ArrayList<Webmention>();

        try (var connection = DriverManager.getConnection(connectionString)) {
            var sql       = String.format("SELECT id, isApproved, sourceUrl, targetUrl, mentionText, created, updated FROM webmentions WHERE isApproved = true ORDER BY %s %s LIMIT %d OFFSET %d", orderByColumn, orderDirection, pageSize, pageSize * pageNumber);
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                webmentions.add(Webmention.existingWebmention(
                        resultSet.getInt("id"),
                        resultSet.getBoolean("isApproved"),
                        resultSet.getString("sourceUrl"),
                        resultSet.getString("targetUrl"),
                        resultSet.getString("mentionText"),
                        resultSet.getTimestamp("created").toLocalDateTime(),
                        resultSet.getTimestamp("updated").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }

        return webmentions;
    }
}
