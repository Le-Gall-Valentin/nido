package com.nido.api.mail.infrastructure.persistence;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.infrastructure.encryption.CurrentOrLegacyTextEncryptor;
import com.nido.api.infrastructure.encryption.LegacyKeys;
import com.nido.api.mail.domain.model.OutboxEntry;
import com.nido.api.mail.domain.model.OutgoingMail;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mail is off in this context, so the adapter is not a bean here: it is built by hand on the
 * context's own JdbcClient and ObjectMapper, which costs no extra Spring context.
 */
@IntegrationTestConfig
class MailOutboxAdapterIT {

    private static final String SECRET = "integration-test-encryption-secret-32chars!";
    private static final TextEncryptor ENCRYPTOR = CurrentOrLegacyTextEncryptor.of(new EncryptionKey(SECRET), MailOutboxAdapter.SALT, () -> true);
    private static final TextEncryptor ANOTHER_KEY =
        CurrentOrLegacyTextEncryptor.of(new EncryptionKey("another-encryption-secret-at-least-32-chars"), MailOutboxAdapter.SALT, () -> true);

    @Autowired JdbcClient jdbc;
    @Autowired ObjectMapper json;
    @Autowired TransactionTemplate transactions;

    private MailOutboxAdapter outbox;
    private final Instant now = Instant.parse("2026-09-28T10:00:00Z");
    private final Duration lease = Duration.ofMinutes(2);

    @BeforeEach
    void setUp() {
        jdbc.sql("DELETE FROM mail_outbox").update();
        outbox = new MailOutboxAdapter(jdbc, json, ENCRYPTOR);
    }

    private static OutgoingMail mail(String address) {
        return new OutgoingMail(new Recipient(address, "Jane Doe"),
            new RenderedMail("Réinitialiser votre mot de passe",
                "<p>https://nido.example/reset-password#token=SECRET-TOKEN</p>",
                "https://nido.example/reset-password#token=SECRET-TOKEN"));
    }

    private int rows() {
        return jdbc.sql("SELECT count(*) FROM mail_outbox").query(Integer.class).single();
    }

    @Test
    void what_is_stored_reads_as_nothing_and_comes_back_whole() {
        outbox.enqueue("authentication/password-reset", mail("jane@example.com"), now, now.plus(Duration.ofMinutes(30)));

        String raw = jdbc.sql("SELECT payload FROM mail_outbox").query(String.class).single();
        assertThat(raw).doesNotContain("jane@example.com", "Jane", "SECRET-TOKEN", "Réinitialiser");

        List<OutboxEntry> claimed = outbox.claimDue(now, 20, lease);
        assertThat(claimed).singleElement().satisfies(entry -> {
            assertThat(entry.kind()).isEqualTo("authentication/password-reset");
            assertThat(entry.mail()).isEqualTo(mail("jane@example.com"));
            assertThat(entry.attempts()).isZero();
            assertThat(entry.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(30)));
        });
    }

    @Test
    void a_claimed_mail_is_invisible_to_other_claims_until_its_lease_ends() {
        outbox.enqueue("k", mail("jane@example.com"), now, null);

        assertThat(outbox.claimDue(now, 20, lease)).hasSize(1);
        assertThat(outbox.claimDue(now.plusSeconds(60), 20, lease)).isEmpty();
        assertThat(outbox.claimDue(now.plus(lease), 20, lease)).hasSize(1);
    }

    @Test
    void a_mail_due_later_is_not_claimed_now() {
        outbox.enqueue("k", mail("jane@example.com"), now.plusSeconds(60), null);

        assertThat(outbox.claimDue(now, 20, lease)).isEmpty();
        assertThat(outbox.claimDue(now.plusSeconds(60), 20, lease)).hasSize(1);
    }

    @Test
    void claims_at_most_the_limit_oldest_first() {
        for (int i = 0; i < 5; i++) {
            outbox.enqueue("k" + i, mail("jane" + i + "@example.com"), now.minusSeconds(10 - i), null);
        }

        assertThat(outbox.claimDue(now, 3, lease)).extracting(OutboxEntry::kind).containsExactly("k0", "k1", "k2");
    }

    @Test
    void claims_at_most_the_limit_whatever_plan_postgres_picks() {
        // A plan that reads the sub-select again for every row — a nested-loop semi-join — made the
        // claim take more than the limit: on each new read the rows this statement had just locked
        // were skipped, so the next ones came in. Postgres picks such a plan on its own for some table
        // statistics (the test above failed now and then); these settings make it pick it every time.
        for (int i = 0; i < 5; i++) {
            outbox.enqueue("k" + i, mail("jane" + i + "@example.com"), now.minusSeconds(10 - i), null);
        }

        List<OutboxEntry> claimed = transactions.execute(status -> {
            // Fresh statistics, so what the other tests left in them cannot steer the plan elsewhere.
            jdbc.sql("ANALYZE mail_outbox").update();
            for (String planner : List.of("enable_hashjoin", "enable_mergejoin", "enable_material",
                    "enable_hashagg", "enable_memoize", "enable_sort")) {
                jdbc.sql("SET LOCAL " + planner + " = off").update();
            }
            return outbox.claimDue(now, 3, lease);
        });

        assertThat(claimed).extracting(OutboxEntry::kind).containsExactly("k0", "k1", "k2");
    }

    @Test
    void two_dispatchers_claiming_at_once_never_get_the_same_mail() throws Exception {
        for (int i = 0; i < 40; i++) {
            outbox.enqueue("k", mail("jane" + i + "@example.com"), now, null);
        }
        CountDownLatch start = new CountDownLatch(1);
        Callable<List<UUID>> claim = () -> {
            start.await();
            return outbox.claimDue(now, 40, lease).stream().map(OutboxEntry::id).toList();
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<List<UUID>> first = pool.submit(claim);
            Future<List<UUID>> second = pool.submit(claim);
            start.countDown();
            List<UUID> all = new ArrayList<>(first.get());
            all.addAll(second.get());

            Set<UUID> distinct = new HashSet<>(all);
            assertThat(distinct).hasSameSizeAs(all);
            assertThat(all).hasSize(40);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rescheduling_records_the_failure_and_releases_the_lease() {
        outbox.enqueue("k", mail("jane@example.com"), now, null);
        UUID id = outbox.claimDue(now, 20, lease).getFirst().id();

        outbox.reschedule(id, 1, now.plusSeconds(60), "MailSendException > ConnectException");

        assertThat(jdbc.sql("SELECT attempts, last_error, locked_until, next_attempt_at FROM mail_outbox")
            .query((rs, n) -> List.<Object>of(rs.getInt(1), rs.getString(2), String.valueOf(rs.getObject(3)),
                rs.getObject(4, OffsetDateTime.class).toInstant().toString()))
            .single())
            .containsExactly(1, "MailSendException > ConnectException", "null", now.plusSeconds(60).toString());
        assertThat(outbox.claimDue(now.plusSeconds(60), 20, lease)).singleElement()
            .satisfies(entry -> assertThat(entry.attempts()).isEqualTo(1));
    }

    @Test
    void a_long_error_is_cut_to_the_column() {
        outbox.enqueue("k", mail("jane@example.com"), now, null);
        UUID id = outbox.claimDue(now, 20, lease).getFirst().id();

        outbox.reschedule(id, 1, now.plusSeconds(60), "x".repeat(900));

        assertThat(jdbc.sql("SELECT length(last_error) FROM mail_outbox").query(Integer.class).single()).isEqualTo(500);
    }

    @Test
    void deleting_removes_the_row() {
        outbox.enqueue("k", mail("jane@example.com"), now, null);
        outbox.delete(outbox.claimDue(now, 20, lease).getFirst().id());

        assertThat(rows()).isZero();
    }

    @Test
    void a_row_this_key_cannot_read_is_dropped_not_retried_forever() {
        // What NIDO_ENCRYPTION_SECRET changing under a queued mail looks like.
        MailOutboxAdapter withAnotherKey = new MailOutboxAdapter(jdbc, json, ANOTHER_KEY);
        withAnotherKey.enqueue("k", mail("jane@example.com"), now, null);
        outbox.enqueue("readable", mail("john@example.com"), now, null);

        assertThat(outbox.claimDue(now, 20, lease)).extracting(OutboxEntry::kind).containsExactly("readable");
        assertThat(rows()).isEqualTo(1);
    }

    @Test
    void the_mails_waiting_for_an_address_are_withdrawn_and_only_those() {
        outbox.enqueue("k", mail("jane@example.com"), now, null);
        outbox.enqueue("k", mail("JANE@example.com"), now, null);
        outbox.enqueue("k", mail("john@example.com"), now, null);

        assertThat(outbox.deleteAddressedTo("jane@example.com")).isEqualTo(2);

        assertThat(outbox.claimDue(now, 20, lease)).extracting(entry -> entry.mail().to().address())
            .containsExactly("john@example.com");
    }

    @Test
    void a_row_this_key_cannot_read_is_left_to_the_dispatcher() {
        MailOutboxAdapter withAnotherKey = new MailOutboxAdapter(jdbc, json, ANOTHER_KEY);
        withAnotherKey.enqueue("k", mail("jane@example.com"), now, null);

        assertThat(outbox.deleteAddressedTo("jane@example.com")).isZero();
        assertThat(rows()).isEqualTo(1);
    }

    @Test
    void a_mail_queued_in_a_rolled_back_transaction_is_gone_with_it() {
        transactions.executeWithoutResult(status -> {
            outbox.enqueue("k", mail("jane@example.com"), now, null);
            status.setRollbackOnly();
        });

        assertThat(rows()).isZero();
    }

    @Test
    void the_expiry_is_stored_in_utc_whatever_the_session_zone() {
        outbox.enqueue("k", mail("jane@example.com"), now, now.plusSeconds(1800));

        assertThat(jdbc.sql("SELECT expires_at FROM mail_outbox").query(OffsetDateTime.class).single()
            .withOffsetSameInstant(ZoneOffset.UTC).toInstant()).isEqualTo(now.plusSeconds(1800));
    }

    @Test
    void a_mail_is_queued_under_the_current_key_and_one_queued_before_0_16_is_still_sent() {
        outbox.enqueue("k", mail("jane@example.com"), now, null);
        assertThat(jdbc.sql("SELECT payload FROM mail_outbox").query(String.class).single()).startsWith("k2:");
        jdbc.sql("DELETE FROM mail_outbox").update();

        new MailOutboxAdapter(jdbc, json, LegacyKeys.writer(SECRET, MailOutboxAdapter.SALT)).enqueue("old", mail("john@example.com"), now, null);

        assertThat(outbox.claimDue(now, 20, lease)).extracting(entry -> entry.mail().to().address()).containsExactly("john@example.com");
    }

    @Test
    void the_queue_is_declared_for_the_migration_and_drops_what_no_key_reads() {
        assertThat(outbox.rekeyedColumn()).hasToString("mail_outbox.payload");
    }
}
