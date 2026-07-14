package com.nido.api.authentication.domain.port.out;

import com.nido.api.authentication.domain.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfilePort {
    Optional<UserProfile> findByUsername(String username);
    Optional<UserProfile> findById(UUID id);
}