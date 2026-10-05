package com.nido.api;

import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.SetupCodePort;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * A whole Nido of its own, on its own database of the shared container — for what the shared test
 * context cannot show: a first start, a restart, a start with another key. The machine's environment is
 * neutralised with arguments, which outrank it: nothing is set but what the test says.
 */
public final class InstallationTestSupport {

    private InstallationTestSupport() {}

    public static String createDatabase(JdbcTemplate shared) {
        String name = "install_" + UUID.randomUUID().toString().replace("-", "");
        shared.execute("CREATE DATABASE " + name);
        return name;
    }

    public static void dropDatabase(JdbcTemplate shared, String name) {
        shared.execute("DROP DATABASE IF EXISTS " + name + " WITH (FORCE)");
    }

    /** The database as a version before 064 left it. */
    public static void migrateUpTo(String database, String changeSetPrefix) throws Exception {
        try (Connection connection = DriverManager.getConnection(SharedContainers.jdbcUrl(database),
                SharedContainers.POSTGRES.getUsername(), SharedContainers.POSTGRES.getPassword())) {
            Database target = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), target)) {
                List<ChangeSet> changeSets = liquibase.getDatabaseChangeLog().getChangeSets();
                int before = IntStream.range(0, changeSets.size())
                    .filter(i -> changeSets.get(i).getId().startsWith(changeSetPrefix))
                    .findFirst().orElseThrow();
                liquibase.update(before, "");
            }
        }
    }

    public static ConfigurableApplicationContext boot(String database, Path dataDir, String... overrides) {
        return boot(database, dataDir, List.of(), overrides);
    }

    /** With listeners on the application, to see what was ready at a given moment of its start. */
    public static ConfigurableApplicationContext boot(String database, Path dataDir, List<ApplicationListener<?>> listeners,
                                                      String... overrides) {
        Map<String, String> arguments = new LinkedHashMap<>();
        arguments.put("spring.datasource.url", SharedContainers.jdbcUrl(database));
        arguments.put("spring.datasource.username", SharedContainers.POSTGRES.getUsername());
        arguments.put("spring.datasource.password", SharedContainers.POSTGRES.getPassword());
        arguments.put("spring.data.redis.url", "redis://" + SharedContainers.REDIS.getHost() + ":" + SharedContainers.REDIS.getMappedPort(6379));
        arguments.put("spring.jpa.hibernate.ddl-auto", "none");
        arguments.put("server.port", "0");
        arguments.put("management.server.port", "0");
        arguments.put("NIDO_DATA_DIR", dataDir.toString());
        arguments.put("nido.jwt.secret", "");
        arguments.put("nido.encryption.secret", "");
        arguments.put("nido.seed.username", "");
        arguments.put("nido.seed.email", "");
        arguments.put("nido.seed.password", "");
        arguments.put("nido.cookie.secure", "false");
        arguments.put("nido.cors.allowed-origins", "");
        for (SettingKey key : SettingKey.values()) {
            arguments.put(key.variable(), "");
        }
        for (String override : overrides) {
            int equals = override.indexOf('=');
            arguments.put(override.substring(0, equals), override.substring(equals + 1));
        }
        // One argument per name: Spring joins repeated ones with commas.
        String[] args = arguments.entrySet().stream().map(e -> "--" + e.getKey() + "=" + e.getValue()).toArray(String[]::new);
        SpringApplication application = new SpringApplication(NidoApiApplication.class);
        application.addListeners(listeners.toArray(ApplicationListener<?>[]::new));
        ConfigurableApplicationContext context = application.run(args);
        context.getBean(RedisRateLimitBucketStore.class).clearAll();
        return context;
    }

    public static MockMvc mvc(ConfigurableApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
            .apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    /** The code the running application printed, read from its port rather than parsed from its log. */
    public static String setupCode(ConfigurableApplicationContext context) {
        return context.getBean(SetupCodePort.class).current().orElseThrow().value();
    }
}
