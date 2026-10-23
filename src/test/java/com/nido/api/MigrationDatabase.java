package com.nido.api;

import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * An empty database of the shared container, migrated a changeset at a time: rows are written the way an earlier
 * version wrote them, then the migrations under test run on them. Dropped when closed.
 */
public final class MigrationDatabase implements AutoCloseable {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

    private final JdbcTemplate shared;
    private final String name;
    private final Connection connection;
    private final Liquibase liquibase;

    private MigrationDatabase(JdbcTemplate shared) throws Exception {
        this.shared = shared;
        name = "migration_" + UUID.randomUUID().toString().replace("-", "");
        shared.execute("CREATE DATABASE " + name);
        String url = "jdbc:postgresql://" + SharedContainers.POSTGRES.getHost() + ":"
            + SharedContainers.POSTGRES.getMappedPort(5432) + "/" + name;
        connection = DriverManager.getConnection(url, SharedContainers.POSTGRES.getUsername(),
            SharedContainers.POSTGRES.getPassword());
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
    }

    /** A new empty database, beside the one {@code shared} reaches. */
    public static MigrationDatabase create(JdbcTemplate shared) throws Exception {
        return new MigrationDatabase(shared);
    }

    /** Runs every changeset before the first whose id starts with {@code changeSetIdPrefix}. */
    public void migrateUpTo(String changeSetIdPrefix) throws LiquibaseException {
        List<ChangeSet> changeSets = liquibase.getDatabaseChangeLog().getChangeSets();
        int before = IntStream.range(0, changeSets.size())
            .filter(i -> changeSets.get(i).getId().startsWith(changeSetIdPrefix))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no changeset id starts with " + changeSetIdPrefix));
        liquibase.update(before, "");
    }

    public void migrateToTheEnd() throws LiquibaseException {
        liquibase.update("");
    }

    public void execute(String sql) throws SQLException {
        connection.setAutoCommit(true);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /** The first column of every row {@code sql} gives, as text. */
    public List<String> strings(String sql) throws SQLException {
        connection.setAutoCommit(true);
        List<String> values = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        }
        return values;
    }

    @Override
    public void close() throws Exception {
        liquibase.close();
        shared.execute("DROP DATABASE IF EXISTS " + name + " WITH (FORCE)");
    }
}
