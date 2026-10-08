package com.nido.api.mfa.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.SentMailCode;
import com.nido.api.mfa.domain.port.out.MailCodeStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class MailCodeRepositoryAdapterIT {

    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");

    @Autowired MailCodeStorePort codes;
    @Autowired JdbcClient jdbc;

    private UUID jane;

    @BeforeEach
    void account() {
        jdbc.sql("DELETE FROM two_factor_mail_codes").update();
        jdbc.sql("DELETE FROM users WHERE username = 'mcr-jane'").update();
        jane = jdbc.sql("INSERT INTO users (username, email, role) VALUES ('mcr-jane', 'mcr-jane@example.fr', 'USER') RETURNING id")
            .query(UUID.class).single();
    }

    private SentMailCode code(CodePurpose purpose, String binding, Instant sentAt) {
        return new SentMailCode(jane, purpose, binding, "hash-" + binding, 0, sentAt, sentAt.plus(10, ChronoUnit.MINUTES));
    }

    @Test
    void a_code_is_read_back_as_written() {
        codes.replace(code(CodePurpose.LOGIN, "b1", NOW));

        assertThat(codes.find(jane, CodePurpose.LOGIN)).contains(code(CodePurpose.LOGIN, "b1", NOW));
        assertThat(codes.find(jane, CodePurpose.ENROL)).isEmpty();
    }

    @Test
    void a_new_code_for_the_same_purpose_replaces_the_old_one_and_its_failures() {
        codes.replace(code(CodePurpose.LOGIN, "b1", NOW));
        codes.recordFailure(jane, CodePurpose.LOGIN);

        codes.replace(code(CodePurpose.LOGIN, "b2", NOW.plusSeconds(90)));

        assertThat(codes.find(jane, CodePurpose.LOGIN)).contains(code(CodePurpose.LOGIN, "b2", NOW.plusSeconds(90)));
    }

    @Test
    void each_purpose_keeps_its_own_code() {
        codes.replace(code(CodePurpose.LOGIN, "b1", NOW));
        codes.replace(code(CodePurpose.DISABLE, "b2", NOW));

        codes.delete(jane, CodePurpose.LOGIN);

        assertThat(codes.find(jane, CodePurpose.LOGIN)).isEmpty();
        assertThat(codes.find(jane, CodePurpose.DISABLE)).isPresent();
    }

    @Test
    void failures_add_up_and_a_missing_code_counts_none() {
        codes.replace(code(CodePurpose.ENROL, "b1", NOW));

        assertThat(codes.recordFailure(jane, CodePurpose.ENROL)).isEqualTo(1);
        assertThat(codes.recordFailure(jane, CodePurpose.ENROL)).isEqualTo(2);
        assertThat(codes.recordFailure(jane, CodePurpose.DISABLE)).isZero();
    }

    @Test
    void the_purge_takes_expired_codes_only() {
        codes.replace(code(CodePurpose.LOGIN, "old", NOW.minus(20, ChronoUnit.MINUTES)));
        codes.replace(code(CodePurpose.ENROL, "live", NOW));

        assertThat(codes.deleteExpired(NOW)).isEqualTo(1);
        assertThat(codes.find(jane, CodePurpose.LOGIN)).isEmpty();
        assertThat(codes.find(jane, CodePurpose.ENROL)).isPresent();
    }

    @Test
    void deleting_an_account_data_takes_every_code() {
        codes.replace(code(CodePurpose.LOGIN, "b1", NOW));
        codes.replace(code(CodePurpose.ENROL, "b2", NOW));

        codes.deleteAll(jane);

        assertThat(codes.find(jane, CodePurpose.LOGIN)).isEmpty();
        assertThat(codes.find(jane, CodePurpose.ENROL)).isEmpty();
    }

    @Test
    void a_code_is_taken_once_and_only_while_it_is_still_the_code_that_was_checked() {
        codes.replace(code(CodePurpose.LOGIN, "b1", NOW));

        assertThat(codes.take(jane, CodePurpose.LOGIN, "hash-b2")).isFalse();
        assertThat(codes.find(jane, CodePurpose.LOGIN)).isPresent();
        assertThat(codes.take(jane, CodePurpose.LOGIN, "hash-b1")).isTrue();
        assertThat(codes.take(jane, CodePurpose.LOGIN, "hash-b1")).isFalse();
        assertThat(codes.find(jane, CodePurpose.LOGIN)).isEmpty();
    }
}
