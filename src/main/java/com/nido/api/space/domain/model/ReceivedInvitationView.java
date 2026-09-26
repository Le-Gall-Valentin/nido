package com.nido.api.space.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Une invitation reçue, enrichie du nom, de l'accent et du glyphe de son contexte, et du nom
 * d'utilisateur de la personne qui l'a envoyée — null si son compte a été anonymisé.
 */
public record ReceivedInvitationView(
    UUID invitationId,
    UUID spaceId,
    String spaceName,
    String spaceAccent,
    String spaceGlyph,
    SpaceRole role,
    Instant expiresAt,
    String invitedByUsername
) {}
