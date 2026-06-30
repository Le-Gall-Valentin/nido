package com.synapsys.api.authentication.domain.port.out;

import com.synapsys.api.authentication.domain.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfilePort {
    Optional<UserProfile> findByUsername(String username);
    Optional<UserProfile> findById(UUID id);
}