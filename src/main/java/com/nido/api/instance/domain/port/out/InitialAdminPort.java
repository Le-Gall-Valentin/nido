package com.nido.api.instance.domain.port.out;

import com.nido.api.instance.domain.model.InitialAdmin;

import java.util.Optional;
import java.util.UUID;

public interface InitialAdminPort {
    /** Creates the first SUPER_ADMIN in the caller's transaction; empty when an account already exists. */
    Optional<UUID> create(InitialAdmin admin);
}
