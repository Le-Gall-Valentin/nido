package com.nido.api.authentication.domain.port.out;

import com.nido.api.authentication.domain.model.UserCredentials;
import java.util.Optional;
import java.util.UUID;

public interface UserCredentialsPort {
    Optional<UserCredentials> findByIdentifier(String identifier);
    Optional<UserCredentials> findById(UUID id);
}