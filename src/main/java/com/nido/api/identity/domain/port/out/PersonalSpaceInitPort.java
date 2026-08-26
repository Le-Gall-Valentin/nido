package com.nido.api.identity.domain.port.out;

import java.util.UUID;

public interface PersonalSpaceInitPort {
    void initForUser(UUID userId);
}
