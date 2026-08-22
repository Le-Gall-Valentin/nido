package com.nido.api.space.infrastructure.persistence.repository;

import java.util.UUID;

public record MemberCount(UUID spaceId, long total) {}
