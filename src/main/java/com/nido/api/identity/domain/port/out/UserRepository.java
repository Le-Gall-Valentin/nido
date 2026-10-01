package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.EmailAddress;
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
    /** The account that is not deleted with this address. */
    Optional<User> findByEmail(EmailAddress email);
    Optional<User> findById(UUID id);
    List<User> findByIds(Collection<UUID> ids);
}
