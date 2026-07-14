package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.GetCurrentUserUseCase;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.model.UserSelfView;
import com.nido.api.identity.domain.port.out.TotpStatusPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class GetCurrentUserHandler implements GetCurrentUserUseCase {

    private final UserRepository userRepository;
    private final TotpStatusPort totpStatusPort;

    public GetCurrentUserHandler(UserRepository userRepository, TotpStatusPort totpStatusPort) {
        this.userRepository = userRepository;
        this.totpStatusPort = totpStatusPort;
    }

    @Override
    @Transactional(readOnly = true)
    public UserSelfView getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(IdentityException.UserNotFound::new);
        if (!user.isActive()) {
            throw new IdentityException.UserNotActive();
        }
        boolean totpEnabled = totpStatusPort.isTotpEnabled(userId);
        return new UserSelfView(user.id(), user.username(), user.email(), user.role(), user.createdAt(), totpEnabled);
    }
}