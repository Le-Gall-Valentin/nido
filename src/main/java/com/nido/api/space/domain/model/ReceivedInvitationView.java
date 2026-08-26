package com.nido.api.space.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Une invitation reçue, enrichie du nom, de l'accent et du glyphe de son contexte. */
public record ReceivedInvitationView(
    UUID invitationId,
    UUID spaceId,
    String spaceName,
    String spaceAccent,
    String spaceGlyph,
    SpaceRole role,
    Instant expiresAt
) {}
