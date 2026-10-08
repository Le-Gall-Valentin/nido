package com.nido.api.identity.infrastructure.security;

import com.nido.api.identity.domain.model.EmailCodeCheck;
import com.nido.api.identity.domain.model.EmailCodeDelivery;
import com.nido.api.identity.domain.port.out.AddressChangeCodePort;
import com.nido.api.mfa.application.port.in.AddressChangeCodeUseCase;
import com.nido.api.mfa.domain.model.CodeDelivery;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** mfa's answers, said in identity's words: identity never meets an MfaException. */
@Component
public class MfaAddressChangeCodeAdapter implements AddressChangeCodePort {

    private final AddressChangeCodeUseCase codes;

    public MfaAddressChangeCodeAdapter(AddressChangeCodeUseCase codes) {
        this.codes = codes;
    }

    @Override
    public boolean mailMethodOn(UUID userId) {
        return codes.mailMethodOn(userId);
    }

    @Override
    public EmailCodeDelivery send(UUID userId, String newAddress) {
        return switch (codes.send(userId, newAddress)) {
            case CodeDelivery.Sent sent -> new EmailCodeDelivery.Sent(sent.resendAfterSeconds());
            case CodeDelivery.TooSoon tooSoon -> new EmailCodeDelivery.TooSoon(tooSoon.retryAfterSeconds());
            case CodeDelivery.LimitReached limit -> new EmailCodeDelivery.LimitReached(limit.retryAfterSeconds());
            case CodeDelivery.Unavailable unavailable -> new EmailCodeDelivery.Unavailable();
        };
    }

    @Override
    public EmailCodeCheck check(UUID userId, String newAddress, String code) {
        return switch (codes.check(userId, newAddress, code)) {
            case SUCCESS -> EmailCodeCheck.VALID;
            case INVALID, REPLAYED -> EmailCodeCheck.INVALID;
            case EXPIRED -> EmailCodeCheck.EXPIRED;
            case SPENT -> EmailCodeCheck.SPENT;
        };
    }

    @Override
    public void forgoMailMethod(UUID userId) {
        codes.forgoMailMethod(userId);
    }
}
