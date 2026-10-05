package com.nido.api.instance.domain.model;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Every setting as it applies now, each with its source. Values were checked on the way in. */
public final class EffectiveSettings {

    private final Map<SettingKey, EffectiveSetting> settings;

    public EffectiveSettings(Map<SettingKey, EffectiveSetting> settings) {
        EnumMap<SettingKey, EffectiveSetting> copy = new EnumMap<>(SettingKey.class);
        copy.putAll(settings);
        if (copy.size() != SettingKey.values().length) {
            throw new IllegalArgumentException("Every setting has a value, even if only its default");
        }
        this.settings = copy;
    }

    public EffectiveSetting get(SettingKey key) {
        return settings.get(key);
    }

    public SettingSource source(SettingKey key) {
        return settings.get(key).source();
    }

    /** Set in the environment: the page shows it and cannot change it. */
    public boolean locked(SettingKey key) {
        return source(key) == SettingSource.ENVIRONMENT;
    }

    /** The value, when there is one that is not blank. */
    public Optional<String> text(SettingKey key) {
        return Optional.ofNullable(settings.get(key).value()).filter(value -> !value.isBlank());
    }

    public int integer(SettingKey key) {
        return Integer.parseInt(text(key).orElseThrow(() -> new IllegalStateException(key.code() + " has no value")));
    }

    public boolean flag(SettingKey key) {
        return text(key).map(Boolean::parseBoolean).orElse(false);
    }

    public Collection<EffectiveSetting> all() {
        return settings.values();
    }

    @Override
    public String toString() {
        return "EffectiveSettings" + settings.values();
    }
}
