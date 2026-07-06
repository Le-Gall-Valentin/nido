package com.boilerplate.api.identity.application.handler;

import com.boilerplate.api.identity.application.port.in.UpdateMyProfileUseCase;
import com.boilerplate.api.identity.domain.model.IdentityException;
import com.boilerplate.api.identity.domain.model.UpdateProfileCommand;
import com.boilerplate.api.identity.domain.model.User;
import com.boilerplate.api.identity.domain.port.out.UserCommandPort;
import com.boilerplate.api.identity.domain.port.out.UserRepository;
import com.boilerplate.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateMyProfileHandler implements UpdateMyProfileUseCase {

    private final UserRepository userRepository;
    private final UserCommandPort userCommandPort;

    public UpdateMyProfileHandler(UserRepository userRepository, UserCommandPort userCommandPort) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
    }

    @Override
    @Transactional
    public void updateProfile(UpdateProfileCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(IdentityException.UserNotFound::new);
        if (!user.isActive()) {
            throw new IdentityException.UserNotActive();
        }
        userCommandPort.updateProfile(command);
    }
}