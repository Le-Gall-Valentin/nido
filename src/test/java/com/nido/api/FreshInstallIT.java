package com.nido.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.nido.api.InstallationTestSupport.boot;
import static com.nido.api.InstallationTestSupport.mvc;
import static com.nido.api.InstallationTestSupport.setupCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A first start with nothing given at all, then the setup screen's calls, as a browser makes them. */
@IntegrationTestConfig
class FreshInstallIT {

    @Autowired JdbcTemplate sharedDatabase;
    @TempDir Path dataDir;

    private String database;

    @BeforeEach
    void create() {
        database = InstallationTestSupport.createDatabase(sharedDatabase);
    }

    @AfterEach
    void drop() {
        InstallationTestSupport.dropDatabase(sharedDatabase, database);
    }

    private Path keyFile() {
        return dataDir.resolve("secrets/encryption-key");
    }

    private static String code(String code) {
        return "{\"code\":\"" + code + "\"}";
    }

    private static String complete(String code, String username, boolean keySaved) {
        return """
            {"code":"%s","admin":{"username":"%s","email":"%s@example.fr","password":"Str0ng!Password","language":"fr"},
             "publicUrl":"http://192.168.1.10:8080/","mail":null,"encryptionKeySaved":%s}""".formatted(code, username, username, keySaved);
    }

    @Test
    void a_fresh_installation_makes_its_secrets_and_is_set_up_from_the_browser() throws Exception {
        try (ConfigurableApplicationContext app = boot(database, dataDir)) {
            assertThat(keyFile()).exists();
            assertThat(dataDir.resolve("secrets/jwt-secret")).exists();
            MockMvc mvc = mvc(app);
            String code = setupCode(app);

            mvc.perform(get("/api/setup/status")).andExpect(jsonPath("$.required").value(true));
            mvc.perform(post("/api/setup/verify-code").contentType(MediaType.APPLICATION_JSON).content(code("AAAA-AAAA-AAAA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("SETUP_CODE_INVALID"));
            mvc.perform(post("/api/setup/verify-code").contentType(MediaType.APPLICATION_JSON)
                    .content(code(code.toLowerCase().replace('-', ' '))))
                .andExpect(status().isNoContent());
            mvc.perform(post("/api/setup/encryption-key").contentType(MediaType.APPLICATION_JSON).content(code(code)))
                .andExpect(jsonPath("$.source").value("GENERATED"))
                .andExpect(jsonPath("$.key").value(Files.readString(keyFile()).strip()));
            mvc.perform(post("/api/setup/complete").contentType(MediaType.APPLICATION_JSON).content(complete(code, "jane", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("ENCRYPTION_KEY_NOT_SAVED"));
            mvc.perform(post("/api/setup/complete").contentType(MediaType.APPLICATION_JSON).content(complete(code, "jane", true)))
                .andExpect(status().isCreated());

            mvc.perform(get("/api/setup/status")).andExpect(jsonPath("$.required").value(false));
            mvc.perform(post("/api/setup/encryption-key").contentType(MediaType.APPLICATION_JSON).content(code(code)))
                .andExpect(status().isNotFound());
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"identifier\":\"jane\",\"password\":\"Str0ng!Password\"}"))
                .andExpect(status().isOk());
            assertThat(app.getBean(JdbcClient.class).sql("SELECT value FROM instance_settings WHERE key = 'public-url'")
                .query(String.class).single()).isEqualTo("http://192.168.1.10:8080");
        }
    }

    @Test
    void a_restart_mid_setup_keeps_the_key_and_changes_the_code_and_a_lost_key_after_setup_stops_the_start() throws Exception {
        String firstCode;
        String key;
        try (ConfigurableApplicationContext app = boot(database, dataDir)) {
            firstCode = setupCode(app);
            key = Files.readString(keyFile()).strip();
        }
        try (ConfigurableApplicationContext app = boot(database, dataDir)) {
            MockMvc mvc = mvc(app);
            String code = setupCode(app);
            assertThat(code).as("a new code at each start (a collision is a 1 in 2^60 event)").isNotEqualTo(firstCode);
            assertThat(Files.readString(keyFile()).strip()).isEqualTo(key);
            mvc.perform(post("/api/setup/verify-code").contentType(MediaType.APPLICATION_JSON).content(code(firstCode)))
                .andExpect(status().isForbidden());
            mvc.perform(post("/api/setup/encryption-key").contentType(MediaType.APPLICATION_JSON).content(code(code)))
                .andExpect(jsonPath("$.key").value(key));
            mvc.perform(post("/api/setup/complete").contentType(MediaType.APPLICATION_JSON).content(complete(code, "jane", true)))
                .andExpect(status().isCreated());
        }

        Files.delete(keyFile());

        assertThatThrownBy(() -> boot(database, dataDir).close()).hasStackTraceContaining("was not provided");
    }

    @Test
    void two_completions_at_the_same_moment_make_one_administrator() throws Exception {
        try (ConfigurableApplicationContext app = boot(database, dataDir)) {
            MockMvc mvc = mvc(app);
            String code = setupCode(app);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Integer>> answers = List.of("alice", "bob").stream().map(name -> pool.submit(() -> {
                go.await();
                return mvc.perform(post("/api/setup/complete").contentType(MediaType.APPLICATION_JSON).content(complete(code, name, true)))
                    .andReturn().getResponse().getStatus();
            })).toList();
            go.countDown();

            List<Integer> statuses = answers.stream().map(answer -> {
                try {
                    return answer.get(60, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
            pool.shutdown();

            assertThat(statuses).containsExactlyInAnyOrder(201, 404);
            assertThat(app.getBean(JdbcClient.class).sql("SELECT count(*) FROM users").query(Integer.class).single()).isEqualTo(1);
        }
    }
}
