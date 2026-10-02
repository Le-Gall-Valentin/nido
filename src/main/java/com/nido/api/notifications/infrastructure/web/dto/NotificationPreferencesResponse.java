package com.nido.api.notifications.infrastructure.web.dto;

import com.nido.api.notifications.domain.model.NotificationPreferencesView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Les canaux disponibles sur cette installation et tous les types de notification, chacun avec son état")
public record NotificationPreferencesResponse(List<ChannelPreference> channels, List<TypePreference> types) {

    public record ChannelPreference(
        @Schema(description = "Code du canal", example = "email") String channel,
        boolean enabled) {}

    public record TypePreference(
        @Schema(description = "Code du type, <contexte>.<nom>", example = "space.invitation") String type,
        boolean enabled) {}

    public static NotificationPreferencesResponse of(NotificationPreferencesView view) {
        return new NotificationPreferencesResponse(
            view.channels().stream().map(setting -> new ChannelPreference(setting.channel().code(), setting.enabled())).toList(),
            view.types().stream().map(setting -> new TypePreference(setting.type().code(), setting.enabled())).toList());
    }
}
