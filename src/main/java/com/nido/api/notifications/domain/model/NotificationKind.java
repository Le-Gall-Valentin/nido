package com.nido.api.notifications.domain.model;

import com.nido.api.shared.model.Role;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The kind a {@link Notification} class declares — how it is listed on the preferences card and how an
 * account switches it off. The catalogue is built at startup from these declarations, so adding a kind
 * never touches the notifications context.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotificationKind {

    /** {@code <context>.<name>}, in lower case: {@code "space.invitation"}. Stored, and exchanged with the client. */
    String value();

    /**
     * The roles that may receive this kind; empty — the default — means every account. A reserved kind is
     * left off the preferences card of every other role, cannot be switched by them, and is never delivered
     * to an account that no longer has one of these roles.
     */
    Role[] roles() default {};
}
