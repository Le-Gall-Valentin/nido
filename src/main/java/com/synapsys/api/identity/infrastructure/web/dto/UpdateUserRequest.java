package com.synapsys.api.identity.infrastructure.web.dto;

import com.synapsys.api.shared.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Données de modification d'un utilisateur par un administrateur")
public record UpdateUserRequest(
    @Schema(description = "Nouveau rôle à attribuer. Un ADMIN ne peut pas assigner le rôle ADMIN ni SUPER_ADMIN.", example = "USER")
    @NotNull Role role
) {}