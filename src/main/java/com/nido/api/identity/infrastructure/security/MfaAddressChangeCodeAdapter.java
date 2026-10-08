package com.nido.api.identity.infrastructure.security;

import com.nido.api.identity.domain.port.out.AddressChangeCodePort;
import com.nido.api.mfa.application.port.in.AddressChangeCodeUseCase;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MfaAddressChangeCodeAdapter implements AddressChangeCodePort {

    private final AddressChangeCodeUseCase codes;

    public MfaAddressChangeCodeAdapter(AddressChangeCodeUseCase codes) {
        this.codes = codes;
    }

    @Override
    public boolean required(UUID userId) {
        return codes.required(userId);
    }

    @Override
    public long send(UUID userId, String newAddress) {
        return codes.send(userId, newAddress);
    }

    @Override
    public boolean check(UUID userId, String newAddress, String code) {
        return codes.check(userId, newAddress, code);
    }
}
