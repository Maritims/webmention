package no.clueless.webmention.persistence.sqlite;

import no.clueless.webmention.persistence.Webmention;
import no.clueless.webmention.persistence.WebmentionRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    protected @NotNull Webmention mapFromResultSet(@NotNull ResultSet resultSet) throws SQLException {
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
    protected @NotNull PreparedStatement prepareCountStatement(@NotNull Connection connection) throws SQLException {
        return connection.prepareStatement("SELECT COUNT(*) FROM webmentions");
    }

    @Override
    protected @NotNull PreparedStatement prepareFindByIdStatement(@NotNull Connection connection, @NotNull Integer id) throws SQLException {
        var preparedStatement = connection.prepareStatement("SELECT id, isApproved, sourceUrl, targetUrl, mentionText, created, updated FROM webmentions WHERE id = ?");
        preparedStatement.setInt(1, id);
        return preparedStatement;
    }

    @Override
    protected @NotNull PreparedStatement prepareCreateStatement(@NotNull Connection connection, @NotNull Webmention webmention) throws SQLException {
        var preparedStatement = connection.prepareStatement("INSERT INTO webmentions(isApproved, sourceUrl, targetUrl, mentionText) VALUES(?, ?, ?, ?)");
        preparedStatement.setBoolean(1, webmention.isApproved());
        preparedStatement.setString(2, webmention.sourceUrl());
        preparedStatement.setString(3, webmention.targetUrl());
        preparedStatement.setString(4, webmention.mentionText());
        return preparedStatement;
    }

    @Override
    protected @NotNull PreparedStatement prepareUpdateStatement(@NotNull Connection connection, @NotNull Webmention webmention) throws SQLException {
        var preparedStatement = connection.prepareStatement("UPDATE webmentions SET isApproved = ?, mentionText = ?, updated = ? WHERE id = ?");
        preparedStatement.setBoolean(1, webmention.isApproved());
        preparedStatement.setString(2, webmention.mentionText());
        preparedStatement.setTimestamp(3, Timestamp.from(webmention.updated().atZone(ZoneId.systemDefault()).toInstant()));
        preparedStatement.setInt(4, webmention.id());
        return preparedStatement;
    }

    @Override
    public @NotNull Optional<Webmention> findWebmentionBySourceUrl(@NotNull String sourceUrl) {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var sql       = "SELECT id, isApproved, sourceUrl, targetURl, mentionText, created, updated FROM webmentions WHERE sourceUrl = ?";
            var statement = connection.prepareStatement(sql);
            statement.setString(1, sourceUrl);
            var resultSet = statement.executeQuery();
            return resultSet.next() ? Optional.of(mapFromResultSet(resultSet)) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public @NotNull List<Webmention> getWebmentionsByIsApproved(int pageNumber, int pageSize, @NotNull String orderByColumn, @NotNull String orderDirection, @Nullable Boolean isApproved) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must be greater than or equal to 0");
        }
        if (orderByColumn.isBlank()) {
            throw new IllegalArgumentException("orderByColumn cannot be blank");
        }
        if (!orderByColumn.equalsIgnoreCase("id") && !orderByColumn.equalsIgnoreCase("name") && !orderByColumn.equalsIgnoreCase("message") && !orderByColumn.equalsIgnoreCase("timestamp")) {
            throw new IllegalArgumentException("orderByColumn must be either id, name, message or timestamp");
        }
        if (!orderDirection.equalsIgnoreCase("asc") && !orderDirection.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("orderDirection must be either asc or desc");
        }

        var webmentions = new ArrayList<Webmention>();

        try (var connection = DriverManager.getConnection(connectionString)) {
            var sql       = isApproved == null ? String.format("SELECT * FROM webmentions ORDER BY %s %s LIMIT %d OFFSET %d", orderByColumn, orderDirection, pageSize, pageSize * pageNumber) : String.format("SELECT * FROM webmentions WHERE isApproved = ? ORDER BY %s %s LIMIT %d OFFSET %d", orderByColumn, orderDirection, pageSize, pageSize * pageNumber);
            var statement = connection.prepareStatement(sql);
            if (isApproved != null) {
                statement.setBoolean(1, isApproved);
            }
            var resultSet = statement.executeQuery();

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

    @Override
    public void updateApproval(@NotNull Webmention webmention, boolean isApproved) {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("UPDATE webmentions SET isApproved = ? WHERE id = ?");
            statement.setBoolean(1, isApproved);
            statement.setInt(2, webmention.id());
            var result = statement.executeUpdate();
            if (result == 0) {
                throw new RuntimeException("Failed to update webmention");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public void deleteWebmention(int id) {
        try(var connection = DriverManager.getConnection(connectionString)) {
            var statement = connection.prepareStatement("DELETE FROM webmentions WHERE id = ?");
            statement.setInt(1, id);
            var result = statement.executeUpdate();
            if(result == 0) {
                throw new RuntimeException("Failed to delete webmention");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }
}
