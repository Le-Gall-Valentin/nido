package com.nido.api.instance.domain.model;

/** A setting as it applies now. {@code value} may be null: no default and nothing set. */
public record EffectiveSetting(SettingKey key, String value, SettingSource source) {
    @Override
    public String toString() {
        return "EffectiveSetting[" + key.code() + "=" + (key.secret() && value != null ? "***" : value) + " from " + source + "]";
    }
}
