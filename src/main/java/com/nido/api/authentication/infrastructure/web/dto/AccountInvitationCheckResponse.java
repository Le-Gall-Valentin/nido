package com.nido.api.authentication.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Le compte qu'invite un lien")
public record AccountInvitationCheckResponse(
    @Schema(description = "Nom d'utilisateur du compte invité", example = "carol") String username
) {}
