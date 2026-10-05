package com.nido.api.instance.domain.port.out;

import com.nido.api.instance.domain.model.SettingKey;

import java.util.Map;

/** The settings the environment sets: variables, -D properties, files of /run/secrets. Blank ones are left out. */
public interface EnvironmentSettingsPort {
    Map<SettingKey, String> values();
}
