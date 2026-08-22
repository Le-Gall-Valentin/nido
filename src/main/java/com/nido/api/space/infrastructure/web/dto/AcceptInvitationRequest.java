package com.nido.api.space.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Le code d'invitation reçu par l'invité")
public record AcceptInvitationRequest(
    @Schema(description = "Code d'invitation", example = "NIDO-ABC123")
    @NotBlank @Size(max = 16) String code
) {}
