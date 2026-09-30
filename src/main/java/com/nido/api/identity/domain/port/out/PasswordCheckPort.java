package com.nido.api.identity.domain.port.out;

import java.util.UUID;

public interface PasswordCheckPort {
    boolean matches(UUID userId, String rawPassword);
}
