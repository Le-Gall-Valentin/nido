package com.nido.api.space.infrastructure.web.dto;

import com.nido.api.space.domain.model.SpaceMemberView;
import com.nido.api.space.domain.model.SpaceRole;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code username} et {@code email} sont null quand le profil du compte n'est plus
 * résoluble, ce qui est le cas d'un compte anonymisé par une suppression RGPD. Un
 * client qui les suppose non nuls casse au premier compte anonymisé.
 */
public record SpaceMemberResponse(
    UUID userId,
    String username,
    String email,
    SpaceRole role,
    Instant joinedAt
) {
    public static SpaceMemberResponse from(SpaceMemberView view) {
        return new SpaceMemberResponse(view.userId(), view.username(), view.email(),
            view.role(), view.joinedAt());
    }
}
