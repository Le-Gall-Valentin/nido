package com.nido.api.space.infrastructure.web.dto;

import com.nido.api.space.domain.model.SpaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Invitation d'une adresse à rejoindre le contexte")
public record InviteMemberRequest(
    @Schema(description = "Adresse de la personne invitée, doit déjà avoir un compte", example = "carol@exemple.fr")
    @NotBlank @Email String email,

    @Schema(description = "ADMIN, MEMBER ou VIEWER. OWNER ne peut pas être proposé à l'invitation.")
    @NotNull SpaceRole role
) {}
