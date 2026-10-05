package com.nido.api.instance;

import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.SettingsStorePort;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The settings rows live in the shared database for the whole run: a test that writes some removes
 * them afterwards, through the port, outside any transaction — which also empties the cache at once.
 */
public final class InstanceSettingsTestSupport {

    private InstanceSettingsTestSupport() {}

    public static void clear(SettingsStorePort store) {
        Map<SettingKey, Optional<String>> nothing = Arrays.stream(SettingKey.values())
            .collect(Collectors.toMap(Function.identity(), key -> Optional.<String>empty()));
        store.save(nothing, null, Instant.now());
    }
}
