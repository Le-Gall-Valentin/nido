package com.nido.api.space.infrastructure.web.dto;

import com.nido.api.space.domain.model.SpaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Invitation d'un compte à rejoindre le contexte")
public record InviteMemberRequest(
    @Schema(description = "Nom d'utilisateur ou adresse email de la personne invitée, qui doit déjà avoir un compte",
        example = "carol", maxLength = 254)
    @NotBlank @Size(max = 254) String identifier,

    @Schema(description = "ADMIN, MEMBER ou VIEWER. OWNER ne peut pas être proposé à l'invitation.")
    @NotNull SpaceRole role
) {
    @Override
    public String toString() {
        return "InviteMemberRequest[identifier=***, role=" + role + "]";
    }
}
