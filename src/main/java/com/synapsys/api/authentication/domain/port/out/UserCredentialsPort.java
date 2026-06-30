package com.synapsys.api.authentication.domain.port.out;

import com.synapsys.api.authentication.domain.model.UserCredentials;
import java.util.Optional;
import java.util.UUID;

public interface UserCredentialsPort {
    Optional<UserCredentials> findByUsername(String username);
    Optional<UserCredentials> findById(UUID id);
}