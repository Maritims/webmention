package no.clueless.webmention.persistence.sqlite;

import no.clueless.webmention.persistence.Entity;
import no.clueless.webmention.persistence.Repository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.sql.*;
import java.util.Objects;
import java.util.Optional;

import static no.clueless.webmention.persistence.sqlite.SqliteDatabasePathVerifier.extractAbsoluteDatabasePath;
import static no.clueless.webmention.persistence.sqlite.SqliteDatabasePathVerifier.verifyDatabasePath;

abstract public class SqliteBaseRepository<TEntity extends Entity<Integer>> implements Repository<TEntity, Integer> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SqliteBaseRepository.class);
    @NotNull
    protected final      String connectionString;
    @NotNull
    private final        String createTableQuery;

    protected SqliteBaseRepository(@NotNull String connectionString, @NotNull String createTableQuery) {
        if (connectionString.isBlank()) {
            throw new IllegalArgumentException("connectionString cannot be blank");
        }
        if (createTableQuery.isBlank()) {
            throw new IllegalArgumentException("createTableQuery cannot be blank");
        }

        this.connectionString = connectionString;
        this.createTableQuery = createTableQuery;
    }

    @NotNull
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

    @NotNull
    protected abstract TEntity mapFromResultSet(@NotNull ResultSet resultSet) throws SQLException;

    @NotNull
    protected abstract PreparedStatement prepareCountStatement(@NotNull Connection connection) throws SQLException;

    @NotNull
    protected abstract PreparedStatement prepareFindByIdStatement(@NotNull Connection connection, @NotNull Integer id) throws SQLException;

    @NotNull
    protected abstract PreparedStatement prepareCreateStatement(@NotNull Connection connection, @NotNull TEntity entity) throws SQLException;

    @NotNull
    protected abstract PreparedStatement prepareUpdateStatement(@NotNull Connection connection, @NotNull TEntity entity) throws SQLException;

    @Override
    public @NotNull Integer count() {
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
    public final @NotNull Optional<TEntity> findById(@NotNull Integer id) {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = prepareFindByIdStatement(connection, id);
            var resultSet = statement.executeQuery();
            return resultSet.next() ? Optional.of(mapFromResultSet(resultSet)) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public final @NotNull TEntity create(@NotNull TEntity entity) {
        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = prepareCreateStatement(connection, entity);
            statement.executeUpdate();
            var id = statement.getGeneratedKeys().getInt(1);
            return findById(id).orElseThrow(() -> new RuntimeException("Failed to find entity after creation"));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public final @NotNull TEntity update(@NotNull TEntity entity) {
        if (entity.id() == null) {
            throw new IllegalArgumentException("Entity must have an id");
        }

        try (var connection = DriverManager.getConnection(connectionString)) {
            var statement = prepareUpdateStatement(connection, entity);
            statement.executeUpdate();
            return findById(Objects.requireNonNull(entity.id())).orElseThrow(() -> new RuntimeException("Failed to find entity after update"));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public @NotNull TEntity upsert(
            @NotNull TEntity entityWithChanges,
            @NotNull FindExistingEntity<TEntity, Integer> findExistingEntity,
            @NotNull CreateNewEntity<TEntity, Integer> createNewEntity,
            @NotNull UpdateExistingEntity<TEntity, Integer> updateExistingEntity
    ) {
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
