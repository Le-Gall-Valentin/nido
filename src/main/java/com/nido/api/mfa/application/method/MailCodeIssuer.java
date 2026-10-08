package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.SentMailCode;
import com.nido.api.mfa.domain.port.out.MailAvailabilityPort;
import com.nido.api.mfa.domain.port.out.MailCodeGeneratorPort;
import com.nido.api.mfa.domain.port.out.MailCodeHasherPort;
import com.nido.api.mfa.domain.port.out.MailCodeSendLimitPort;
import com.nido.api.mfa.domain.port.out.MailCodeStorePort;
import com.nido.api.mfa.domain.port.out.TwoFactorMailPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorPolicy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Every code Nido sends by mail, whatever it is for: one live code per account and purpose, bound to what it
 * protects, never kept in clear, sent at most once a minute for the same thing and five times a quarter of an
 * hour in all. The only place a code exists in clear is the mail that carries it.
 */
@ApplicationService
public class MailCodeIssuer {

    private final MailCodeStorePort codes;
    private final MailCodeSendLimitPort limit;
    private final MailCodeGeneratorPort generator;
    private final MailCodeHasherPort hasher;
    private final TwoFactorMailPort mails;
    private final MailAvailabilityPort availability;
    private final Clock clock;

    public MailCodeIssuer(MailCodeStorePort codes, MailCodeSendLimitPort limit, MailCodeGeneratorPort generator,
                          MailCodeHasherPort hasher, TwoFactorMailPort mails, MailAvailabilityPort availability, Clock clock) {
        this.codes = codes;
        this.limit = limit;
        this.generator = generator;
        this.hasher = hasher;
        this.mails = mails;
        this.availability = availability;
        this.clock = clock;
    }

    /**
     * Writes a new code, replacing the last one for this purpose, and queues its mail to {@code address}.
     *
     * @param binding what the code is bound to: the challenge id, the account id, or the new address
     */
    public CodeDelivery issue(UUID userId, CodePurpose purpose, String binding, String address) {
        if (!availability.isAvailable()) {
            return new CodeDelivery.Unavailable();
        }
        Instant now = clock.instant();
        String bindingHash = hasher.bindingHash(binding);
        Optional<SentMailCode> previous = codes.find(userId, purpose);
        if (previous.isPresent() && previous.get().bindingHash().equals(bindingHash)) {
            long wait = TwoFactorPolicy.RESEND_DELAY.toSeconds() - Duration.between(previous.get().sentAt(), now).toSeconds();
            if (wait > 0) {
                return new CodeDelivery.TooSoon(wait);
            }
        }
        OptionalLong refused = limit.tryCount(userId);
        if (refused.isPresent()) {
            return new CodeDelivery.LimitReached(refused.getAsLong());
        }
        String code = generator.newCode();
        Instant expiresAt = now.plus(TwoFactorPolicy.CODE_VALIDITY);
        codes.replace(new SentMailCode(userId, purpose, bindingHash, hasher.codeHash(binding, code), 0, now, expiresAt));
        mails.sendCode(userId, address, purpose, code, expiresAt);
        return new CodeDelivery.Sent(TwoFactorPolicy.RESEND_DELAY.toSeconds());
    }

    /**
     * A right code is used up. A wrong one counts against the code, and the fifth takes it — at sign-in, the
     * account's counter does the counting. Nothing waiting for this binding is told apart from a wrong guess.
     */
    public CodeCheck check(UUID userId, CodePurpose purpose, String binding, String code) {
        Optional<SentMailCode> sent = codes.find(userId, purpose)
            .filter(live -> live.expiresAt().isAfter(clock.instant()))
            .filter(live -> live.bindingHash().equals(hasher.bindingHash(binding)));
        if (sent.isEmpty()) {
            return CodeCheck.EXPIRED;
        }
        if (hasher.matches(sent.get().codeHash(), binding, code)) {
            // Taken, not merely deleted: of two requests carrying the right code at once, only one gets in.
            return codes.take(userId, purpose, sent.get().codeHash()) ? CodeCheck.SUCCESS : CodeCheck.EXPIRED;
        }
        if (purpose != CodePurpose.LOGIN && codes.recordFailure(userId, purpose) >= TwoFactorPolicy.MAX_ATTEMPTS) {
            codes.delete(userId, purpose);
            return CodeCheck.SPENT;
        }
        return CodeCheck.INVALID;
    }

    public void forget(UUID userId) {
        codes.deleteAll(userId);
    }
}
