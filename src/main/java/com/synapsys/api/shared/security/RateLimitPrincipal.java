package com.synapsys.api.shared.security;

import java.util.UUID;

public interface RateLimitPrincipal {
    UUID getUserId();
}