package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    List<User> findByIds(Collection<UUID> ids);
}
