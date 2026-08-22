package com.nido.api.space.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SpaceMemberView(
    UUID userId,
    String username,
    String email,
    SpaceRole role,
    Instant joinedAt
) {}
