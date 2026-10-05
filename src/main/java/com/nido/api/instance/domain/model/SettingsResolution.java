package com.nido.api.instance.domain.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Environment over database over default. The environment is passed with its blank variables already
 * left out: a variable set to nothing is a variable not set — or whoever copied .env.example, with its
 * empty NIDO_SMTP_HOST=, would find mail locked by the environment and impossible to configure.
 *
 * <p>Environment values are not checked here: a wrong one stops the start (StartInstanceHandler). A
 * stored value that no longer holds — a rule tightened by a later version — is ignored: the default
 * applies and the caller reports it, but nothing fails over it.
 */
public final class SettingsResolution {

    public record Resolved(EffectiveSettings settings, List<SettingKey> ignored) {}

    private SettingsResolution() {}

    public static Resolved resolve(Map<SettingKey, String> environment, Map<SettingKey, String> stored) {
        EnumMap<SettingKey, EffectiveSetting> settings = new EnumMap<>(SettingKey.class);
        List<SettingKey> ignored = new ArrayList<>();
        for (SettingKey key : SettingKey.values()) {
            boolean groupFromEnvironment = key.group().lockedAsAWhole()
                && key.group().keys().stream().anyMatch(environment::containsKey);
            if (groupFromEnvironment || environment.containsKey(key)) {
                String value = environment.containsKey(key) ? SettingRules.normalize(key, environment.get(key)) : key.defaultValue();
                settings.put(key, new EffectiveSetting(key, value, SettingSource.ENVIRONMENT));
            } else if (stored.containsKey(key) && SettingRules.problem(key, stored.get(key)).isEmpty()) {
                settings.put(key, new EffectiveSetting(key, stored.get(key), SettingSource.DATABASE));
            } else {
                if (stored.containsKey(key)) {
                    ignored.add(key);
                }
                settings.put(key, new EffectiveSetting(key, key.defaultValue(), SettingSource.DEFAULT));
            }
        }
        return new Resolved(new EffectiveSettings(settings), List.copyOf(ignored));
    }
}
