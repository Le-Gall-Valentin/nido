package com.nido.api.notifications.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "L'état voulu pour un interrupteur")
public record ChangeNotificationPreferenceRequest(
    @Schema(description = "Activé ou non", example = "false") @NotNull Boolean enabled
) {}
