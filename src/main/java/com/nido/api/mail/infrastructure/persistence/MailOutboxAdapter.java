package com.nido.api.mail.infrastructure.persistence;

import com.nido.api.infrastructure.config.ConditionalOnMailEnabled;
import com.nido.api.infrastructure.config.NidoProperties;
import com.nido.api.mail.domain.model.OutboxEntry;
import com.nido.api.mail.domain.model.OutgoingMail;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.domain.port.out.MailOutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The outbox in Postgres, through plain SQL: claiming needs {@code FOR UPDATE SKIP LOCKED} and
 * {@code RETURNING}, which say in one statement what JPA would take a transaction and three queries to.
 *
 * <p>Every statement joins the caller's transaction when there is one (enqueue, from inside the
 * sending context's) and commits on its own when there is none (claims, from the dispatcher, whose
 * lease must be visible before the SMTP exchange starts).
 *
 * <p>The payload is encrypted with a key derived from NIDO_ENCRYPTION_SECRET and a salt that belongs
 * to the outbox alone, so it can never be mistaken for the finance, calendar or TOTP keys. The key is
 * derived once: the salt never rotates, so there is nothing for an expiring cache to pick up.
 */
@Component
@ConditionalOnMailEnabled
public class MailOutboxAdapter implements MailOutboxPort {

    private static final Logger log = LoggerFactory.getLogger(MailOutboxAdapter.class);

    /** "nido-mail-outbox", hex-encoded as Encryptors expects. Not a secret: it separates keys. */
    static final String SALT = "6e69646f2d6d61696c2d6f7574626f78";

    private static final int LAST_ERROR_LENGTH = 500;

    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final TextEncryptor encryptor;

    @Autowired
    public MailOutboxAdapter(JdbcClient jdbc, ObjectMapper json, NidoProperties properties) {
        this(jdbc, json, Encryptors.delux(properties.encryption().secret(), SALT));
    }

    MailOutboxAdapter(JdbcClient jdbc, ObjectMapper json, TextEncryptor encryptor) {
        this.jdbc = jdbc;
        this.json = json;
        this.encryptor = encryptor;
    }

    @Override
    public void enqueue(String kind, OutgoingMail mail, Instant now, Instant expiresAt) {
        jdbc.sql("""
                INSERT INTO mail_outbox (id, kind, payload, created_at, next_attempt_at, expires_at, attempts)
                VALUES (:id, :kind, :payload, :now, :now, :expiresAt, 0)
                """)
            .param("id", UUID.randomUUID())
            .param("kind", kind)
            .param("payload", encryptor.encrypt(json.writeValueAsString(Payload.of(mail))))
            .param("now", utc(now))
            .param("expiresAt", utc(expiresAt), Types.TIMESTAMP_WITH_TIMEZONE)
            .update();
    }

    @Override
    public List<OutboxEntry> claimDue(Instant now, int limit, Duration lease) {
        // The batch is chosen once, in a materialized CTE. Inside "WHERE id IN (…)" Postgres may read
        // the sub-select again for every row it updates, and each new read skips the rows this very
        // statement has just locked — so the claim took more than the limit on some plans.
        List<ClaimedRow> rows = jdbc.sql("""
                WITH batch AS MATERIALIZED (
                    SELECT id FROM mail_outbox
                    WHERE next_attempt_at <= :now
                      AND (locked_until IS NULL OR locked_until <= :now)
                    ORDER BY next_attempt_at
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED)
                UPDATE mail_outbox SET locked_until = :leaseEnd
                FROM batch
                WHERE mail_outbox.id = batch.id
                RETURNING mail_outbox.id, mail_outbox.kind, mail_outbox.payload, mail_outbox.attempts,
                          mail_outbox.expires_at, mail_outbox.next_attempt_at
                """)
            .param("now", utc(now))
            .param("leaseEnd", utc(now.plus(lease)))
            .param("limit", limit)
            .query((rs, rowNum) -> new ClaimedRow(
                rs.getObject("id", UUID.class),
                rs.getString("kind"),
                rs.getString("payload"),
                rs.getInt("attempts"),
                instant(rs.getObject("expires_at", OffsetDateTime.class)),
                instant(rs.getObject("next_attempt_at", OffsetDateTime.class))))
            .list();

        List<OutboxEntry> entries = new ArrayList<>();
        rows.stream()
            .sorted((a, b) -> a.nextAttemptAt().compareTo(b.nextAttemptAt()))
            .forEach(row -> {
                try {
                    entries.add(new OutboxEntry(row.id(), row.kind(), decrypt(row.payload()), row.attempts(), row.expiresAt()));
                } catch (RuntimeException e) {
                    // A payload this key cannot read will never become sendable — NIDO_ENCRYPTION_SECRET
                    // changed under it, most likely. Kept, it would be claimed and fail forever. Only the
                    // class is logged: a parser's message can quote the decrypted text.
                    log.error("Mail {} ({}) cannot be read with the current key and is dropped: {}",
                        row.id(), row.kind(), e.getClass().getSimpleName());
                    delete(row.id());
                }
            });
        return entries;
    }

    @Override
    public void delete(UUID id) {
        jdbc.sql("DELETE FROM mail_outbox WHERE id = :id").param("id", id).update();
    }

    @Override
    public void reschedule(UUID id, int attempts, Instant nextAttemptAt, String lastError) {
        String error = lastError == null || lastError.length() <= LAST_ERROR_LENGTH
            ? lastError
            : lastError.substring(0, LAST_ERROR_LENGTH);
        jdbc.sql("""
                UPDATE mail_outbox
                SET attempts = :attempts, next_attempt_at = :next, last_error = :error, locked_until = NULL
                WHERE id = :id
                """)
            .param("attempts", attempts)
            .param("next", utc(nextAttemptAt))
            .param("error", error, Types.VARCHAR)
            .param("id", id)
            .update();
    }

    @Override
    public int deleteAddressedTo(String address) {
        // The address is encrypted with the rest, so the queue is read whole: it holds what waits to be sent,
        // a few rows seconds old, and this runs when an account is erased — rarely.
        List<UUID> addressed = jdbc.sql("SELECT id, payload FROM mail_outbox")
            .query((rs, rowNum) -> new StoredRow(rs.getObject("id", UUID.class), rs.getString("payload")))
            .list().stream()
            .filter(row -> isAddressedTo(row.payload(), address))
            .map(StoredRow::id)
            .toList();
        if (addressed.isEmpty()) {
            return 0;
        }
        return jdbc.sql("DELETE FROM mail_outbox WHERE id IN (:ids)").param("ids", addressed).update();
    }

    private boolean isAddressedTo(String payload, String address) {
        try {
            return decrypt(payload).to().address().equalsIgnoreCase(address);
        } catch (RuntimeException unreadable) {
            return false;
        }
    }

    private OutgoingMail decrypt(String payload) {
        return json.readValue(encryptor.decrypt(payload), Payload.class).toMail();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private record StoredRow(UUID id, String payload) {}

    private record ClaimedRow(UUID id, String kind, String payload, int attempts, Instant expiresAt, Instant nextAttemptAt) {}

    /** What is encrypted: the recipient and the written mail, nothing else. */
    record Payload(String address, String displayName, String subject, String html, String text) {

        static Payload of(OutgoingMail mail) {
            return new Payload(mail.to().address(), mail.to().displayName(),
                mail.content().subject(), mail.content().html(), mail.content().text());
        }

        OutgoingMail toMail() {
            return new OutgoingMail(new Recipient(address, displayName), new RenderedMail(subject, html, text));
        }
    }
}
