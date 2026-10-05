package com.nido.api.instance.application.port.in;

import com.nido.api.instance.domain.model.EffectiveSettings;
import com.nido.api.instance.domain.model.SettingGroup;
import com.nido.api.instance.domain.model.SettingKey;

import java.util.Map;
import java.util.UUID;

public interface UpdateSettingsUseCase {

    /**
     * Saves what was typed in one block of the settings page, checked as a whole first. Absent keys
     * are left alone; a blank value goes back to the default — except a secret, which a blank field keeps.
     */
    EffectiveSettings update(SettingGroup group, Map<SettingKey, String> values, UUID by);

    /** Back to the default; a secret is cleared. */
    EffectiveSettings reset(SettingKey key, UUID by);
}
