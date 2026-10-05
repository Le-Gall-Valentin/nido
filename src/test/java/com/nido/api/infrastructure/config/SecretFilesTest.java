package com.nido.api.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker secrets are files named after a variable. Read through a config tree, each one becomes that
 * variable — and an explicit value of the same name, more specific, still wins.
 */
class SecretFilesTest {

    @TempDir Path secrets;

    @Configuration(proxyBeanMethods = false)
    static class Nothing {}

    private ConfigurableApplicationContext start(String... arguments) {
        String[] all = new String[arguments.length + 1];
        all[0] = "--spring.config.import=optional:configtree:" + secrets + "/";
        System.arraycopy(arguments, 0, all, 1, arguments.length);
        return new SpringApplicationBuilder(Nothing.class).web(WebApplicationType.NONE).run(all);
    }

    @Test
    void a_file_named_after_a_variable_is_read_as_that_variable() throws Exception {
        Files.writeString(secrets.resolve("NIDO_DB_PASSWORD"), "from-a-file");

        try (ConfigurableApplicationContext context = start()) {
            assertThat(context.getEnvironment().getProperty("NIDO_DB_PASSWORD")).isEqualTo("from-a-file");
        }
    }

    @Test
    void an_explicit_value_of_the_same_name_wins_over_the_file() throws Exception {
        Files.writeString(secrets.resolve("NIDO_DB_PASSWORD"), "from-a-file");

        try (ConfigurableApplicationContext context = start("--NIDO_DB_PASSWORD=explicit")) {
            assertThat(context.getEnvironment().getProperty("NIDO_DB_PASSWORD")).isEqualTo("explicit");
        }
    }

    @Test
    void the_application_reads_the_docker_secrets_directory() throws Exception {
        String yaml = new ClassPathResource("application.yaml").getContentAsString(StandardCharsets.UTF_8);

        assertThat(yaml).contains("optional:configtree:/run/secrets/");
    }
}
