package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindUserUseCase {
    Optional<User> findByUsername(String username);
    Optional<User> findById(UUID id);
    List<User> findByIds(Collection<UUID> ids);
    Optional<User> findByEmail(String email);

    /** Non-deleted accounts with this address, letter case ignored — usually one, possibly none or several. */
    List<User> findByEmailIgnoreCase(String email);
}
