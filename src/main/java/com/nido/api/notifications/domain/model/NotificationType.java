package com.nido.api.notifications.domain.model;

import java.util.regex.Pattern;

/** A kind of notification, by its code: {@code space.invitation}. */
public record NotificationType(String code) {

    private static final Pattern FORMAT = Pattern.compile("[a-z][a-z0-9-]*\\.[a-z][a-z0-9-]*");
    /** The width of the column that stores it. */
    private static final int MAX_LENGTH = 64;

    public NotificationType {
        if (code == null || code.length() > MAX_LENGTH || !FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException(
                "A notification kind reads <context>.<name>, in lower case, in at most 64 characters: " + code);
        }
    }

    /** The context it belongs to — how the preferences card groups it. */
    public String group() {
        return code.substring(0, code.indexOf('.'));
    }
}
