package com.nido.api.space.infrastructure.web.dto;

import com.nido.api.space.domain.model.SpaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nouveau rôle d'un membre du contexte")
public record ChangeMemberRoleRequest(
    @Schema(description = "ADMIN, MEMBER ou VIEWER. OWNER passe par le transfert de propriété.")
    @NotNull SpaceRole role
) {}
