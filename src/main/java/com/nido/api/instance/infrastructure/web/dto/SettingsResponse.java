package com.nido.api.instance.infrastructure.web.dto;

import com.nido.api.instance.domain.model.EffectiveSetting;
import com.nido.api.instance.domain.model.EffectiveSettings;
import com.nido.api.instance.domain.model.SettingGroup;

import java.util.Arrays;
import java.util.List;

/** Every setting with where its value comes from. A secret's value is never sent: only whether it is set. */
public record SettingsResponse(List<Group> groups) {

    public record Group(String group, List<Field> fields) {}

    public record Field(String key, String value, String source, String variable, boolean secret, boolean set) {}

    public static SettingsResponse of(EffectiveSettings settings) {
        return new SettingsResponse(Arrays.stream(SettingGroup.values())
            .map(group -> new Group(group.code(), group.keys().stream().map(key -> field(settings.get(key))).toList()))
            .toList());
    }

    private static Field field(EffectiveSetting setting) {
        boolean set = setting.value() != null && !setting.value().isBlank();
        return new Field(setting.key().code(), setting.key().secret() ? null : setting.value(),
            setting.source().name(), setting.key().variable(), setting.key().secret(), set);
    }
}
