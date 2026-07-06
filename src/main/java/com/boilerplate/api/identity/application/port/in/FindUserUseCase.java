package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindUserUseCase {
    Optional<User> findByUsername(String username);
    Optional<User> findById(UUID id);
    List<User> findByIds(Collection<UUID> ids);
}
