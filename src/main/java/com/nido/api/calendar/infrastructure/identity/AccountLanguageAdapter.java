package com.nido.api.calendar.infrastructure.identity;

import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.infrastructure.web.MailLanguage;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/** The account's own language, as mails pick it: the request's language when it has none, English otherwise. */
@Component
public class AccountLanguageAdapter implements AccountLanguage {

    private final FindUserUseCase findUser;

    public AccountLanguageAdapter(FindUserUseCase findUser) {
        this.findUser = findUser;
    }

    @Override
    public Locale of(UUID userId) {
        return MailLanguage.resolve(findUser.findById(userId).map(User::language).orElse(null));
    }
}
