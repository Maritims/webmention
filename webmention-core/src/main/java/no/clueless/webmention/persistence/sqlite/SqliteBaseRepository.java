package no.clueless.webmention.persistence.sqlite;

import no.clueless.webmention.persistence.Entity;
import no.clueless.webmention.persistence.Repository;
import org.slf4j.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

abstract public class SqliteBaseRepository<TEntity extends Entity<Integer>> implements Repository<TEntity, Integer> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SqliteBaseRepository.class);
    protected final      String connectionString;
    private final        String createTableQuery;

    protected SqliteBaseRepository(String connectionString, String createTableQuery) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalArgumentException("connectionString cannot be null or empty");
        }
        if (createTableQuery == null || createTableQuery.isBlank()) {
            throw new IllegalArgumentException("createTableQuery cannot be null or empty");
        }

        this.connectionString = connectionString;
        this.createTableQuery = createTableQuery;
    }

    public final SqliteBaseRepository<TEntity> initialize() {
        if (!verifyDatabasePath(extractAbsoluteDatabasePath(connectionString))) {
            throw new RuntimeException("Database path did not pass verification. The connection string must be of the form jdbc:sqlite:path/to/database.db, but was " + connectionString);
        }

        try (var connection = DriverManager.getConnection(connectionString);
             var statement = connection.createStatement()) {
            statement.execute(createTableQuery);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to setup database", e);
        }

        return this;
    }

    protected abstract TEntity mapFromResultSet(ResultSet resultSet) throws SQLException;

    protected abstract PreparedStatement prepareCountStatement(Connection connection) throws SQLException;

    protected abstract PreparedStatement prepareFindByIdStatement(Connection connection, Integer id) throws SQLException;

    protected abstract PreparedStatement prepareCreateStatement(Connection connection, TEntity entity) throws SQLException;

    protected abstract PreparedStatement prepareUpdateStatement(Connection connection, TEntity entity) throws SQLException;

    @Override
    public Integer count() {
        try(var connection = DriverManager.getConnection(connectionString)) {
            var statement = prepareCountStatement(connection);
            var resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public final TEntity getById(Integer id) {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = prepareFindByIdStatement(connection, id);
            var resultSet = statement.executeQuery();
            return resultSet.next() ? mapFromResultSet(resultSet) : null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public final TEntity create(TEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = prepareCreateStatement(connection, entity);
            statement.executeUpdate();
            var id = statement.getGeneratedKeys().getInt(1);
            return getById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public final TEntity update(TEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = prepareUpdateStatement(connection, entity);
            statement.executeUpdate();

            return getById(entity.id());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public TEntity upsert(
            TEntity entityWithChanges,
            FindExistingEntity<TEntity, Integer> findExistingEntity,
            CreateNewEntity<TEntity, Integer> createNewEntity,
            UpdateExistingEntity<TEntity, Integer> updateExistingEntity
    ) {
        if (entityWithChanges == null) {
            throw new IllegalArgumentException("entityWithChanges cannot be null");
        }
        if (findExistingEntity == null) {
            throw new IllegalArgumentException("findExistingEntity cannot be null");
        }
        if (createNewEntity == null) {
            throw new IllegalArgumentException("createNewEntity cannot be null");
        }
        if (updateExistingEntity == null) {
            throw new IllegalArgumentException("updateExistingEntity cannot be null");
        }

        var existingEntity = findExistingEntity.findBySomethingFromEntity(entityWithChanges);
        if (existingEntity == null) {
            log.debug("Creating entity of type {}: {}", entityWithChanges.getClass(), entityWithChanges);
            var newEntity = createNewEntity.createNewWithSomethingFromEntity(entityWithChanges);
            return create(newEntity);
        } else {
            existingEntity = updateExistingEntity.updateWithSomethingFromEntity(existingEntity, entityWithChanges);
            return update(existingEntity);
        }
    }

    /**
     * Extracts the last part of the connection string, which is the path to the database file.
     *
     * @param connectionString The connection string.
     * @return The path to the database file.
     */
    static Path extractAbsoluteDatabasePath(String connectionString) {
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
    static boolean verifyDatabasePath(Path databaseFilePath) {
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
