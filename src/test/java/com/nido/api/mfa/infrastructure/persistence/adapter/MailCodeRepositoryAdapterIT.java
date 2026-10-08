package com.nido.api.mfa.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.SentMailCode;
import com.nido.api.mfa.domain.port.out.MailCodeStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class MailCodeRepositoryAdapterIT {

    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");

    @Autowired MailCodeStorePort codes;
    @Autowired JdbcClient jdbc;
    @Autowired PlatformTransactionManager transactions;

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
    void a_second_hold_on_the_same_code_waits_for_the_first_transaction_to_end() throws Exception {
        TransactionTemplate inTransaction = new TransactionTemplate(transactions);
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> first = CompletableFuture.runAsync(() -> inTransaction.executeWithoutResult(status -> {
            codes.lock(jane, CodePurpose.LOGIN);
            held.countDown();
            await(release);
        }));
        assertThat(held.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Void> second = CompletableFuture.runAsync(() ->
            inTransaction.executeWithoutResult(status -> codes.lock(jane, CodePurpose.LOGIN)));
        CompletableFuture<Void> otherPurpose = CompletableFuture.runAsync(() ->
            inTransaction.executeWithoutResult(status -> codes.lock(jane, CodePurpose.DISABLE)));

        otherPurpose.get(10, TimeUnit.SECONDS);
        Thread.sleep(300);
        assertThat(second).as("waiting on the first").isNotDone();
        release.countDown();
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);
    }

    @Test
    void a_hold_outside_a_transaction_is_refused_rather_than_released_at_once() {
        assertThatThrownBy(() -> codes.lock(jane, CodePurpose.LOGIN)).isInstanceOf(IllegalStateException.class);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

    @Test
    void a_code_with_five_failures_recorded_cannot_be_taken_even_by_a_guess_already_under_way() {
        // Requests running at once can each compare a guess before the fifth failure deletes the code.
        codes.replace(code(CodePurpose.EMAIL_CHANGE, "b1", NOW));
        for (int i = 0; i < 5; i++) {
            codes.recordFailure(jane, CodePurpose.EMAIL_CHANGE);
        }

        assertThat(codes.take(jane, CodePurpose.EMAIL_CHANGE, "hash-b1")).isFalse();
    }
}
