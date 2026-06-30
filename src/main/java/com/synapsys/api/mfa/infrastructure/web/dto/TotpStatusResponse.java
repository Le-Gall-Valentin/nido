package com.synapsys.api.mfa.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statut actuel du TOTP pour l'utilisateur connecté")
public record TotpStatusResponse(
    @Schema(description = "true si le TOTP est activé et confirmé, false sinon", example = "false")
    boolean totpEnabled
) {}