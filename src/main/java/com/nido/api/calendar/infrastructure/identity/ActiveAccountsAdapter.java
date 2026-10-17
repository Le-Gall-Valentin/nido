package com.nido.api.calendar.infrastructure.identity;

import com.nido.api.calendar.domain.port.out.ActiveAccounts;
import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** An account that is deactivated, or gone, may not sign in. */
@Component
public class ActiveAccountsAdapter implements ActiveAccounts {

    private final FindUserUseCase findUser;

    public ActiveAccountsAdapter(FindUserUseCase findUser) {
        this.findUser = findUser;
    }

    @Override
    public boolean isActive(UUID userId) {
        return findUser.findById(userId).map(User::isActive).orElse(false);
    }
}
