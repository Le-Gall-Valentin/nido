package com.nido.api.instance.application.port.in;

import com.nido.api.instance.domain.model.SettingKey;

import java.util.Locale;
import java.util.Map;

public interface SendSettingsTestMailUseCase {
    /** With the values of the form — nothing is saved; a blank password field means the saved one. */
    void sendTest(Map<SettingKey, String> mailValues, String recipient, Locale locale);
}
