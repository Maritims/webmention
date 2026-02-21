package no.clueless.webmention.persistence.sqlite;

import no.clueless.webmention.persistence.Entity;
import no.clueless.webmention.persistence.Repository;
import org.slf4j.Logger;

import java.sql.*;
import java.util.Optional;

import static no.clueless.webmention.persistence.sqlite.SqliteDatabasePathVerifier.extractAbsoluteDatabasePath;
import static no.clueless.webmention.persistence.sqlite.SqliteDatabasePathVerifier.verifyDatabasePath;

/**
 * A base repository for using a Sqlite database.
 *
 * @param <TEntity>
 */
abstract public class SqliteBaseRepository<TEntity extends Entity<Integer>> implements Repository<TEntity, Integer> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SqliteBaseRepository.class);
    protected final      String connectionString;
    private final        String createTableQuery;

    /**
     * Constructor.
     *
     * @param connectionString the JDBC connection string, e.g. "jdbc:sqlite:path/to/database.db"
     * @param createTableQuery the SQL query to create the table if it does not exist
     * @throws IllegalArgumentException if connectionString or createTableQuery is null or empty
     */
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

    /**
     * Initializes the database by creating the table if it does not exist.
     *
     * @return this repository
     */
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

    /**
     * Maps a ResultSet to an entity.
     *
     * @param resultSet the ResultSet to map from
     * @return the mapped entity
     * @throws SQLException if mapping fails
     */
    protected abstract TEntity mapFromResultSet(ResultSet resultSet) throws SQLException;

    /**
     * Prepares a statement for counting entities.
     *
     * @param connection the connection
     * @return the prepared statement
     * @throws SQLException if preparing fails
     */
    protected abstract PreparedStatement prepareCountStatement(Connection connection) throws SQLException;

    /**
     * Prepares a statement for finding an entity by id.
     *
     * @param connection the connection
     * @param id         the id
     * @return the prepared statement
     * @throws SQLException if preparing fails
     */
    protected abstract PreparedStatement prepareFindByIdStatement(Connection connection, Integer id) throws SQLException;

    /**
     * Prepares a statement for creating an entity.
     *
     * @param connection the connection
     * @param entity     the entity to create
     * @return the prepared statement
     * @throws SQLException if preparing fails
     */
    protected abstract PreparedStatement prepareCreateStatement(Connection connection, TEntity entity) throws SQLException;

    /**
     * Prepares a statement for updating an entity.
     *
     * @param connection the connection
     * @param entity     the entity to update
     * @return the prepared statement
     * @throws SQLException if preparing fails
     */
    protected abstract PreparedStatement prepareUpdateStatement(Connection connection, TEntity entity) throws SQLException;

    @Override
    public Integer count() {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = prepareCountStatement(connection);
            var resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public final Optional<TEntity> getById(Integer id) {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = prepareFindByIdStatement(connection, id);
            var resultSet = statement.executeQuery();
            return resultSet.next() ? Optional.ofNullable(mapFromResultSet(resultSet)) : Optional.empty();
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
            return getById(id).orElseThrow(() -> new RuntimeException("Failed to create entity of type " + entity.getClass()));
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

            return getById(entity.id()).orElse(null);
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
}
