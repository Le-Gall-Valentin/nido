package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    /**
     * The account that is not deleted with this username, letter case ignored — a deleted one may have
     * kept it, and its name taken again.
     */
    Optional<User> findByUsername(String username);
    /** The account that is not deleted with exactly this address — addresses are stored lower-case. */
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    List<User> findByIds(Collection<UUID> ids);
}
