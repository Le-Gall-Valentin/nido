package com.synapsys.api.authentication.infrastructure.persistence.adapter;

import com.synapsys.api.authentication.domain.model.UserCredentials;
import com.synapsys.api.authentication.domain.model.UserProfile;
import com.synapsys.api.authentication.domain.port.out.UserCredentialsPort;
import com.synapsys.api.authentication.domain.port.out.UserProfilePort;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserCredentialsRepositoryAdapter implements UserCredentialsPort {

    private static final Logger log = LoggerFactory.getLogger(UserCredentialsRepositoryAdapter.class);

    private final UserProfilePort userProfilePort;
    private final UserCredentialJpaRepository credentialRepo;

    public UserCredentialsRepositoryAdapter(UserProfilePort userProfilePort,
                                            UserCredentialJpaRepository credentialRepo) {
        this.userProfilePort = userProfilePort;
        this.credentialRepo = credentialRepo;
    }

    @Override
    public Optional<UserCredentials> findByUsername(String username) {
        return userProfilePort.findByUsername(username).flatMap(this::resolveCredentials);
    }

    @Override
    public Optional<UserCredentials> findById(UUID id) {
        return userProfilePort.findById(id).flatMap(this::resolveCredentials);
    }

    private Optional<UserCredentials> resolveCredentials(UserProfile profile) {
        var cred = credentialRepo.findById(profile.id());
        if (cred.isEmpty()) {
            log.error("Data integrity: profile {} ({}) has no credentials", profile.id(), profile.username());
        }
        return cred.map(c -> toUserCredentials(profile, c.getPasswordHash()));
    }

    private UserCredentials toUserCredentials(UserProfile profile, String passwordHash) {
        return new UserCredentials(
                profile.id(),
                profile.username(),
                profile.email(),
                passwordHash,
                profile.isActive(),
                profile.role(),
                profile.createdAt()
        );
    }
}