package com.boilerplate.api.shared.security;

import java.util.UUID;

public interface RateLimitPrincipal {
    UUID getUserId();
}