package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.SeedUseCase;
import com.nido.api.identity.domain.model.CreateUserProfileCommand;
import com.nido.api.identity.domain.port.out.CredentialSetupPort;
import com.nido.api.identity.domain.port.out.PersonalSpaceInitPort;
import com.nido.api.identity.domain.port.out.TotpRecordInitPort;
import com.nido.api.identity.domain.port.out.UserAdminPort;
import com.nido.api.identity.domain.port.out.UserCommandPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class SeedHandler implements SeedUseCase {

    private static final Logger log = LoggerFactory.getLogger(SeedHandler.class);

    private final UserAdminPort userAdminPort;
    private final UserCommandPort userCommandPort;
    private final CredentialSetupPort credentialSetupPort;
    private final TotpRecordInitPort totpRecordInitPort;
    private final PersonalSpaceInitPort personalSpaceInitPort;

    public SeedHandler(UserAdminPort userAdminPort,
                       UserCommandPort userCommandPort,
                       CredentialSetupPort credentialSetupPort,
                       TotpRecordInitPort totpRecordInitPort,
                       PersonalSpaceInitPort personalSpaceInitPort) {
        this.userAdminPort = userAdminPort;
        this.userCommandPort = userCommandPort;
        this.credentialSetupPort = credentialSetupPort;
        this.totpRecordInitPort = totpRecordInitPort;
        this.personalSpaceInitPort = personalSpaceInitPort;
    }

    @Override
    @Transactional
    public Optional<UUID> seedInitialSuperAdmin(String username, String email, String password, Language language) {
        if (!userAdminPort.isEmpty()) {
            log.info("Database already has users, skipping seed");
            return Optional.empty();
        }
        var user = userCommandPort.createProfile(new CreateUserProfileCommand(username, email, Role.SUPER_ADMIN));
        credentialSetupPort.setup(user.id(), password);
        totpRecordInitPort.initForUser(user.id());
        personalSpaceInitPort.initForUser(user.id());
        if (language != null) {
            userCommandPort.updateLanguage(user.id(), language);
        }
        log.info("Initial SUPER_ADMIN '{}' created", username);
        return Optional.of(user.id());
    }
}