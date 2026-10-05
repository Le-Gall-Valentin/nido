package com.nido.api.instance.application.port.in;

import com.nido.api.instance.domain.model.EffectiveSettings;

/** Every setting as it applies now — asked at the moment of use, so a change applies without a restart. */
public interface GetEffectiveSettingsQuery {
    EffectiveSettings current();
}
