package com.nido.api.authentication.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ce que les pages de connexion peuvent proposer sur cette installation")
public record CapabilitiesResponse(
    @Schema(description = "Le mot de passe oublié est disponible (l'envoi de mails est configuré)", example = "true")
    boolean passwordReset
) {}
