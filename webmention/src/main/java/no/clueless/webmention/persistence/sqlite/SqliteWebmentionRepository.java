package no.clueless.webmention.persistence.sqlite;

import no.clueless.webmention.persistence.Webmention;
import no.clueless.webmention.persistence.WebmentionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class SqliteWebmentionRepository implements WebmentionRepository<Integer> {
    private static final Logger log = LoggerFactory.getLogger(SqliteWebmentionRepository.class);
    private final String connectionString;

    public SqliteWebmentionRepository(String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalArgumentException("connectionString cannot be null or empty");
        }
        this.connectionString = connectionString;
    }

    @Override
    public WebmentionRepository<Integer> initialize() {
        try (var connection = DriverManager.getConnection(connectionString);
             var statement = connection.createStatement()) {
            statement.execute("""
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
        } catch (SQLException e) {
            throw new RuntimeException("Failed to setup database", e);
        }

        return this;
    }

    Webmention mapFromResultSet(ResultSet resultSet) throws SQLException {
        var id          = resultSet.getInt("id");
        var isApproved  = resultSet.getBoolean("isApproved");
        var sourceUrl   = resultSet.getString("sourceUrl");
        var targetUrl   = resultSet.getString("targetUrl");
        var mentionText = resultSet.getString("mentionText");
        var created     = resultSet.getTimestamp("created").toLocalDateTime();
        var updated     = resultSet.getTimestamp("updated").toLocalDateTime();
        return new Webmention(id, isApproved, sourceUrl, targetUrl, mentionText, created, updated);
    }

    public Webmention getWebmentionById(Integer id) {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var sql       = "SELECT id, isApproved, sourceUrl, targetURl, mentionText, created, updated FROM webmentions WHERE id = ?";
            var statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            var resultSet = statement.executeQuery();
            return resultSet.next() ? mapFromResultSet(resultSet) : null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
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

    public Webmention createWebmention(Webmention webmention) {
        if (webmention == null) {
            throw new IllegalArgumentException("webmention cannot be null");
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var sql       = "INSERT INTO webmentions(isApproved, sourceUrl, targetUrl, mentionText) VALUES(?, ?, ?, ?)";
            var statement = connection.prepareStatement(sql);
            statement.setBoolean(1, webmention.isApproved());
            statement.setString(2, webmention.sourceUrl());
            statement.setString(3, webmention.targetUrl());
            statement.setString(4, webmention.mentionText());
            statement.executeUpdate();

            var id = statement.getGeneratedKeys().getInt(1);
            return getWebmentionById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public Webmention updateWebmention(Webmention webmention) {
        if (webmention == null) {
            throw new IllegalArgumentException("webmention cannot be null");
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var sql       = "UPDATE webmentions SET isApproved = ?, mentionText = ?, updated = ? WHERE id = ?";
            var statement = connection.prepareStatement(sql);
            statement.setBoolean(1, webmention.isApproved());
            statement.setString(2, webmention.mentionText());
            statement.setTimestamp(3, Timestamp.from(webmention.updated().atZone(ZoneId.systemDefault()).toInstant()));
            statement.setInt(4, webmention.id());
            statement.executeUpdate();

            return getWebmentionById(webmention.id());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public Webmention upsertWebmention(Webmention webmention) {
        var existingWebmention = getWebmentionBySourceUrl(webmention.sourceUrl());
        if (existingWebmention == null) {
            log.debug("Creating webmention: {} -> {}", webmention.sourceUrl(), webmention.targetUrl());
            var newWebmention = Webmention.newWebmention(webmention.sourceUrl(), webmention.targetUrl(), webmention.mentionText());
            return createWebmention(newWebmention);
        } else {
            log.info("Updating webmention with ID {}: {} -> {}", existingWebmention.id(), existingWebmention.sourceUrl(), existingWebmention.targetUrl());
            existingWebmention = existingWebmention.update(existingWebmention.isApproved(), webmention.mentionText(), LocalDateTime.now());
            return updateWebmention(existingWebmention);
        }
    }
}
