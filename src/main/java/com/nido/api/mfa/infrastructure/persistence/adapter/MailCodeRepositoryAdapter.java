package com.nido.api.mfa.infrastructure.persistence.adapter;

import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.SentMailCode;
import com.nido.api.mfa.domain.port.out.MailCodeStorePort;
import com.nido.api.shared.model.TwoFactorPolicy;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * The codes in Postgres. Two sends at once are put in line by an advisory lock — a row lock would hold nothing
 * the first time, when there is no row yet — bounded like {@code PostgresAccountLock}'s, in a space of its own.
 */
@Component
public class MailCodeRepositoryAdapter implements MailCodeStorePort {

    /** mfa's own space of advisory keys; the second key is the account and purpose. */
    static final int LOCK_NAMESPACE = 1_002;
    static final String LOCK_TIMEOUT = "10s";

    private final JdbcClient jdbc;

    public MailCodeRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lock(UUID userId, CodePurpose purpose) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("A code is held until its transaction ends: hold it inside one");
        }
        jdbc.sql("SET LOCAL lock_timeout = '" + LOCK_TIMEOUT + "'").update();
        jdbc.sql("SELECT pg_advisory_xact_lock(:namespace, hashtext(:code))")
            .param("namespace", LOCK_NAMESPACE)
            .param("code", userId + "/" + purpose.name())
            .query((rs, rowNum) -> Boolean.TRUE)
            .single();
    }

    @Override
    public Optional<SentMailCode> find(UUID userId, CodePurpose purpose) {
        return jdbc.sql("""
                SELECT binding_hash, code_hash, failed_attempts, sent_at, expires_at
                FROM two_factor_mail_codes WHERE user_id = :userId AND purpose = :purpose
                """)
            .param("userId", userId)
            .param("purpose", purpose.name())
            .query((rs, rowNum) -> new SentMailCode(userId, purpose,
                rs.getString("binding_hash"),
                rs.getString("code_hash"),
                rs.getInt("failed_attempts"),
                rs.getObject("sent_at", OffsetDateTime.class).toInstant(),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant()))
            .optional();
    }

    @Override
    public void replace(SentMailCode code) {
        jdbc.sql("""
                INSERT INTO two_factor_mail_codes (user_id, purpose, binding_hash, code_hash, failed_attempts, sent_at, expires_at)
                VALUES (:userId, :purpose, :bindingHash, :codeHash, 0, :sentAt, :expiresAt)
                ON CONFLICT (user_id, purpose) DO UPDATE SET
                    binding_hash = EXCLUDED.binding_hash,
                    code_hash = EXCLUDED.code_hash,
                    failed_attempts = 0,
                    sent_at = EXCLUDED.sent_at,
                    expires_at = EXCLUDED.expires_at
                """)
            .param("userId", code.userId())
            .param("purpose", code.purpose().name())
            .param("bindingHash", code.bindingHash())
            .param("codeHash", code.codeHash())
            .param("sentAt", utc(code.sentAt()), Types.TIMESTAMP_WITH_TIMEZONE)
            .param("expiresAt", utc(code.expiresAt()), Types.TIMESTAMP_WITH_TIMEZONE)
            .update();
    }

    @Override
    public int recordFailure(UUID userId, CodePurpose purpose) {
        return jdbc.sql("""
                UPDATE two_factor_mail_codes SET failed_attempts = failed_attempts + 1
                WHERE user_id = :userId AND purpose = :purpose
                RETURNING failed_attempts
                """)
            .param("userId", userId)
            .param("purpose", purpose.name())
            .query(Integer.class)
            .optional()
            .orElse(0);
    }

    @Override
    public boolean take(UUID userId, CodePurpose purpose, String codeHash) {
        // The row lock makes a second delete wait for the first to commit, then find nothing. The failure count is
        // read again here: guesses running at once may each have compared a code before the fifth failure took it.
        return jdbc.sql("""
                DELETE FROM two_factor_mail_codes
                WHERE user_id = :userId AND purpose = :purpose AND code_hash = :codeHash AND failed_attempts < :maxAttempts
                """)
            .param("userId", userId)
            .param("purpose", purpose.name())
            .param("codeHash", codeHash)
            .param("maxAttempts", TwoFactorPolicy.MAX_ATTEMPTS)
            .update() == 1;
    }

    @Override
    public void delete(UUID userId, CodePurpose purpose) {
        jdbc.sql("DELETE FROM two_factor_mail_codes WHERE user_id = :userId AND purpose = :purpose")
            .param("userId", userId).param("purpose", purpose.name()).update();
    }

    @Override
    public void deleteAll(UUID userId) {
        jdbc.sql("DELETE FROM two_factor_mail_codes WHERE user_id = :userId").param("userId", userId).update();
    }

    @Override
    public int deleteExpired(Instant now) {
        return jdbc.sql("DELETE FROM two_factor_mail_codes WHERE expires_at <= :now")
            .param("now", utc(now), Types.TIMESTAMP_WITH_TIMEZONE).update();
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
