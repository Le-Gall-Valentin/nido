package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.port.out.IssuedTokenCutoffPort;
import com.nido.api.infrastructure.config.NidoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * The cut-off lives in Redis with a TTL equal to the access token's own lifetime, which is what
 * keeps this from becoming a list that grows: a cut-off older than any token it could reject has
 * nothing left to reject, so Redis forgets it. In steady state the keyspace is empty — an entry
 * exists only in the minutes following an actual demotion or deactivation.
 *
 * <p><b>A Redis outage lets requests through rather than locking everyone out.</b> The alternative
 * turns a cache failure into a total one: nobody can use the application because a store that only
 * ever holds a handful of short-lived keys is unreachable. The exposure it trades for is the same
 * window that existed before this class — one token lifetime — and only for a user whose rights
 * changed during the outage. Read that way round, failing open costs nothing that was not already
 * being accepted, while failing closed costs everything.
 */
@Component
public class RedisIssuedTokenCutoffStore implements IssuedTokenCutoffPort {

    private static final Logger log = LoggerFactory.getLogger(RedisIssuedTokenCutoffStore.class);
    private static final String PREFIX = "auth:token-cutoff:user:";

    private final StringRedisTemplate redisTemplate;
    private final Duration retention;

    public RedisIssuedTokenCutoffStore(StringRedisTemplate redisTemplate, NidoProperties properties) {
        this.redisTemplate = redisTemplate;
        // One access token lifetime, plus a minute so that clock skew between the issuing and the
        // checking instance cannot let a token outlive the note that rejects it.
        this.retention = Duration.ofMinutes(properties.jwt().expiryMinutes() + 1L);
    }

    @Override
    public void cutOffNow(UUID userId) {
        redisTemplate.opsForValue().set(
            PREFIX + userId, Long.toString(Instant.now().toEpochMilli()), retention);
    }

    @Override
    public Optional<Instant> cutoffFor(UUID userId) {
        try {
            String value = redisTemplate.opsForValue().get(PREFIX + userId);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(Instant.ofEpochMilli(Long.parseLong(value)));
        } catch (DataAccessException | NumberFormatException e) {
            // See the class comment: unreachable or unreadable means "no cut-off known", never
            // "reject". Logged at warn because it is not normal, and silently letting tokens
            // through is precisely the kind of thing that must be visible.
            log.warn("Could not read the token cut-off for user {} — letting the request through", userId, e);
            return Optional.empty();
        }
    }
}
