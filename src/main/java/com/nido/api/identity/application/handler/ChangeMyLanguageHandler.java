package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.ChangeMyLanguageUseCase;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.UserCommandPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.Language;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Records the language a person reads the app in. The account's language wins over any device's
 * detection once signed in, and it is what a mail is written in when nobody is there to ask.
 */
@ApplicationService
public class ChangeMyLanguageHandler implements ChangeMyLanguageUseCase {

    private final UserRepository userRepository;
    private final UserCommandPort userCommandPort;

    public ChangeMyLanguageHandler(UserRepository userRepository, UserCommandPort userCommandPort) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
    }

    @Override
    @Transactional
    public void changeLanguage(UUID userId, Language language) {
        User user = userRepository.findById(userId).orElseThrow(IdentityException.UserNotFound::new);
        if (!user.isActive()) {
            throw new IdentityException.UserNotActive();
        }
        userCommandPort.updateLanguage(userId, language);
    }
}
