package com.nido.api.instance.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Settings saved together from one block of the administration page. {@code lockedAsAWhole}: one
 * variable of the group in the environment puts the whole group in its hands — half of a mail
 * configuration from the environment and the other half from the page would fail in ways nobody
 * could read, and it is how every installation that predates the page configures its mail.
 */
public enum SettingGroup {
    MAIL("mail", true),
    PUBLIC_URL("public-url", false),
    SESSIONS("sessions", false),
    API("api", false);

    private final String code;
    private final boolean lockedAsAWhole;

    SettingGroup(String code, boolean lockedAsAWhole) {
        this.code = code;
        this.lockedAsAWhole = lockedAsAWhole;
    }

    public String code() {
        return code;
    }

    public boolean lockedAsAWhole() {
        return lockedAsAWhole;
    }

    public List<SettingKey> keys() {
        return Arrays.stream(SettingKey.values()).filter(key -> key.group() == this).toList();
    }

    public static Optional<SettingGroup> fromCode(String code) {
        return Arrays.stream(values()).filter(group -> group.code.equals(code)).findFirst();
    }
}
