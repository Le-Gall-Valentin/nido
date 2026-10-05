package com.nido.api.instance.application.handler;

import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.EffectiveSettings;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingsResolution;
import com.nido.api.instance.domain.port.out.EnvironmentSettingsPort;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationService
public class GetEffectiveSettingsHandler implements GetEffectiveSettingsQuery {

    private static final Logger log = LoggerFactory.getLogger(GetEffectiveSettingsHandler.class);

    private final EnvironmentSettingsPort environment;
    private final SettingsStorePort store;
    private final Set<SettingKey> reported = ConcurrentHashMap.newKeySet();

    public GetEffectiveSettingsHandler(EnvironmentSettingsPort environment, SettingsStorePort store) {
        this.environment = environment;
        this.store = store;
    }

    @Override
    public EffectiveSettings current() {
        SettingsResolution.Resolved resolved = SettingsResolution.resolve(environment.values(), store.load());
        resolved.ignored().stream().filter(reported::add).forEach(key -> log.error(
            "The saved value of {} is not valid any more and is ignored: its default applies. Set it again "
                + "from the administration page.", key.code()));
        return resolved.settings();
    }
}
