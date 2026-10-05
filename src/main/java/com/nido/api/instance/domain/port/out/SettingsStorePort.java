package com.nido.api.instance.domain.port.out;

import com.nido.api.instance.domain.model.SettingKey;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SettingsStorePort {

    /** What was saved from the page, secrets decrypted. */
    Map<SettingKey, String> load();

    /**
     * In the caller's transaction: a present value is stored, an empty one deletes its row. Readers see
     * the change once it commits. {@code by}: the account that made the change — for the setup screen, the
     * administrator it has just created; null only where no account made it.
     */
    void save(Map<SettingKey, Optional<String>> changes, UUID by, Instant at);
}
