package com.nido.api.instance.infrastructure.web;

import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.SettingKey;

import java.util.EnumMap;
import java.util.Map;

/** The settings of a request, named by their codes ({@code mail.host}…), as the domain knows them. */
final class SettingCodes {

    private SettingCodes() {}

    /** An unknown code is answered as a setting that does not exist. */
    static Map<SettingKey, String> keys(Map<String, String> values) {
        Map<SettingKey, String> keys = new EnumMap<>(SettingKey.class);
        values.forEach((code, value) -> keys.put(
            SettingKey.fromCode(code).orElseThrow(() -> new InstanceException.UnknownSetting(code)), value));
        return keys;
    }
}
