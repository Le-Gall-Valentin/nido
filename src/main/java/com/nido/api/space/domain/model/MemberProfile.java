package com.nido.api.space.domain.model;

import java.util.UUID;

/** Vue minimale d'un compte, telle que le contexte en a besoin pour afficher ses membres. */
public record MemberProfile(UUID userId, String username, String email) {}
