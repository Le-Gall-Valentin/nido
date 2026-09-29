package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
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
        return findUser.findByUsername(username).map(UserProfileAdapter::toProfile);
    }

    @Override
    public Optional<UserProfile> findById(UUID id) {
        return findUser.findById(id).map(UserProfileAdapter::toProfile);
    }

    @Override
    public List<UserProfile> findByEmailIgnoreCase(String email) {
        return findUser.findByEmailIgnoreCase(email).stream().map(UserProfileAdapter::toProfile).toList();
    }

    private static UserProfile toProfile(User u) {
        return new UserProfile(u.id(), u.username(), u.email(), u.isActive(), u.role(), u.createdAt(),
            u.language() == null ? null : u.language().code());
    }
}
