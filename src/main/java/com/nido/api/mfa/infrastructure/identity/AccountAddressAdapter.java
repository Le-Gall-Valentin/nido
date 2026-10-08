package com.nido.api.mfa.infrastructure.identity;

import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.mfa.domain.port.out.AccountAddressPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AccountAddressAdapter implements AccountAddressPort {

    private final FindUserUseCase findUser;

    public AccountAddressAdapter(FindUserUseCase findUser) {
        this.findUser = findUser;
    }

    @Override
    public Optional<String> addressOf(UUID userId) {
        return findUser.findById(userId).map(User::email).filter(address -> !address.isBlank());
    }
}
