package com.nido.api.mfa.infrastructure.security;

import com.nido.api.infrastructure.config.NidoProperties;
import com.nido.api.mfa.domain.port.out.PendingTotpEnrolmentPort;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Where an enrolment in progress lives: alongside the TOTP challenge, the anti-replay marks and the
 * confirmation attempt counter, rather than in the row that describes the account.
 *
 * <p>Expiry is the point, and it is structural here rather than a check somebody has to remember.
 * Once the key is gone there is nothing left to confirm, so {@code ConfirmTotpHandler} cannot
 * accept an enrolment that has timed out even if it forgot to look — which is what an expiry stored
 * as a timestamp column would have required at two separate call sites.
 *
 * <p>It also removes a transactional mismatch rather than working around one. Discarding the
 * enrolment used to be a JPA write while the attempt counter it was paired with was a Redis write,
 * so a rollback could undo one and not the other — a secret kept alive with its guess counter back
 * at zero. Both now live in the same non-transactional store and cannot come apart.
 *
 * <p><b>The secret is encrypted before it is written.</b> It is encrypted at rest in Postgres with a
 * per-user salt, and moving it must not quietly downgrade that: this Redis has neither password nor
 * TLS and persists to disk. The same {@link TotpEncryptorFactory} is used, so the protection
 * travels with the value.
 */
@Component
public class RedisPendingTotpEnrolmentStore implements PendingTotpEnrolmentPort {

    private static final String PREFIX = "totp:enrolment:user:";
    private static final int DEFAULT_TTL_MINUTES = 15;

    private final StringRedisTemplate redisTemplate;
    private final TotpEncryptorFactory encryptorFactory;
    private final Duration ttl;

    public RedisPendingTotpEnrolmentStore(StringRedisTemplate redisTemplate,
                                          TotpEncryptorFactory encryptorFactory,
                                          NidoProperties properties) {
        this.redisTemplate = redisTemplate;
        this.encryptorFactory = encryptorFactory;
        NidoProperties.SecurityProperties security = properties.security();
        this.ttl = Duration.ofMinutes(
            security != null ? security.totpSetupTtlMinutes() : DEFAULT_TTL_MINUTES);
    }

    @Override
    public boolean startIfAbsent(UUID userId, String secret) {
        // SET NX EX is the atomic equivalent of the UPDATE ... WHERE totp_secret IS NULL it
        // replaces: the first caller wins, the second is told to read what is already there.
        Boolean written = redisTemplate.opsForValue().setIfAbsent(
            PREFIX + userId, encryptorFactory.forUser(userId).encrypt(secret), ttl);
        return Boolean.TRUE.equals(written);
    }

    @Override
    public Optional<String> find(UUID userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(PREFIX + userId))
            .map(stored -> encryptorFactory.forUser(userId).decrypt(stored));
    }

    @Override
    public void discard(UUID userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}
