package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindUserUseCase {
    Optional<User> findById(UUID id);
    List<User> findByIds(Collection<UUID> ids);

    /**
     * The account behind what a person typed to name one — at sign-in, in "forgot password", when
     * inviting. Never a technical id: surrounding spaces and letter case are ignored; with an '@' it is
     * an email address, without one a username. The two cannot overlap — a username holds no '@'. A
     * deleted account is never found; a deactivated one is.
     */
    Optional<User> findByIdentifier(String identifier);
}
