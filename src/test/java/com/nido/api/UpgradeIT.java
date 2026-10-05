package com.nido.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.Statement;

import static com.nido.api.InstallationTestSupport.boot;
import static com.nido.api.InstallationTestSupport.mvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** An installation of the previous version, with its variables, starting on this one — and nothing to do. */
@IntegrationTestConfig
class UpgradeIT {

    private static final String KEY = "the-key-this-installation-always-had-32+";
    private static final String OTHER = "a-key-someone-typed-by-mistake-32-chars+";
    private static final String JWT = "the-jwt-secret-of-this-installation-32+";

    @Autowired JdbcTemplate sharedDatabase;
    @TempDir Path dataDir;

    private String database;

    @BeforeEach
    void anInstallationOfThePreviousVersion() throws Exception {
        database = InstallationTestSupport.createDatabase(sharedDatabase);
        InstallationTestSupport.migrateUpTo(database, "064-");
        try (var connection = DriverManager.getConnection(SharedContainers.jdbcUrl(database),
                SharedContainers.POSTGRES.getUsername(), SharedContainers.POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO users (username, email, role) VALUES ('admin', 'admin@example.fr', 'SUPER_ADMIN')");
        }
    }

    @AfterEach
    void drop() {
        InstallationTestSupport.dropDatabase(sharedDatabase, database);
    }

    @Test
    void it_starts_as_before_and_from_then_on_refuses_another_key() throws Exception {
        try (ConfigurableApplicationContext app = boot(database, dataDir, "nido.encryption.secret=" + KEY, "nido.jwt.secret=" + JWT)) {
            mvc(app).perform(get("/api/setup/status")).andExpect(jsonPath("$.required").value(false));
            JdbcClient jdbc = app.getBean(JdbcClient.class);
            assertThat(jdbc.sql("SELECT setup_completed_at IS NOT NULL AND key_fingerprint IS NOT NULL AND NOT key_generated FROM instance")
                .query(Boolean.class).single()).isTrue();
        }
        assertThat(dataDir.resolve("secrets")).doesNotExist();

        assertThatThrownBy(() -> boot(database, dataDir, "nido.encryption.secret=" + OTHER, "nido.jwt.secret=" + JWT).close())
            .hasStackTraceContaining("not the one this database was encrypted with");
        assertThatThrownBy(() -> boot(database, dataDir, "nido.jwt.secret=" + JWT).close())
            .hasStackTraceContaining("was not provided");
    }
}
