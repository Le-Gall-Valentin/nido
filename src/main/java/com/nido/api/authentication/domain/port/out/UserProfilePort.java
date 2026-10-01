package com.nido.api.authentication.domain.port.out;

import com.nido.api.authentication.domain.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfilePort {
    /** The account behind a typed username or email address — identity's rule, FindUserUseCase#findByIdentifier. */
    Optional<UserProfile> findByIdentifier(String identifier);
    Optional<UserProfile> findById(UUID id);
}
