package com.synapsys.api.shared.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailFactoryTest {

    @Test
    void of_setsStatus() {
        ProblemDetail problem = ProblemDetailFactory.of(HttpStatus.NOT_FOUND, "TestTitle", "detail", URI.create("/test"));

        assertThat(problem.getStatus()).isEqualTo(404);
    }

    @Test
    void of_setsTitle() {
        ProblemDetail problem = ProblemDetailFactory.of(HttpStatus.CONFLICT, "MyTitle", "detail", URI.create("/test"));

        assertThat(problem.getTitle()).isEqualTo("MyTitle");
    }

    @Test
    void of_setsDetail() {
        ProblemDetail problem = ProblemDetailFactory.of(HttpStatus.BAD_REQUEST, "T", "my detail", URI.create("/test"));

        assertThat(problem.getDetail()).isEqualTo("my detail");
    }

    @Test
    void of_setsInstance() {
        URI instance = URI.create("/api/resource/123");
        ProblemDetail problem = ProblemDetailFactory.of(HttpStatus.OK, "T", "d", instance);

        assertThat(problem.getInstance()).isEqualTo(instance);
    }
}