package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.authentication.domain.model.UserProfile;
import com.synapsys.api.authentication.domain.port.out.UserProfilePort;
import com.synapsys.api.identity.application.port.in.FindUserUseCase;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserProfileAdapter implements UserProfilePort {

    private final FindUserUseCase findUser;

    public UserProfileAdapter(FindUserUseCase findUser) {
        this.findUser = findUser;
    }

    @Override
    public Optional<UserProfile> findByUsername(String username) {
        return findUser.findByUsername(username)
            .map(u -> new UserProfile(u.id(), u.username(), u.email(), u.isActive(), u.role(), u.createdAt()));
    }

    @Override
    public Optional<UserProfile> findById(UUID id) {
        return findUser.findById(id)
            .map(u -> new UserProfile(u.id(), u.username(), u.email(), u.isActive(), u.role(), u.createdAt()));
    }
}