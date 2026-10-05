package com.nido.api.instance.infrastructure.secrets;

import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.EnvironmentSettingsPort;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Read once: the environment of a running process does not change. Through Spring's Environment, so a
 * variable, a -D property and a file of /run/secrets named after the variable all count.
 */
@Component
public class SpringEnvironmentSettingsAdapter implements EnvironmentSettingsPort {

    private final Map<SettingKey, String> values;

    public SpringEnvironmentSettingsAdapter(Environment environment) {
        EnumMap<SettingKey, String> found = new EnumMap<>(SettingKey.class);
        for (SettingKey key : SettingKey.values()) {
            String value = environment.getProperty(key.variable());
            if (value != null && !value.isBlank()) {
                found.put(key, value);
            }
        }
        this.values = Collections.unmodifiableMap(found);
    }

    @Override
    public Map<SettingKey, String> values() {
        return values;
    }
}
